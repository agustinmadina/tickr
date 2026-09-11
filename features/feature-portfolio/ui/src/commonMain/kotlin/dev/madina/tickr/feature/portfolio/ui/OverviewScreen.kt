package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.madina.tickr.core.ui.component.AllocationBar
import dev.madina.tickr.core.ui.component.AllocationSegment
import dev.madina.tickr.core.ui.component.AnimatedAmount
import dev.madina.tickr.core.ui.component.InteractiveLineChart
import dev.madina.tickr.core.ui.format.formatPercent
import dev.madina.tickr.core.ui.format.formatSignedUsd
import dev.madina.tickr.core.ui.format.formatUsd
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Positive
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Sizing
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.TextSecondary
import dev.madina.tickr.feature.portfolio.ui.component.HoldingCard
import dev.madina.tickr.feature.portfolio.ui.component.PortfolioSkeleton
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import tickr.features.feature_portfolio.ui.generated.resources.Res
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_add_asset
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_author
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_byline_prefix
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_empty_body
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_empty_title
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_offline_banner
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_platforms
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_pricing_badge
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_return_vs_cost
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_scrubbed_change
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_title
import tickr.features.feature_portfolio.ui.generated.resources.portfolio_title_scrubbing

@Composable
internal fun OverviewScreen(
    state: PortfolioUiState,
    onAction: (PortfolioAction) -> Unit,
    modifier: Modifier = Modifier,
    /** Marks the open row. Only meaningful in the two pane layout, where the detail is visible. */
    selectedSymbol: String? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = Sizing.ContentMaxWidth),
            contentPadding =
                PaddingValues(
                    start = Spacing.Large,
                    end = Spacing.Large,
                    // Status bar and notch folded into the padding rather than added as a leading
                    // spacer item, which stacked on top of it and pushed the header down the screen.
                    top = Spacing.Large + WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                    bottom = Spacing.Huge + WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            item { Byline(onClick = { onAction(PortfolioAction.AboutClicked) }) }

            item {
                Spacer(Modifier.height(Spacing.Small))
                PortfolioHeader(state)
            }

            if (state.totalHistory.size >= MinimumChartPoints) {
                item {
                    Column {
                        InteractiveLineChart(
                            points = state.totalHistory,
                            // Coloured by the move it actually draws, which is also what the
                            // percentage beside every row reports for the same window.
                            color = if ((state.rangeChangePercent ?: 0.0) >= 0) Positive else Negative,
                            scrubIndex = state.overviewScrubIndex,
                            onScrub = { index ->
                                onAction(
                                    PortfolioAction.Scrubbed(PortfolioAction.Chart.Overview, index),
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(Sizing.HeaderChartHeight),
                        )
                        Spacer(Modifier.height(Spacing.ExtraSmall))
                        // The caption sits beside the picker rather than under the chart alone:
                        // the window is now a choice, so naming it next to the control that
                        // changes it saves the reader working out which button did what.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.displayedRange.windowLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            RangePicker(
                                selected = state.range,
                                onSelect = { onAction(PortfolioAction.RangeSelected(it)) },
                            )
                        }
                    }
                }
            }

            if (state.isFeedDown) {
                item { FeedDownBanner() }
            }

            if (state.holdings.isNotEmpty()) {
                item {
                    AllocationBar(
                        segments =
                            state.holdings
                                .map { AllocationSegment(key = it.symbol, weight = it.value ?: 0.0, color = it.accent) }
                                .toImmutableList(),
                    )
                }
            }

            item { Spacer(Modifier.height(Spacing.ExtraSmall)) }

            if (state.isLoading) {
                items(SkeletonRowCount) { PortfolioSkeleton() }
            }

            items(state.holdings, key = { it.symbol }) { holding ->
                HoldingCard(
                    holding = holding,
                    range = state.displayedRange,
                    onClick = { onAction(PortfolioAction.HoldingClicked(holding.symbol)) },
                    isSelected = holding.symbol == selectedSymbol,
                    modifier = Modifier.animateItem(),
                )
            }

            if (state.isEmpty) {
                item { EmptyState() }
            }

            if (!state.isLoading) {
                item { AddAssetRow(onClick = { onAction(PortfolioAction.AddClicked) }) }
            }
        }
    }
}

/**
 * The add action sits at the end of the list rather than floating over it. A floating button
 * covered the last card, and it is not what a web visitor expects, which is where this demo is
 * mostly seen.
 */
@Composable
private fun AddAssetRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(Radius.Large),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.portfolio_add_asset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FeedDownBanner() {
    // Prices are not persisted, so offline every value on this screen is blank. Saying so turns a
    // screen that looks broken into one that looks deliberate, and it is cheaper than caching a
    // quote whose age the user cannot see.
    Surface(
        color = Negative.copy(alpha = BannerFillAlpha),
        shape = RoundedCornerShape(Radius.Medium),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.portfolio_offline_banner),
            style = MaterialTheme.typography.bodySmall,
            color = Negative,
            modifier = Modifier.padding(Spacing.Medium),
        )
    }
}

/**
 * Signature and the way into [AboutSheet], in one line.
 *
 * A demo is read by someone deciding whose work it is, and a portfolio piece with no name on it
 * makes them go looking. Putting it in the layout rather than over the content keeps it out of the
 * way of the app itself, and tapping it explains the project rather than just crediting it.
 */
