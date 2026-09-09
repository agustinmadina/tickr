package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.ui.model.AssetUi
import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Which screen is showing is part of the state, not of the composition. Keeping it here means the
 * back stack survives a configuration change, and the same state drives all three platforms.
 */
sealed interface PortfolioDestination {
    data object Overview : PortfolioDestination

    data class Detail(
        val symbol: String,
    ) : PortfolioDestination
}

data class PortfolioUiState(
    val holdings: ImmutableList<HoldingUi> = persistentListOf(),
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalReturnPercent: Double? = null,
    /** Today's move across the whole portfolio, the headline figure. */
    val dayChange: Double = 0.0,
    val dayChangePercent: Double? = null,
    /** Value of the whole portfolio over time, one sample per update, for the header chart. */
    val totalHistory: ImmutableList<Float> = persistentListOf(),
    /**
     * Which sample the user is pointing at, or null when they are not.
     *
     * It lives here rather than in the chart because it changes what the header reads: while
     * scrubbing, the headline figure shows the value at that point in time instead of the latest
     * one. State that drives what another part of the screen displays is not local to a component.
     */
    val scrubIndex: Int? = null,
    val isLoading: Boolean = true,
    val isPartiallyPriced: Boolean = false,
    val destination: PortfolioDestination = PortfolioDestination.Overview,
    val isAddSheetVisible: Boolean = false,
    /**
     * Picker state. The query lives here rather than in the sheet because it drives which results
     * are shown, and the results come from a use case rather than from the composition.
     */
    val assetQuery: String = "",
    val assetResults: ImmutableList<AssetUi> = persistentListOf(),
    val selectedAsset: AssetUi? = null,
    val isCatalogLoading: Boolean = false,
    val catalogError: String? = null,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean = !isLoading && holdings.isEmpty()

    val isScrubbing: Boolean = scrubIndex != null

    /**
     * How the total has moved since the app opened, which is the window the header chart covers.
     *
     * It is a third measure alongside all time return and the rows' 24 hour change, and the only
     * one bounded by the session, since the app keeps no history between launches.
     */
    val sessionChange: Double =
        if (totalHistory.size < 2) {
            0.0
        } else {
            (totalHistory.last() - totalHistory.first()).toDouble()
        }

    /** What the headline shows: the scrubbed point when pointing, the live total otherwise. */
    val displayedValue: Double =
        scrubIndex?.let { totalHistory.getOrNull(it)?.toDouble() } ?: totalValue

    /**
     * Change from the first sample to the scrubbed point, which is what a reader wants while
     * scrubbing: not the profit against cost, but how the portfolio moved up to that moment.
     */
    val scrubbedChange: Double?
        get() {
            val index = scrubIndex ?: return null
            val first = totalHistory.firstOrNull() ?: return null
            val at = totalHistory.getOrNull(index) ?: return null
            return (at - first).toDouble()
        }

    val selectedHolding: HoldingUi?
        get() =
            (destination as? PortfolioDestination.Detail)
                ?.let { detail -> holdings.firstOrNull { it.symbol == detail.symbol } }
}
