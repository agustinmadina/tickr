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
            contentPadding = PaddingValues(
                start = Spacing.Large,
                end = Spacing.Large,
                // Status bar and notch folded into the padding rather than added as a leading
                // spacer item, which stacked on top of it and pushed the header down the screen.
                top = Spacing.Large + WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                bottom = Spacing.Huge + WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            item { PortfolioHeader(state) }

            if (state.totalHistory.size >= MinimumChartPoints) {
                item {
                    InteractiveLineChart(
                        points = state.totalHistory,
                        color = if (state.totalProfit >= 0) Positive else Negative,
                        scrubIndex = state.scrubIndex,
                        onScrub = { index -> onAction(PortfolioAction.Scrubbed(index)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizing.HeaderChartHeight),
                    )
                }
            }

            if (state.holdings.isNotEmpty()) {
                item {
                    AllocationBar(
                        segments = state.holdings
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
                text = "+  Add asset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PortfolioHeader(state: PortfolioUiState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.isScrubbing) "AT THIS POINT" else "YOUR PORTFOLIO",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            AnimatedVisibility(
                visible = state.isPartiallyPriced && !state.isScrubbing,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(Spacing.Small))
                    Text(
                        text = "PRICING",
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
        if (scrubbedChange != null) {
            Text(
                text = "${scrubbedChange.formatSignedUsd()} since the start of this chart",
                style = MaterialTheme.typography.titleMedium,
                color = if (scrubbedChange >= 0) Positive else Negative,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedAmount(
                    value = state.totalProfit,
                    format = { it.formatSignedUsd() },
                    style = MaterialTheme.typography.titleMedium,
                    baseColor = if (state.totalProfit >= 0) Positive else Negative,
                    flashOnChange = false,
                )
                state.totalReturnPercent?.let { percent ->
                    Spacer(Modifier.width(Spacing.Small))
                    Text(
                        text = "(${percent.formatPercent()})",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (percent >= 0) Positive else Negative,
                    )
                }
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
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.Small))
        Text(
            text = "Add an asset to start tracking what it is worth.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(Spacing.Large))
    }
}

private const val SkeletonRowCount = 4
/** Below this the chart is a line between two dots, which reads as broken rather than as early. */
private const val MinimumChartPoints = 4
