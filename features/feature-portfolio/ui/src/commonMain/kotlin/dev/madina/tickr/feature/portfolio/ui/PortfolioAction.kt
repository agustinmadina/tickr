package dev.madina.tickr.feature.portfolio.ui

sealed interface PortfolioAction {
    data class HoldingClicked(
        val symbol: String,
    ) : PortfolioAction

    data object BackClicked : PortfolioAction

    data object AddClicked : PortfolioAction

    data object AddDismissed : PortfolioAction

    data object AboutClicked : PortfolioAction

    data object AboutDismissed : PortfolioAction

    data class AssetQueryChanged(
        val query: String,
    ) : PortfolioAction

    data class AssetSelected(
        val symbol: String,
    ) : PortfolioAction

    data object AssetSelectionCleared : PortfolioAction

    /**
     * Only the amounts: which asset is being added comes from the selection, so a symbol that the
     * exchange does not quote cannot be typed in.
     */
    data class AddConfirmed(
        val quantity: String,
        val averageCost: String,
    ) : PortfolioAction

    data class RemoveClicked(
        val symbol: String,
    ) : PortfolioAction

    /**
     * Which chart the pointer is on, because in the two pane layout both are on screen at once and
     * a single shared index made hovering one of them mark the other too.
     */
    enum class Chart { Overview, Detail }

    /** Index of the chart sample being pointed at, or null when the pointer leaves. */
    data class Scrubbed(
        val chart: Chart,
        val index: Int?,
    ) : PortfolioAction

    data object ErrorDismissed : PortfolioAction
}
