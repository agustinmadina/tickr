package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Sizing
import kotlinx.collections.immutable.ImmutableList

data class AllocationSegment(
    val key: String,
    val weight: Double,
    val color: Color,
)

/**
 * Proportional bar. Each segment animates its own width, so when prices move the split shifts
 * gradually rather than the whole bar re-laying out on every tick.
 *
 * Each segment must declare a height: children of a fixed-height `Row` do not inherit it, and a
 * `Box` with only a weight measures zero tall and renders nothing at all.
 */
@Composable
fun AllocationBar(
    segments: ImmutableList<AllocationSegment>,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.weight }
    if (total <= 0.0) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Sizing.AllocationBarHeight)
                .clip(RoundedCornerShape(Radius.Small)),
        horizontalArrangement = Arrangement.spacedBy(SegmentGap),
    ) {
        segments.forEach { segment ->
            val weight by animateFloatAsState(
                targetValue = (segment.weight / total).toFloat().coerceAtLeast(MinimumWeight),
                animationSpec = tween(durationMillis = ResizeDurationMillis),
                label = "allocation-${segment.key}",
            )
            Box(
                modifier =
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(segment.color),
            )
        }
    }
}

/** A holding worth almost nothing still gets a sliver, so the bar accounts for everything held. */
private const val MinimumWeight = 0.01f
private const val ResizeDurationMillis = 600
private val SegmentGap = 2.dp
