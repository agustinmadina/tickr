package dev.madina.tickr.feature.portfolio.ui

import androidx.lifecycle.viewModelScope
import dev.madina.tickr.core.ui.mvi.BaseViewModel
import dev.madina.tickr.core.ui.theme.assetColor
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.RemoveHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.SearchAssetsUseCase
import dev.madina.tickr.feature.portfolio.ui.mapper.toUi
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
    private val addHolding: AddHoldingUseCase,
    private val removeHolding: RemoveHoldingUseCase,
    private val searchAssets: SearchAssetsUseCase,
) : BaseViewModel<PortfolioAction, PortfolioEffect, PortfolioUiState>(PortfolioUiState()) {
    /** Held so a new keystroke cancels the search in flight rather than racing it. */
    private var searchJob: Job? = null

    init {
        observePortfolio(Unit)
            .onEach { portfolio ->
                updateState { previous ->
                    previous.copy(
                        // The previous holdings carry the price history, so the reducer derives the
                        // new samples from the old state rather than from a field, keeping it pure.
                        holdings = portfolio.toUi(previous.holdings.associate { it.symbol to it.history }),
                        totalValue = portfolio.totalValue,
                        totalProfit = portfolio.totalProfit,
                        totalReturnPercent = portfolio.totalReturnPercent,
                        dayChange = portfolio.dayChange,
                        dayChangePercent = portfolio.dayChangePercent,
                        // Only once every holding has a price. Quotes arrive one asset at a time,
                        // so the first few totals are partial sums, and charting them drew a
                        // vertical climb out of nothing while the feed filled in. A point on this
                        // series has to be comparable with the ones beside it, which means it must
                        // cover the whole portfolio.
                        totalHistory =
                            if (!portfolio.isPartiallyPriced && portfolio.totalValue > 0) {
                                previous.totalHistory.append(portfolio.totalValue)
                            } else {
                                previous.totalHistory
                            },
                        isPartiallyPriced = portfolio.isPartiallyPriced,
                        isLoading = false,
                    )
                }
            }.catch { throwable ->
                updateState { it.copy(isLoading = false, errorMessage = throwable.readableMessage()) }
            }
            // launchIn, not launch { collect { } }: with collect, an exception kills the enclosing
            // coroutine and anything after it never runs, silently.
            .launchIn(viewModelScope)
    }

    override fun onAction(action: PortfolioAction) {
        when (action) {
            // Both screens have a chart and share the scrub index, since only one is ever visible.
            // Clearing it on navigation stops the marker from arriving already placed on the next
            // chart, which has an unrelated series behind it.
            is PortfolioAction.HoldingClicked ->
                updateState {
                    it.copy(destination = PortfolioDestination.Detail(action.symbol), scrubIndex = null)
                }

            PortfolioAction.BackClicked ->
                updateState { it.copy(destination = PortfolioDestination.Overview, scrubIndex = null) }

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
                    // Clamped against whichever series is on screen: the two charts hold a different
                    // number of samples, so bounding the detail's index by the overview's would land
                    // the marker on the wrong point.
                    val series =
                        when (state.destination) {
                            PortfolioDestination.Overview -> state.totalHistory
                            is PortfolioDestination.Detail -> state.selectedHolding?.history.orEmpty()
                        }
                    // coerceIn over an empty range throws, and a series is empty until its first quote
                    // lands, which is exactly when a stray pointer event can arrive.
                    val index =
                        action.index
                            ?.takeIf { series.isNotEmpty() }
                            ?.coerceIn(0, series.lastIndex)
                    state.copy(scrubIndex = index)
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
                    updateState {
                        it.copy(
                            isCatalogLoading = false,
                            catalogError = "Could not load the asset list. Check your connection.",
                        )
                    }
                }
            }
    }

    private fun confirmAdd(action: PortfolioAction.AddConfirmed) {
        val asset = currentState.selectedAsset
        if (asset == null) {
            emitEffect(PortfolioEffect.ShowMessage("Pick an asset first"))
            return
        }

        val quantity = action.quantity.trim().toDoubleOrNull()
        val averageCost = action.averageCost.trim().toDoubleOrNull()
        if (quantity == null || averageCost == null) {
            emitEffect(PortfolioEffect.ShowMessage("Quantity and average cost must be numbers"))
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
                emitEffect(PortfolioEffect.ShowMessage(throwable.readableMessage()))
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
                    emitEffect(PortfolioEffect.ShowMessage(throwable.readableMessage()))
                }
        }
    }
}

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong"

/**
 * Appends a sample to the total's history, dropping a repeat of the last value.
 *
 * Skipping the repeat is what makes this safe inside the reducer, which `MutableStateFlow.update`
 * may re-run under contention, and it also keeps a still market from filling the chart with a
 * straight line of identical points.
 */
private fun ImmutableList<Float>.append(value: Double): ImmutableList<Float> {
    val sample = value.toFloat()
    if (lastOrNull() == sample) return this
    return (this + sample).takeLast(MaxTotalSamples).toImmutableList()
}

/** Roughly a few minutes of feed at the current tick rate, which is all the chart can resolve. */
private const val MaxTotalSamples = 120
private const val SearchDebounceMillis = 220L
