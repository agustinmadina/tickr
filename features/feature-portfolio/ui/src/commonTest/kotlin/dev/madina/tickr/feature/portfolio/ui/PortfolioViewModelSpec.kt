package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.domain.model.Asset
import dev.madina.tickr.feature.portfolio.domain.model.FeedStatus
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObserveFeedStatusUseCase
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
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
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

        Given("a portfolio that has been priced") {

            When("the feed emits") {
                Then("loading ends and the totals reach the state") {
                    mviTest {
                        val viewModel = viewModel()

                        val state = viewModel.state.first { !it.isLoading }

                        state.totalValue shouldBe 300.0
                        state.holdings.size shouldBe 1
                    }
                }

                Then("the total's history collects a sample once everything is priced") {
                    mviTest {
                        val viewModel = viewModel()

                        val state = viewModel.state.first { it.totalHistory.isNotEmpty() }

                        state.totalHistory.first() shouldBe 300f
                    }
                }
            }

            When("the user points at a sample on the chart") {
                Then("the headline reads that point instead of the live total") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, 0))

                        val state = viewModel.state.value
                        state.isScrubbing shouldBe true
                        state.displayedValue shouldBe 300.0
                    }
                }

                Then("releasing returns the headline to the live total") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, 0))

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, null))

                        viewModel.state.value.isScrubbing shouldBe false
                    }
                }

                Then("an index past the end is clamped rather than crashing") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, 9_999))

                        viewModel.state.value.overviewScrubIndex shouldBe viewModel.state.value.totalHistory.lastIndex
                    }
                }
            }

            When("the user opens an asset while pointing at the overview chart") {
                Then("the overview marker stays, since that chart never left the screen") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, 0))

                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        // It used to be cleared, back when a single index was shared by both
                        // charts. In the two pane layout the overview is still visible after
                        // opening an asset, so clearing it would wipe a marker the user can see.
                        viewModel.state.value.overviewScrubIndex shouldBe 0
                    }
                }

                Then("the detail's own marker is cleared, since it pointed into another series") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.holdings.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))
                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Detail, 0))

                        viewModel.onAction(PortfolioAction.BackClicked)

                        viewModel.state.value.detailScrubIndex
                            .shouldBeNull()
                    }
                }
            }
        }

        Given("the detail screen open on an asset") {
            When("the user points at its chart") {
                Then("the selected holding has a series to read a price from") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.holdings.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Detail, 0))

                        val state = viewModel.state.value
                        state.selectedHolding.shouldNotBeNull()
                        state.selectedHolding!!.history.shouldNotBeEmpty()
                    }
                }

                Then("only the detail's marker moves, not the overview's") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Detail, 0))

                        val state = viewModel.state.value
                        state.detailScrubIndex shouldBe 0
                        // Both charts are on screen together in the two pane layout, so a shared
                        // index marked whichever one the pointer was not on.
                        state.overviewScrubIndex.shouldBeNull()
                    }
                }

                Then("pointing at the overview does not mark the detail either") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.HoldingClicked("BTC"))

                        viewModel.onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, 0))

                        viewModel.state.value.detailScrubIndex
                            .shouldBeNull()
                    }
                }
            }
        }

        Given("a chart built up over a session") {
            When("an asset is added, changing what the total covers") {
                Then("the series restarts, since a bigger portfolio is not a market move") {
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.state.first { it.totalHistory.isNotEmpty() }

                        viewModel.onAction(PortfolioAction.AddClicked)
                        viewModel.state.first { it.assetResults.isNotEmpty() }
                        viewModel.onAction(PortfolioAction.AssetSelected("ETH"))
                        viewModel.onAction(
                            PortfolioAction.AddConfirmed(quantity = "1", averageCost = "10"),
                        )

                        // Not "the series is empty": it restarts and immediately begins filling
                        // again. What matters is that it no longer carries the previous
                        // portfolio's total, which is the value the cliff was drawn from.
                        val history = viewModel.state.first { it.holdings.size == 2 }.totalHistory
                        history shouldNotContain SingleHoldingTotal
                    }
                }
            }
        }

        Given("an asset already held") {
            When("the add sheet is opened") {
                Then("that asset is not offered, so it cannot be added twice") {
                    mviTest {
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
                    mviTest {
                        val viewModel = viewModel()
                        viewModel.onAction(PortfolioAction.AddClicked)

                        viewModel.onAction(PortfolioAction.AddConfirmed(quantity = "1", averageCost = "1"))

                        viewModel.state.value.isAddSheetVisible shouldBe true
                    }
                }
            }

            When("the price feed drops") {
                Then("the screen is told, so blank values read as offline rather than broken") {
                    mviTest {
                        // Prices are not persisted, so offline every value is blank. Without this
                        // the screen looks broken instead of disconnected.
                        val prices = FakePriceRepository()
                        val viewModel = viewModel(prices)
                        viewModel.state.first { !it.isLoading }

                        viewModel.state.value.isFeedDown shouldBe false

                        prices.status.value = FeedStatus.Disconnected

                        viewModel.state.first { it.isFeedDown }.isFeedDown shouldBe true
                    }
                }

                Then("it is told again when the feed comes back") {
                    mviTest {
                        val prices = FakePriceRepository()
                        val viewModel = viewModel(prices)
                        viewModel.state.first { !it.isLoading }

                        prices.status.value = FeedStatus.Disconnected
                        viewModel.state.first { it.isFeedDown }

                        prices.status.value = FeedStatus.Live

                        viewModel.state.first { !it.isFeedDown }.isFeedDown shouldBe false
                    }
                }
            }
        }
    })

/** BTC alone, 2 units at 150. Adding ETH takes the total past this, so the old value must go. */
private const val SingleHoldingTotal = 300f

private fun viewModel(
    prices: FakePriceRepository = FakePriceRepository(),
): PortfolioViewModel {
    val dispatcher = UnconfinedTestDispatcher()
    val holdings = FakeHoldingsRepository()
    val catalog = FakeAssetCatalogRepository()

    return PortfolioViewModel(
        observePortfolio = ObservePortfolioUseCase(holdings, prices, dispatcher),
        observeFeedStatus = ObserveFeedStatusUseCase(prices, dispatcher),
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

/**
 * `runTest` with the main dispatcher pointed at that test's own scheduler.
 *
 * Calling `setMain` once in `beforeSpec` built a dispatcher carrying a scheduler of its own, so
 * `viewModelScope` ran on a clock no test ever advanced and `runTest` never awaited it. Every path
 * exercised here happens to be delay-free, which is the only reason it did not show: a spec for the
 * debounced search would have read the state from before the delay and passed.
 */
private fun mviTest(body: suspend TestScope.() -> Unit): TestResult =
    runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            body()
        } finally {
            Dispatchers.resetMain()
        }
    }

private class FakePriceRepository : PriceRepository {
    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> =
        MutableStateFlow(
            symbols.associateWith { PriceTick(symbol = it, price = 150.0, changePercent24h = 0.0) },
        )

    val status = MutableStateFlow(FeedStatus.Live)

    override fun observeStatus(): Flow<FeedStatus> = status
}

private class FakeAssetCatalogRepository : AssetCatalogRepository {
    override suspend fun tradableAssets(): List<Asset> =
        listOf(
            Asset(symbol = "BTC", name = "Bitcoin"),
            Asset(symbol = "ETH", name = "Ethereum"),
        )
}
