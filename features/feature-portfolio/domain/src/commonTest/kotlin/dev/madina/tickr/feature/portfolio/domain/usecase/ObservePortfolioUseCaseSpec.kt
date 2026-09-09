package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.model.Portfolio
import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * State is created inside each `When` rather than at `Given` level. Kotest's isolation modes are
 * silently ignored on Kotlin/Native, so shared state would leak between blocks and fail on iOS
 * only.
 */
internal class ObservePortfolioUseCaseSpec :
    BehaviorSpec({

        Given("holdings and a price feed") {

            When("both have emitted") {
                Then("each holding is valued at its latest price") {
                    runTest {
                        val holdings = FakeHoldingsRepository(listOf(Btc, Eth))
                        val prices =
                            FakePriceRepository(
                                mapOf(
                                    "BTC" to PriceTick("BTC", price = 200.0, changePercent24h = 1.0),
                                    "ETH" to PriceTick("ETH", price = 50.0, changePercent24h = -1.0),
                                ),
                            )
                        val useCase = ObservePortfolioUseCase(holdings, prices, UnconfinedTestDispatcher(testScheduler))

                        val portfolio = useCase(Unit).first { it.holdings.all { holding -> holding.price != null } }

                        portfolio.totalValue shouldBe (2 * 200.0) + (3 * 50.0)
                        portfolio.holdings.map { it.holding.symbol } shouldContainExactly listOf("BTC", "ETH")
                    }
                }
            }

            When("the feed has only quoted one of them") {
                Then("the other is present but unpriced, rather than missing or zero") {
                    runTest {
                        val holdings = FakeHoldingsRepository(listOf(Btc, Eth))
                        val prices =
                            FakePriceRepository(
                                mapOf("BTC" to PriceTick("BTC", price = 200.0, changePercent24h = 0.0)),
                            )
                        val useCase = ObservePortfolioUseCase(holdings, prices, UnconfinedTestDispatcher(testScheduler))

                        val portfolio = useCase(Unit).first()

                        portfolio.holdings.size shouldBe 2
                        portfolio.holdings
                            .first { it.holding.symbol == "ETH" }
                            .value
                            .shouldBeNull()
                        portfolio.isPartiallyPriced shouldBe true
                        portfolio.totalValue shouldBe 400.0
                    }
                }
            }

            When("a holding is added") {
                Then("the price feed is resubscribed with the new symbol set") {
                    runTest {
                        val holdings = FakeHoldingsRepository(listOf(Btc))
                        val prices =
                            FakePriceRepository(
                                mapOf(
                                    "BTC" to PriceTick("BTC", price = 200.0, changePercent24h = 0.0),
                                    "ETH" to PriceTick("ETH", price = 50.0, changePercent24h = 0.0),
                                ),
                            )
                        val useCase = ObservePortfolioUseCase(holdings, prices, UnconfinedTestDispatcher(testScheduler))

                        // Subscribed first, mutated second. Adding the holding inside the chain
                        // being built ran before anything collected, so the feed was asked once
                        // for the final set and this passed with flatMapLatest deleted from the
                        // use case, which is the opposite of what it claims to check.
                        val seen = mutableListOf<Portfolio>()
                        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                            useCase(Unit).toList(seen)
                        }

                        prices.requestedSymbols shouldContainExactly listOf(setOf("BTC"))

                        holdings.add(Eth)

                        prices.requestedSymbols shouldContainExactly
                            listOf(setOf("BTC"), setOf("BTC", "ETH"))
                        seen.last().totalValue shouldBe 400.0 + 150.0
                    }
                }
            }
        }
    })

private val Btc = Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 100.0)
private val Eth = Holding(symbol = "ETH", name = "Ethereum", quantity = 3.0, averageCost = 20.0)

private class FakeHoldingsRepository(
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

    fun add(holding: Holding) {
        state.value = state.value + holding
    }
}

private class FakePriceRepository(
    private val quotes: Map<String, PriceTick>,
) : PriceRepository {
    /** Records each subscription so a test can assert the feed was resubscribed, not just re-read. */
    val requestedSymbols = mutableListOf<Set<String>>()

    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> {
        requestedSymbols += symbols
        return MutableStateFlow(quotes.filterKeys { it in symbols })
    }
}
