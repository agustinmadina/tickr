package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.feature.portfolio.domain.model.Holding
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * State is created inside each `When` rather than at `Given` level. Kotest's isolation modes are
 * silently ignored on Kotlin/Native, so shared state would leak between blocks and fail on iOS
 * only.
 */
internal class ObservePortfolioUseCaseSpec : BehaviorSpec({

    Given("holdings and a price feed") {

        When("both have emitted") {
            Then("each holding is valued at its latest price") {
                runTest {
                    val holdings = FakeHoldingsRepository(listOf(Btc, Eth))
                    val prices = FakePriceRepository(
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
                    val prices = FakePriceRepository(
                        mapOf("BTC" to PriceTick("BTC", price = 200.0, changePercent24h = 0.0)),
                    )
                    val useCase = ObservePortfolioUseCase(holdings, prices, UnconfinedTestDispatcher(testScheduler))

                    val portfolio = useCase(Unit).first()

                    portfolio.holdings.size shouldBe 2
                    portfolio.holdings.first { it.holding.symbol == "ETH" }.value.shouldBeNull()
                    portfolio.isPartiallyPriced shouldBe true
                    portfolio.totalValue shouldBe 400.0
                }
            }
        }

        When("a holding is added") {
            Then("the price feed is resubscribed with the new symbol set") {
                runTest {
                    val holdings = FakeHoldingsRepository(listOf(Btc))
                    val prices = FakePriceRepository(
                        mapOf(
                            "BTC" to PriceTick("BTC", price = 200.0, changePercent24h = 0.0),
                            "ETH" to PriceTick("ETH", price = 50.0, changePercent24h = 0.0),
                        ),
                    )
                    val useCase = ObservePortfolioUseCase(holdings, prices, UnconfinedTestDispatcher(testScheduler))

                    // Collect until the portfolio reflects both holdings, which can only happen if
                    // the symbol set was recomputed and the feed resubscribed.
                    val sizes = mutableListOf<Int>()
                    val portfolio = useCase(Unit)
                        .map { it.also { current -> sizes += current.holdings.size } }
                        .also { holdings.add(Eth) }
                        .first { it.holdings.size == 2 }

                    portfolio.totalValue shouldBe 400.0 + 150.0
                    prices.requestedSymbols.last() shouldBe setOf("BTC", "ETH")
                }
            }
        }
    }
})

private val Btc = Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 100.0)
private val Eth = Holding(symbol = "ETH", name = "Ethereum", quantity = 3.0, averageCost = 20.0)

private class FakeHoldingsRepository(initial: List<Holding>) : HoldingsRepository {
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

private class FakePriceRepository(private val quotes: Map<String, PriceTick>) : PriceRepository {
    /** Records each subscription so a test can assert the feed was resubscribed, not just re-read. */
    val requestedSymbols = mutableListOf<Set<String>>()

    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> {
        requestedSymbols += symbols
        return MutableStateFlow(quotes.filterKeys { it in symbols })
    }
}
