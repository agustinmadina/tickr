package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.madina.tickr.core.ui.component.AllocationBar
import dev.madina.tickr.core.ui.component.AllocationSegment
import dev.madina.tickr.core.ui.component.AnimatedAmount
import dev.madina.tickr.core.ui.format.formatPercent
import dev.madina.tickr.core.ui.format.formatSignedUsd
import dev.madina.tickr.core.ui.format.formatUsd
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Positive
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
                // Clears the floating button, so the last row is never trapped underneath it.
                bottom = Spacing.Huge * FabClearanceFactor,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            item { PortfolioHeader(state) }

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
                    modifier = Modifier.animateItem(),
                )
            }

            if (state.isEmpty) {
                item { EmptyState() }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onAction(PortfolioAction.AddClicked) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.ExtraLarge)
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
        ) {
            Text(text = "Add asset", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PortfolioHeader(state: PortfolioUiState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "YOUR PORTFOLIO",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            AnimatedVisibility(visible = state.isPartiallyPriced, enter = fadeIn(), exit = fadeOut()) {
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

        // No tint on the headline figure: at this size a red total reads as an error rather than
        // as a downtick, and it fought with the profit line right underneath it.
        AnimatedAmount(
            value = state.totalValue,
            format = { it.formatUsd() },
            style = MaterialTheme.typography.displayLarge,
            baseColor = MaterialTheme.colorScheme.onBackground,
            flashOnChange = false,
        )

        Spacer(Modifier.height(Spacing.ExtraSmall))

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
private const val FabClearanceFactor = 3