@Composable
private fun Byline(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(Radius.Small))
                    .clickable(onClick = onClick)
                    .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.portfolio_byline_prefix),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            // The name carries the accent colour rather than the muted one. Small grey text in a
            // corner reads as a watermark and nobody taps it, while in this app blue already means
            // tappable, from the add row to the links in the sheet. So it needs no icon, border or
            // extra element to be understood as one.
            Text(
                text = stringResource(Res.string.portfolio_author),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Grey and outside the clickable area. Blue means tappable here, and the name above it
        // already is; two tappable things this close would fight. This one is only a fact, and it
        // is the fact somebody opening the web build has no other way to learn without reading the
        // repository: that the page they are on is the phone app, compiled.
        Text(
            text = stringResource(Res.string.portfolio_platforms),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            // End, not the default start: a longer translation, or a narrower phone, wraps this
            // and the overflow used to land hard left under a right-aligned first line.
            textAlign = TextAlign.End,
            modifier = Modifier.padding(horizontal = Spacing.Small),
        )
    }
}

@Composable
private fun PortfolioHeader(state: PortfolioUiState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                    stringResource(
                        if (state.isScrubbing) {
                            Res.string.portfolio_title_scrubbing
                        } else {
                            Res.string.portfolio_title
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            AnimatedVisibility(
                visible = state.isPartiallyPriced && !state.isUnpriced && !state.isScrubbing,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(Spacing.Small))
                    Text(
                        text = stringResource(Res.string.portfolio_pricing_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.Small))

        if (state.isScrubbing) {
            // Rendered directly rather than through AnimatedAmount: while a finger is moving, every
            // sample would start a new count animation and the figure would lag behind the pointer
            // instead of tracking it.
            Text(
                text = state.displayedValue.formatUsd(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } else if (state.isUnpriced) {
            // Unknown, not zero. $0.00 here is a statement about the portfolio's worth, and it is
            // the wrong one.
            Text(
                text = Pending,
                style = MaterialTheme.typography.displayLarge,
                color = TextSecondary,
            )
        } else {
            // No tint on the headline figure: at this size a red total reads as an error rather
            // than as a downtick, and it fought with the profit line right underneath it.
            AnimatedAmount(
                value = state.totalValue,
                format = { it.formatUsd() },
                style = MaterialTheme.typography.displayLarge,
                baseColor = MaterialTheme.colorScheme.onBackground,
                flashOnChange = false,
            )
        }

        Spacer(Modifier.height(Spacing.ExtraSmall))

        val scrubbedChange = state.scrubbedChange
        if (state.isUnpriced) {
            // Every figure below the headline is derived from prices, so there is nothing truthful
            // to put here. The banner above says why.
        } else if (scrubbedChange != null) {
            // Money and percent, worded exactly as the detail screen does while scrubbing: the two
            // charts behave the same way, so reading one should teach you the other.
            val percentSuffix =
                state.scrubbedChangePercent
                    ?.let { " (${it.formatPercent()})" }
                    .orEmpty()
            Text(
                text =
                    stringResource(
                        Res.string.portfolio_scrubbed_change,
                        scrubbedChange.formatSignedUsd(),
                        percentSuffix,
                    ),
                style = MaterialTheme.typography.titleMedium,
                color = if (scrubbedChange >= 0) Positive else Negative,
            )
        } else {
            // The market move first. It is what a reader looks for, it is the same measure as the
            // per-row percentages underneath, and unlike return against cost it depends on the
            // market rather than on a number the user typed in.
            //
            // It follows the selected window, like everything else on the screen. Left on "today"
            // it read "+2.78% today" above a chart covering a year, which is the largest figure
            // here disagreeing with the largest picture.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedAmount(
                    value = state.chartChange,
                    format = { it.formatSignedUsd() },
                    style = MaterialTheme.typography.titleMedium,
                    baseColor = if (state.chartChange >= 0) Positive else Negative,
                    flashOnChange = false,
                )
                state.rangeChangePercent?.let { percent ->
                    Spacer(Modifier.width(Spacing.Small))
                    Text(
                        text = "(${percent.formatPercent()})",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (percent >= 0) Positive else Negative,
                    )
                }
                Spacer(Modifier.width(Spacing.Small))
                Text(
                    text = state.displayedRange.windowLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            // Return against cost is demoted to a second line and named after its reference, since
            // "all time" still did not say all time against what.
            state.totalReturnPercent?.let { percent ->
                Spacer(Modifier.height(Spacing.ExtraSmall))
                Text(
                    text =
                        stringResource(
                            Res.string.portfolio_return_vs_cost,
                            state.totalProfit.formatSignedUsd(),
                            percent.formatPercent(),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.portfolio_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.Small))
        Text(
            text = stringResource(Res.string.portfolio_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Spacing.Large))
    }
}

/** Tinted, not solid: a warning the reader can skip past, not an error state. */
private const val BannerFillAlpha = 0.12f

private const val SkeletonRowCount = 4

/** Below this the chart is a line between two dots, which reads as broken rather than as early. */
private const val MinimumChartPoints = 4
