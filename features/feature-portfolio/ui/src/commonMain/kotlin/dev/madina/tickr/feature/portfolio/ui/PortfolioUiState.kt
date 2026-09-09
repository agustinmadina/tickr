package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Which screen is showing is part of the state, not of the composition. Keeping it here means the
 * back stack survives a configuration change, and the same state drives all three platforms.
 */
sealed interface PortfolioDestination {
    data object Overview : PortfolioDestination

    data class Detail(val symbol: String) : PortfolioDestination
}

data class PortfolioUiState(
    val holdings: ImmutableList<HoldingUi> = persistentListOf(),
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalReturnPercent: Double? = null,
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
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean = !isLoading && holdings.isEmpty()

    val isScrubbing: Boolean = scrubIndex != null

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
        get() = (destination as? PortfolioDestination.Detail)
            ?.let { detail -> holdings.firstOrNull { it.symbol == detail.symbol } }
}
