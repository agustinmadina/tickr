package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.domain.model.FeedStatus
import dev.madina.tickr.feature.portfolio.domain.model.HistoryRange
import dev.madina.tickr.feature.portfolio.ui.model.AssetUi
import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Which screen is showing is part of the state, not of the composition. Keeping it here means the
 * back stack survives a configuration change, and the same state drives all three platforms.
 */
internal sealed interface PortfolioDestination {
    data object Overview : PortfolioDestination

    data class Detail(
        val symbol: String,
    ) : PortfolioDestination
}

internal data class PortfolioUiState(
    val holdings: ImmutableList<HoldingUi> = persistentListOf(),
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalReturnPercent: Double? = null,
    /** Today's move across the whole portfolio, the headline figure. */
    val dayChange: Double = 0.0,
    val dayChangePercent: Double? = null,
    /**
     * The last 24 hourly closes for the whole portfolio, with its final hour replaced by the live
     * total. The window is the same one the headline percentage covers, which is the point: the
     * chart used to draw the minutes since launch beside a number covering a day.
     */
    val totalHistory: ImmutableList<Float> = persistentListOf(),
    /**
     * The day as fetched, before the live price is laid over it.
     *
     * Kept so both reducers can derive a series without either holding a field: prices and the day
     * arrive on separate streams, and whichever lands second has to rebuild from the same base.
     */
    val dayBySymbol: Map<String, ImmutableList<Float>> = emptyMap(),
    val dayTotal: ImmutableList<Float> = persistentListOf(),
    /** What the user last tapped. Drives the fetch and the selector's highlight, nothing else. */
    val range: HistoryRange = HistoryRange.Day,
    /**
     * What the series currently on screen actually covers, which drives every label.
     *
     * Separate from [range] so picking a new window does not relabel a chart that is still showing
     * the old one. They move together the moment the new series lands. Clearing the charts on the
     * tap instead was worse: every row collapsed and sprang back a second later.
     */
    val displayedRange: HistoryRange = HistoryRange.Day,
    /**
     * Which sample the user is pointing at on each chart, or null when they are not.
     *
     * One index per chart, not one shared. They live here rather than inside the charts because
     * each changes what its own header reads, and a single shared value made pointing at the
     * overview mark the detail as well once the two pane layout put both on screen together.
     */
    val overviewScrubIndex: Int? = null,
    val detailScrubIndex: Int? = null,
    val isLoading: Boolean = true,
    val feedStatus: FeedStatus = FeedStatus.Connecting,
    val isPartiallyPriced: Boolean = false,
    val destination: PortfolioDestination = PortfolioDestination.Overview,
    val isAddSheetVisible: Boolean = false,
    val isAboutVisible: Boolean = false,
    /**
     * Picker state. The query lives here rather than in the sheet because it drives which results
     * are shown, and the results come from a use case rather than from the composition.
     */
    val assetQuery: String = "",
    val assetResults: ImmutableList<AssetUi> = persistentListOf(),
    val selectedAsset: AssetUi? = null,
    val isCatalogLoading: Boolean = false,
    val catalogError: UiMessage? = null,
    val errorMessage: UiMessage? = null,
) {
    val isEmpty: Boolean = !isLoading && holdings.isEmpty()

    val isScrubbing: Boolean = overviewScrubIndex != null

    /**
     * Worth telling the user about only once something is actually missing.
     *
     * A banner during the first second of every launch would be noise; a screen full of blanks
     * with no explanation is worse.
     */
    val isFeedDown: Boolean = feedStatus == FeedStatus.Disconnected

    /**
     * Not one holding has a price yet, which is what a cold start with no network looks like.
     *
     * Distinct from [isPartiallyPriced], which is true whenever *any* holding is missing a quote.
     * The totals sum an unpriced holding as zero so a partial total still means something, but with
     * nothing priced at all that sum is not a small total, it is no total: rendering it as $0.00
     * tells the reader their portfolio is worthless.
     */
    val isUnpriced: Boolean = holdings.isNotEmpty() && holdings.none { it.isPriced }

    /**
     * The whole portfolio's move across the window the chart draws, from its own endpoints.
     *
     * Not [dayChangePercent], which comes from each asset's rolling 24 hour open and is therefore
     * only right for one of the ranges. Taking it from the series is what keeps the number and the
     * line describing the same stretch of time whichever range is selected.
     */
    val rangeChangePercent: Double? =
        totalHistory
            .firstOrNull()
            ?.takeIf { it != 0f && totalHistory.size > 1 }
            ?.let { first -> ((totalHistory.last() - first) / first * PERCENT).toDouble() }

    val chartChange: Double =
        if (totalHistory.size < 2) {
            0.0
        } else {
            (totalHistory.last() - totalHistory.first()).toDouble()
        }

    /** What the headline shows: the scrubbed point when pointing, the live total otherwise. */
    val displayedValue: Double =
        overviewScrubIndex?.let { totalHistory.getOrNull(it)?.toDouble() } ?: totalValue

    /**
     * Change from the first sample to the scrubbed point, which is what a reader wants while
     * scrubbing: not the profit against cost, but how the portfolio moved up to that moment.
     */
    val scrubbedChange: Double?
        get() {
            val index = overviewScrubIndex ?: return null
            val first = totalHistory.firstOrNull() ?: return null
            val at = totalHistory.getOrNull(index) ?: return null
            return (at - first).toDouble()
        }

    /** The same move as a percentage, which is the shape people compare movements in. */
    val scrubbedChangePercent: Double?
        get() {
            val first = totalHistory.firstOrNull()?.toDouble()?.takeIf { it != 0.0 } ?: return null
            return scrubbedChange?.div(first)?.times(PERCENT)
        }

    val selectedHolding: HoldingUi?
        get() =
            (destination as? PortfolioDestination.Detail)
                ?.let { detail -> holdings.firstOrNull { it.symbol == detail.symbol } }
}

private const val PERCENT = 100
