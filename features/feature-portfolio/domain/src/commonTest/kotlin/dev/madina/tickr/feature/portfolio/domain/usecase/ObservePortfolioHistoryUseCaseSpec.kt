package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.feature.portfolio.domain.model.HistoryRange
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.model.PricePoint
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceHistoryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * State is created inside each `Then`: Kotest's isolation modes are silently ignored on
 * Kotlin/Native, so shared state would leak between blocks and fail on iOS only.
 */
internal class ObservePortfolioHistoryUseCaseSpec :
    BehaviorSpec({

        Given("two holdings whose candles line up") {

            When("the day is read") {
                Then("the total is the portfolio's value at each shared instant") {
                    runTest {
                        val history =
                            FakeHistory(
                                "BTC" to listOf(PricePoint(1, 100.0), PricePoint(2, 110.0)),
                                "ETH" to listOf(PricePoint(1, 10.0), PricePoint(2, 20.0)),
                            )
                        val useCase = buildUseCase(listOf(twoBtc, threeEth), history, testScheduler)

                        val day = useCase(HistoryRange.Day).first()

                        // 2 BTC + 3 ETH at each hour.
                        day.total shouldContainExactly listOf(230.0, 280.0)
                        day.perSymbol.getValue("BTC") shouldContainExactly listOf(100.0, 110.0)
                    }
                }
            }
        }

        Given("candles that do not cover the same instants") {

            When("the day is read") {
                Then("only the instants every holding has are totalled") {
                    runTest {
                        // Zipping by position would have added BTC at hour 2 to ETH at hour 3 and
                        // called the result a portfolio.
                        val history =
                            FakeHistory(
                                "BTC" to listOf(PricePoint(1, 100.0), PricePoint(2, 100.0)),
                                "ETH" to listOf(PricePoint(2, 10.0), PricePoint(3, 10.0)),
                            )
                        val useCase = buildUseCase(listOf(twoBtc, threeEth), history, testScheduler)

                        val day = useCase(HistoryRange.Day).first()

                        day.total shouldContainExactly listOf(230.0)
                    }
                }
            }
        }

        Given("a holding the exchange has no history for") {

            When("the day is read") {
                Then("no total is produced, rather than one missing that position") {
                    runTest {
                        // A total that silently drops a holding is not a smaller total, it is a
                        // different portfolio, and nothing on screen would say which.
                        val history = FakeHistory("BTC" to listOf(PricePoint(1, 100.0)))
                        val useCase = buildUseCase(listOf(twoBtc, threeEth), history, testScheduler)

                        val day = useCase(HistoryRange.Day).first()

                        day.total.shouldBeEmpty()
                        day.perSymbol.getValue("ETH").shouldBeEmpty()
                    }
                }
            }
        }

        Given("an empty portfolio") {

            When("the day is read") {
                Then("nothing is fetched") {
                    runTest {
                        val history = FakeHistory()
                        val useCase = buildUseCase(emptyList(), history, testScheduler)

                        val day = useCase(HistoryRange.Day).first()

                        day.total.shouldBeEmpty()
                        history.requested.shouldBeEmpty()
                    }
                }
            }
        }

        Given("a quantity that changes without the symbol set changing") {

            When("the day is read again") {
                Then("the total is refetched, because the position is worth something else") {
                    runTest {
                        val history = FakeHistory("BTC" to listOf(PricePoint(1, 100.0)))
                        val holdings = FakeHoldings(listOf(twoBtc))
                        val useCase =
                            ObservePortfolioHistoryUseCase(
                                holdings,
                                history,
                                UnconfinedTestDispatcher(testScheduler),
                            )

                        useCase(HistoryRange.Day).first().total shouldContainExactly listOf(200.0)

                        holdings.replace(listOf(twoBtc.copy(quantity = 5.0)))

                        useCase(HistoryRange.Day).first().total shouldContainExactly listOf(500.0)
                    }
                }
            }
        }
    })

private val twoBtc = Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 50.0)
private val threeEth = Holding(symbol = "ETH", name = "Ethereum", quantity = 3.0, averageCost = 5.0)

private fun buildUseCase(
    holdings: List<Holding>,
    history: FakeHistory,
    scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
) = ObservePortfolioHistoryUseCase(
    FakeHoldings(holdings),
    history,
    UnconfinedTestDispatcher(scheduler),
)

private class FakeHoldings(
    initial: List<Holding>,
) : HoldingsRepository {
    private val state = MutableStateFlow(initial)

    override fun observeHoldings(): Flow<List<Holding>> = state

    override suspend fun addHolding(holding: Holding) {
        state.value = state.value + holding
    }

    override suspend fun removeHolding(symbol: String) {
        state.value = state.value.filterNot { it.symbol == symbol }
    }

    fun replace(holdings: List<Holding>) {
        state.value = holdings
    }
}

private class FakeHistory(
    vararg days: Pair<String, List<PricePoint>>,
) : PriceHistoryRepository {
    private val bySymbol = days.toMap()

    /** Recorded so a spec can assert an empty portfolio asks the exchange for nothing. */
    val requested = mutableListOf<String>()

    override suspend fun history(symbol: String, range: HistoryRange): List<PricePoint> {
        requested += symbol
        return bySymbol[symbol].orEmpty()
    }
}
