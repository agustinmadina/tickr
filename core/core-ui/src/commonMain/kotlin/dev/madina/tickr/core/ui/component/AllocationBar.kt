package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Sizing
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.min

data class AllocationSegment(
    val key: String,
    val weight: Double,
    val color: Color,
)

/**
 * Proportional bar drawn as one gradient rather than as separate blocks.
 *
 * The first version was a `Row` of boxes with a gap between them, which let the background through
 * and read as four unrelated bars. A portfolio is one thing divided up, so the colours now hand
 * over to each other: each segment holds its own colour across most of its width and blends into
 * the next one at the boundary.
 *
 * Widths animate, so when prices move the split shifts gradually instead of the bar re-laying out
 * on every tick.
 */
@Composable
fun AllocationBar(
    segments: ImmutableList<AllocationSegment>,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.weight }
    if (total <= 0.0) return

    val animatedWeights =
        segments.map { segment ->
            animateFloatAsState(
                targetValue = (segment.weight / total).toFloat().coerceAtLeast(MinimumWeight),
                animationSpec = tween(durationMillis = ResizeDurationMillis),
                label = "allocation-${segment.key}",
            ).value
        }

    val animatedTotal = animatedWeights.sum().takeIf { it > 0f } ?: return
    val stops = colourStops(segments, animatedWeights, animatedTotal)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(Sizing.AllocationBarHeight)
                .clip(RoundedCornerShape(Radius.Small)),
    ) {
        drawRect(brush = Brush.horizontalGradient(colorStops = stops.toTypedArray()))
    }
}

/**
 * Two stops per segment: one just after it starts and one just before it ends, both in its own
 * colour. The gradient then interpolates across the small gap left between neighbours, which is
 * the hand-off. The blend is capped at half a segment's width so a tiny holding still shows its
 * colour instead of being swallowed by the two beside it.
 */
private fun colourStops(
    segments: List<AllocationSegment>,
    weights: List<Float>,
    total: Float,
): List<Pair<Float, Color>> {
    val stops = mutableListOf<Pair<Float, Color>>()
    var cursor = 0f

    segments.forEachIndexed { index, segment ->
        val width = weights[index] / total
        val start = cursor
        val end = cursor + width
        val blend = min(BlendFraction, width / 2f)

        val from = if (index == 0) start else start + blend
        val to = if (index == segments.lastIndex) end else end - blend

        stops += from.coerceIn(0f, 1f) to segment.color
        stops += to.coerceIn(0f, 1f) to segment.color
        cursor = end
    }

    return stops
}

/** A holding worth almost nothing still gets a sliver, so the bar accounts for everything held. */
private const val MinimumWeight = 0.01f
private const val ResizeDurationMillis = 600

/** How much of each segment is given over to fading into its neighbour. */
private const val BlendFraction = 0.02f
