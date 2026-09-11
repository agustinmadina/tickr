package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    // Still occupies its space when there is nothing to draw. Returning outright emitted no node at
    // all, so the caller's `weight` vanished and every row without a line yet laid itself out
    // differently: amounts drifted to the middle until the first two quotes arrived.
    if (points.size < MinimumPoints) {
        Spacer(modifier)
        return
    }

    // Started at zero and animated up. animateFloatAsState(1f) remembers its Animatable at the
    // initial target, so it sat at 1f from the first frame and the reveal never ran.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(durationMillis = AppearDurationMillis))
    }

    val scale = ChartRange(points)

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)

        fun yOf(value: Float): Float {
            val usableHeight = size.height * appear.value
            return size.height - (scale.fractionOf(value) * usableHeight)
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
private const val FillAlpha = 0.22f
private val StrokeWidth = 2.dp
private val EndDotRadius = 3.dp
