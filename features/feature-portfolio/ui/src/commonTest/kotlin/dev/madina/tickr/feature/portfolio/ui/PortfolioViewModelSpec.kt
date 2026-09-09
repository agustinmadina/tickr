package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.domain.model.Asset
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.RemoveHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.SearchAssetsUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * `viewModelScope` dispatches on `Dispatchers.Main`, which does not exist on a test JVM, so it is
 * replaced for the duration of each scenario. State is built inside each `When` block: Kotest's
 * isolation modes are silently ignored on Kotlin/Native, so anything shared would leak.
 */
internal class PortfolioViewModelSpec :
    BehaviorSpec({

        beforeSpec { Dispatchers.setMain(UnconfinedTestDispatcher()) }
        afterSpec { Dispatchers.resetMain() }

        Given("a portfolio that has been priced") {

            When("the feed emits") {
                Then("loading ends and the totals reach the state") {
                    runTest {
                        val viewModel = viewModel()

                        val state = viewModel.state.first { !it.isLoading }

                        state.totalValue shouldBe 300.0
                        state.holdings.size shouldBe 1
                    }
                }

                Then("the total's history collects a sample once everything is priced") {
                    runTest {
                        val viewModel = viewModel()

                        val state = viewModel.state.first { it.totalHistory.isNotEmpty() }

                        state.totalHistory.first() shouldBe 300f
                    }
                }
            }

            When("the user points at a sample on the chart") {
                Then("the headline reads that point instead of the live total") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }

                        viewModel.onAction(PortfolioAction.Scrubbed(0))

                        val state = viewModel.state.value
                        state.isScrubbing shouldBe true
                        state.displayedValue shouldBe 300.0
                    }
                }

                Then("releasing returns the headline to the live total") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.Scrubbed(0))

                        viewModel.onAction(PortfolioAction.Scrubbed(null))

                        viewModel.state.value.isScrubbing shouldBe false
                    }
                }

                Then("an index past the end is clamped rather than crashing") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }

                        viewModel.onAction(PortfolioAction.Scrubbed(9_999))

                        viewModel.state.value.scrubIndex shouldBe viewModel.state.value.totalHistory.lastIndex
                    }
                }
            }

            When("the user opens an asset while pointing at the chart") {
                Then("the scrub is cleared, since the next chart holds a different series") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.Scrubbed(0))

                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.state.value.scrubIndex
                            .shouldBeNull()
                    }
                }
            }
        }

        Given("the detail screen open on an asset") {
            When("the user points at its chart") {
                Then("the selected holding has a series to read a price from") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.holdings.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.onAction(PortfolioAction.Scrubbed(0))

                        val state = viewModel.state.value
                        state.selectedHolding.shouldNotBeNull()
                        state.selectedHolding!!.history.shouldNotBeEmpty()
                    }
                }

                Then("the scrub index survives, so the detail can render the point") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.holdings.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.onAction(PortfolioAction.Scrubbed(0))

                        viewModel.state.value.scrubIndex shouldBe 0
                    }
                }
            }
        }

        Given("an asset already held") {
            When("the add sheet is opened") {
                Then("that asset is not offered, so it cannot be added twice") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.state.first { !it.isLoading }

                        viewModel.onAction(PortfolioAction.AddClicked)

                        val results = viewModel.state.first { it.assetResults.isNotEmpty() }.assetResults
                        results.map { it.symbol } shouldNotContain "BTC"
                    }
                }
            }
        }

        Given("the add sheet with nothing selected") {
            When("the user confirms") {
                Then("nothing is added and the sheet stays open") {
                    runTest {
                        val viewModel = viewModel()
                        viewModel.onAction(PortfolioAction.AddClicked)

                        viewModel.onAction(PortfolioAction.AddConfirmed(quantity = "1", averageCost = "1"))

                        viewModel.state.value.isAddSheetVisible shouldBe true
                    }
                }
            }
        }
    })

private fun viewModel(): PortfolioViewModel {
    val dispatcher = UnconfinedTestDispatcher()
    val holdings = FakeHoldingsRepository()
    val prices = FakePriceRepository()
    val catalog = FakeAssetCatalogRepository()

    return PortfolioViewModel(
        observePortfolio = ObservePortfolioUseCase(holdings, prices, dispatcher),
        addHolding = AddHoldingUseCase(holdings, dispatcher),
        removeHolding = RemoveHoldingUseCase(holdings, dispatcher),
        searchAssets = SearchAssetsUseCase(catalog, dispatcher),
    )
}

private class FakeHoldingsRepository : HoldingsRepository {
    private val state =
        MutableStateFlow(
            listOf(Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 100.0)),
        )

    override fun observeHoldings(): Flow<List<Holding>> = state

    override suspend fun addHolding(holding: Holding) {
        state.value = state.value.filterNot { it.symbol == holding.symbol } + holding
    }

    override suspend fun removeHolding(symbol: String) {
        state.value = state.value.filterNot { it.symbol == symbol }
    }
}

private class FakePriceRepository : PriceRepository {
    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> =
        MutableStateFlow(
            symbols.associateWith { PriceTick(symbol = it, price = 150.0, changePercent24h = 0.0) },
        )
}

private class FakeAssetCatalogRepository : AssetCatalogRepository {
    override suspend fun tradableAssets(): List<Asset> =
        listOf(
            Asset(symbol = "BTC", name = "Bitcoin"),
            Asset(symbol = "ETH", name = "Ethereum"),
        )
}
