package dev.madina.tickr.feature.portfolio.ui

import androidx.lifecycle.viewModelScope
import dev.madina.tickr.core.domain.usecase.invoke
import dev.madina.tickr.core.ui.mvi.BaseViewModel
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObserveFeedStatusUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioHistoryUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.RemoveHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.SearchAssetsUseCase
import dev.madina.tickr.feature.portfolio.ui.mapper.toUi
import dev.madina.tickr.feature.portfolio.ui.mapper.withLatest
import dev.madina.tickr.feature.portfolio.ui.model.AssetUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class PortfolioViewModel(
    private val observePortfolio: ObservePortfolioUseCase,
    private val observeFeedStatus: ObserveFeedStatusUseCase,
    private val observePortfolioHistory: ObservePortfolioHistoryUseCase,
    private val addHolding: AddHoldingUseCase,
    private val removeHolding: RemoveHoldingUseCase,
    private val searchAssets: SearchAssetsUseCase,
) : BaseViewModel<PortfolioAction, PortfolioEffect, PortfolioUiState>(PortfolioUiState()) {
    /** Held so a new keystroke cancels the search in flight rather than racing it. */
    private var searchJob: Job? = null

    init {
        observePortfolio()
            .onEach { portfolio ->
                updateState { previous ->
                    previous.copy(
                        // The day is fetched by its own stream and kept in state, so this reducer
                        // reads it rather than a field and stays pure. Each series is that day with
                        // its final, still-forming hour replaced by the live price.
                        holdings = portfolio.toUi(previous.dayBySymbol),
                        totalValue = portfolio.totalValue,
                        totalProfit = portfolio.totalProfit,
                        totalReturnPercent = portfolio.totalReturnPercent,
                        dayChange = portfolio.dayChange,
                        dayChangePercent = portfolio.dayChangePercent,
                        // The live total only belongs on the chart once every holding has a price.
                        // Quotes arrive one asset at a time, so a partial sum would drop the last
                        // point off a cliff and back again.
                        totalHistory =
                            withLatest(
                                day = previous.dayTotal,
                                live = portfolio.totalValue.takeIf { !portfolio.isPartiallyPriced && it > 0 },
                            ),
                        isPartiallyPriced = portfolio.isPartiallyPriced,
                        isLoading = false,
                    )
                }
            }.catch { throwable ->
                log.e(throwable) { "The portfolio stream failed" }
                updateState { it.copy(isLoading = false, errorMessage = UiMessage.GenericFailure) }
            }
            // launchIn, not launch { collect { } }: with collect, an exception kills the enclosing
            // coroutine and anything after it never runs, silently.
            .launchIn(viewModelScope)

        // A separate stream rather than a combine: the feed blinks on its own schedule and has
        // nothing to say about what the portfolio is worth.
        observeFeedStatus()
            .onEach { status -> updateState { it.copy(feedStatus = status) } }
            .catch { throwable -> log.e(throwable) { "The feed status stream failed" } }
            .launchIn(viewModelScope)

        // The day is fetched over REST and only changes when the portfolio does, while prices
        // arrive several times a second. Its own stream, so a tick does not refetch a day of
        // candles.
        observePortfolioHistory()
            .onEach { day ->
                updateState { previous ->
                    val bySymbol = day.perSymbol.mapValues { (_, closes) -> closes.toFloats() }
                    val total = day.total.toFloats()
                    previous.copy(
                        dayBySymbol = bySymbol,
                        dayTotal = total,
                        // The rows and the total already on screen carry live prices, so the
                        // arriving day is re-applied to them here rather than waiting for the next
                        // tick to redraw a chart the user is looking at.
                        holdings =
                            previous.holdings
                                .map {
                                    it.copy(
                                        history = withLatest(bySymbol[it.symbol] ?: persistentListOf(), it.price),
                                    )
                                }.toImmutableList(),
                        totalHistory =
                            withLatest(
                                day = total,
                                live = previous.totalValue.takeIf { !previous.isPartiallyPriced && it > 0 },
                            ),
                        // A new day is a new series, so an index into the old one means nothing.
                        overviewScrubIndex = null,
                        detailScrubIndex = null,
                    )
                }
            }.catch { throwable -> log.e(throwable) { "The price history stream failed" } }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: PortfolioAction) {
        when (action) {
            // Only the detail's scrub is cleared: it points into the series of whichever asset is
            // open, so it means nothing once a different one is. The overview's chart does not
            // change, and in the two pane layout it stays on screen throughout.
            is PortfolioAction.HoldingClicked ->
                updateState {
                    it.copy(
                        destination = PortfolioDestination.Detail(action.symbol),
                        detailScrubIndex = null,
                    )
                }

            PortfolioAction.BackClicked ->
                updateState {
                    it.copy(destination = PortfolioDestination.Overview, detailScrubIndex = null)
                }

            PortfolioAction.AddClicked -> {
                updateState { it.copy(isAddSheetVisible = true) }
                search(query = "", debounce = false)
            }

            PortfolioAction.AddDismissed -> {
                searchJob?.cancel()
                updateState {
                    it.copy(
                        isAddSheetVisible = false,
                        assetQuery = "",
                        assetResults = persistentListOf(),
                        selectedAsset = null,
                        catalogError = null,
                    )
                }
            }

            is PortfolioAction.AssetQueryChanged -> {
                updateState { it.copy(assetQuery = action.query) }
                search(query = action.query, debounce = true)
            }

            is PortfolioAction.AssetSelected ->
                updateState { state ->
                    state.copy(selectedAsset = state.assetResults.firstOrNull { it.symbol == action.symbol })
                }

            PortfolioAction.AssetSelectionCleared -> updateState { it.copy(selectedAsset = null) }

            PortfolioAction.AboutClicked -> updateState { it.copy(isAboutVisible = true) }

            PortfolioAction.AboutDismissed -> updateState { it.copy(isAboutVisible = false) }

            is PortfolioAction.AddConfirmed -> confirmAdd(action)

            is PortfolioAction.RemoveClicked -> remove(action.symbol)

            is PortfolioAction.Scrubbed ->
                updateState { state ->
                    // Clamped against the series the pointer is actually on. The two charts hold a
                    // different number of samples, so one shared index would land the marker on the
                    // wrong point even before the two pane layout showed both at once.
                    val series =
                        when (action.chart) {
                            PortfolioAction.Chart.Overview -> state.totalHistory
                            PortfolioAction.Chart.Detail -> state.selectedHolding?.history.orEmpty()
                        }
                    // coerceIn over an empty range throws, and a series is empty until its first
                    // quote lands, which is exactly when a stray pointer event can arrive.
                    val index =
                        action.index
                            ?.takeIf { series.isNotEmpty() }
                            ?.coerceIn(0, series.lastIndex)

                    when (action.chart) {
                        PortfolioAction.Chart.Overview -> state.copy(overviewScrubIndex = index)
                        PortfolioAction.Chart.Detail -> state.copy(detailScrubIndex = index)
                    }
                }

            PortfolioAction.ErrorDismissed ->
                updateState { it.copy(errorMessage = null, catalogError = null) }
        }
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                // The first search fetches the catalogue over the network; every later one is filtered
                // from the cache. Debouncing keeps a fast typist from queueing a search per keystroke.
                if (debounce) delay(SearchDebounceMillis)

                updateState { it.copy(isCatalogLoading = true, catalogError = null) }

                searchAssets(
                    SearchAssetsUseCase.Params(
                        query = query,
                        excludedSymbols = currentState.holdings.map { it.symbol }.toSet(),
                    ),
                ).onSuccess { assets ->
                    updateState { state ->
                        state.copy(
                            isCatalogLoading = false,
                            assetResults =
                                assets
                                    .map { AssetUi(it.symbol, it.name, assetColor(it.symbol)) }
                                    .toImmutableList(),
                        )
                    }
                }.onFailure { throwable ->
                    log.e(throwable) { "Could not load the asset catalogue" }
                    updateState {
                        it.copy(
                            isCatalogLoading = false,
                            catalogError = UiMessage.CatalogueUnavailable,
                        )
                    }
                }
            }
    }

    private fun confirmAdd(action: PortfolioAction.AddConfirmed) {
        val asset = currentState.selectedAsset
        if (asset == null) {
            emitEffect(PortfolioEffect.ShowMessage(UiMessage.PickAnAssetFirst))
            return
        }

        // takeIf(isFinite) rather than a bare parse: "NaN" and "Infinity" are both valid to
        // toDoubleOrNull, and the sheet is not the only caller that has to hold this line.
        val quantity =
            action.quantity
                .trim()
                .toDoubleOrNull()
                ?.takeIf { it.isFinite() }
        val averageCost =
            action.averageCost
                .trim()
                .toDoubleOrNull()
                ?.takeIf { it.isFinite() }
        if (quantity == null || averageCost == null) {
            emitEffect(PortfolioEffect.ShowMessage(UiMessage.NumbersRequired))
            return
        }

        viewModelScope.launch {
            addHolding(
                AddHoldingUseCase.Params(
                    symbol = asset.symbol,
                    name = asset.name,
                    quantity = quantity,
                    averageCost = averageCost,
                ),
            ).onSuccess {
                updateState {
                    it.copy(
                        isAddSheetVisible = false,
                        assetQuery = "",
                        assetResults = persistentListOf(),
                        selectedAsset = null,
                    )
                }
            }.onFailure { throwable ->
                log.e(throwable) { "Could not add the holding" }
                emitEffect(PortfolioEffect.ShowMessage(UiMessage.GenericFailure))
            }
        }
    }

    private fun remove(symbol: String) {
        viewModelScope.launch {
            removeHolding(RemoveHoldingUseCase.Params(symbol))
                .onSuccess {
                    // Leaving the detail open would show a position that no longer exists.
                    updateState { it.copy(destination = PortfolioDestination.Overview) }
                }.onFailure { throwable ->
                    log.e(throwable) { "Could not remove the holding" }
                    emitEffect(PortfolioEffect.ShowMessage(UiMessage.GenericFailure))
                }
        }
    }
}

private fun List<Double>.toFloats(): ImmutableList<Float> = map { it.toFloat() }.toImmutableList()

private const val SearchDebounceMillis = 220L
