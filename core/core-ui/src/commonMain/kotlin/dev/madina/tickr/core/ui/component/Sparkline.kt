package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/**
 * A line chart with no axes, labels or interaction: it exists to show shape, not to be read.
 *
 * Values are normalised to their own range, so a flat series still renders as a centred line rather
 * than collapsing onto the bottom edge.
 */
@Composable
fun Sparkline(
    points: ImmutableList<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    if (points.size < MinimumPoints) return

    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = AppearDurationMillis),
        label = "sparkline-appear",
    )

    val minimum = points.min()
    val maximum = points.max()
    val range = (maximum - minimum).takeIf { it > 0f }

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)

        fun yOf(value: Float): Float {
            // A flat series has no range to normalise against, so it is pinned to the middle.
            val normalised = range?.let { (value - minimum) / it } ?: MidPoint
            val usableHeight = size.height * appear
            return size.height - (normalised * usableHeight)
        }

        val line =
            Path().apply {
                moveTo(0f, yOf(points.first()))
                points.forEachIndexed { index, value ->
                    if (index > 0) lineTo(index * stepX, yOf(value))
                }
            }

        if (filled) {
            val area =
                Path().apply {
                    addPath(line)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            drawPath(
                path = area,
                brush =
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = FillAlpha), Color.Transparent),
                        startY = 0f,
                        endY = size.height,
                    ),
            )
        }

        // DrawScope is a Density, so dp converts here. Using `.value` would treat dp as px and
        // render a hairline on dense screens.
        drawPath(path = line, color = color, style = Stroke(width = StrokeWidth.toPx()))

        val lastPoint = Offset(size.width, yOf(points.last()))
        drawCircle(color = color, radius = EndDotRadius.toPx(), center = lastPoint)
    }
}

private const val MinimumPoints = 2
private const val AppearDurationMillis = 700
private const val MidPoint = 0.5f
private const val FillAlpha = 0.22f
private val StrokeWidth = 2.dp
private val EndDotRadius = 3.dp
