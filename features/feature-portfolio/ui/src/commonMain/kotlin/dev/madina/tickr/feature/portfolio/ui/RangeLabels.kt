package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.runtime.Composable
import dev.madina.tickr.feature.portfolio.domain.model.HistoryRange
import org.jetbrains.compose.resources.stringResource
import tickr.features.feature_portfolio.ui.generated.resources.Res
import tickr.features.feature_portfolio.ui.generated.resources.range_day_short
import tickr.features.feature_portfolio.ui.generated.resources.range_day_window
import tickr.features.feature_portfolio.ui.generated.resources.range_month_short
import tickr.features.feature_portfolio.ui.generated.resources.range_month_window
import tickr.features.feature_portfolio.ui.generated.resources.range_week_short
import tickr.features.feature_portfolio.ui.generated.resources.range_week_window
import tickr.features.feature_portfolio.ui.generated.resources.range_year_short
import tickr.features.feature_portfolio.ui.generated.resources.range_year_window

/**
 * The window's name, in the two lengths the screen needs.
 *
 * Kept together so the selector, the caption under the chart and the suffix on every row can never
 * disagree about what is being shown. The suffix used to be a hardcoded "24h" in the row, which is
 * exactly how a chart and the number beside it end up describing different stretches of time.
 */
@Composable
internal fun HistoryRange.shortLabel(): String =
    stringResource(
        when (this) {
            HistoryRange.Day -> Res.string.range_day_short
            HistoryRange.Week -> Res.string.range_week_short
            HistoryRange.Month -> Res.string.range_month_short
            HistoryRange.Year -> Res.string.range_year_short
        },
    )

@Composable
internal fun HistoryRange.windowLabel(): String =
    stringResource(
        when (this) {
            HistoryRange.Day -> Res.string.range_day_window
            HistoryRange.Week -> Res.string.range_week_window
            HistoryRange.Month -> Res.string.range_month_window
            HistoryRange.Year -> Res.string.range_year_window
        },
    )
