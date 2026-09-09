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
import androidx.compose.ui.text.style.TextOverflow
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

            // Fixed width, not weight and not wrap. A weight reserved half the row and left a gap
            // before the line; wrapping made every chart start wherever its own label happened to
            // end, so "0.241 BTC" and "940 XRP" pushed their charts to different places and the
            // column of lines looked ragged. A fixed width lines them all up.
            Column(modifier = Modifier.width(LabelColumnWidth)) {
                Text(
                    text = holding.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${holding.quantity.formatQuantity()} ${holding.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Sparkline(
                points = holding.history,
                color = if ((holding.changePercent24h ?: 0.0) >= 0) Positive else Negative,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.Small)
                        .height(SparklineHeight),
            )

            Column(
                modifier = Modifier.width(AmountColumnWidth),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall / 2),
            ) {
                if (holding.value != null) {
                    // No directional tint, for the same reason the headline lost it: this figure
                    // sat red for the last downtick while the percentage beside it read green for
                    // the day, and three rows doing that at once looks like a broken screen. The
                    // pulsing dot and the moving line already say the data is live.
                    AnimatedAmount(
                        value = holding.value,
                        format = { it.formatUsd() },
                        style = MaterialTheme.typography.titleMedium,
                        baseColor = MaterialTheme.colorScheme.onSurface,
                        flashOnChange = false,
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

/**
 * Fixed so every row's chart starts and ends on the same vertical line, and kept tight: whatever
 * these two reserve is taken from the chart between them, which was left with barely a third of the
 * row. Sized for the widest real content, "10,000 DOGE" and a five figure amount.
 */
private val LabelColumnWidth = 86.dp
private val AmountColumnWidth = 100.dp
private val SparklineHeight = Sizing.SparklineHeight / 2
