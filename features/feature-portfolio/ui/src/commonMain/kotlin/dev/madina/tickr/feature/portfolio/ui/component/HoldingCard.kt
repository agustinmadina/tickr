package dev.madina.tickr.feature.portfolio.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.madina.tickr.core.ui.component.AnimatedAmount
import dev.madina.tickr.core.ui.component.LiveDot
import dev.madina.tickr.core.ui.component.Sparkline
import dev.madina.tickr.core.ui.format.formatPercent
import dev.madina.tickr.core.ui.format.formatQuantity
import dev.madina.tickr.core.ui.format.formatUsd
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Positive
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Sizing
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.SurfaceElevated
import dev.madina.tickr.core.ui.theme.TextSecondary
import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi

@Composable
internal fun HoldingCard(
    holding: HoldingUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) holding.accent else Color.Transparent,
        label = "holding-card-selection",
    )

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = SurfaceElevated,
        shape = RoundedCornerShape(Radius.Large),
        border = BorderStroke(SelectionBorderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveDot(color = holding.accent)

            Spacer(Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(SymbolWeight)) {
                Text(
                    text = holding.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${holding.quantity.formatQuantity()} ${holding.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Sparkline(
                points = holding.history,
                color = if ((holding.changePercent24h ?: 0.0) >= 0) Positive else Negative,
                modifier =
                    Modifier
                        .weight(SparklineWeight)
                        .height(SparklineHeight),
            )

            Spacer(Modifier.width(Spacing.Medium))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
            ) {
                if (holding.value != null) {
                    AnimatedAmount(
                        value = holding.value,
                        format = { it.formatUsd() },
                        style = MaterialTheme.typography.titleMedium,
                        baseColor = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    // A position whose price has not arrived reads as pending, not as worth zero.
                    // ASCII on purpose: the web build's bundled font has no em dash.
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                }
                holding.changePercent24h?.let { change ->
                    // Suffixed, because the headline above reports all time return and an
                    // unlabelled percentage here invites the reader to compare the two.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = change.formatPercent(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (change >= 0) Positive else Negative,
                        )
                        Spacer(Modifier.width(Spacing.ExtraSmall))
                        Text(
                            text = "24h",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

private val SelectionBorderWidth = 1.dp
private const val SymbolWeight = 1f
private const val SparklineWeight = 0.9f
private val SparklineHeight = Sizing.SparklineHeight / 2
