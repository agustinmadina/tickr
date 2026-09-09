package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList

/**
 * A line chart the user can run a finger or a cursor along, reporting which sample they are on.
 *
 * Input is handled twice on purpose, and this is the whole point of the component on a
 * multiplatform project: [detectHorizontalDragGestures] covers touch, and a pointer loop watching
 * `Move` and `Exit` covers a mouse hovering with no button held. One is useless on a phone and the
 * other is useless in a browser, and neither needs a platform-specific source set.
 *
 * The drag detector is horizontal only, which is what lets this live inside a vertically scrolling
 * list: Compose gives a horizontal drag to the chart and a vertical one to the list, so scrolling
 * past the chart still works.
 */
@Composable
fun InteractiveLineChart(
    points: ImmutableList<Float>,
    color: Color,
    scrubIndex: Int?,
    onScrub: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (points.size < MinimumPoints) return

    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = AppearDurationMillis),
        label = "chart-appear",
    )

    val minimum = points.min()
    val maximum = points.max()
    val range = (maximum - minimum).takeIf { it > 0f }

    Canvas(
        modifier = modifier
            .pointerInput(points.size) {
                awaitEachGesture {
                    // Report on the touch down itself, before any movement. A drag detector only
                    // fires once the finger has travelled past the touch slop, so the marker
                    // appeared a few millimetres late and the chart read as unresponsive: you had
                    // to already know it was interactive to discover that it was.
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScrub(indexAt(down.position.x, size.width, points.size))

                    // The down is deliberately not consumed, so a vertical swipe still reaches the
                    // list underneath. Only once the gesture proves itself horizontal do we claim
                    // it; until then the scroll wins and the marker is dismissed.
                    val slopChange = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }

                    if (slopChange == null) {
                        onScrub(null)
                    } else {
                        horizontalDrag(slopChange.id) { change ->
                            onScrub(indexAt(change.position.x, size.width, points.size))
                            change.consume()
                        }
                        onScrub(null)
                    }
                }
            }
            .pointerInput(points.size) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> {
                                val position = event.changes.lastOrNull()?.position ?: continue
                                // Not consumed: a hover must not swallow events the list needs.
                                onScrub(indexAt(position.x, size.width, points.size))
                            }

                            PointerEventType.Exit -> onScrub(null)
                        }
                    }
                }
            },
    ) {
        val stepX = size.width / (points.size - 1)

        fun yOf(value: Float): Float {
            val normalised = range?.let { (value - minimum) / it } ?: MidPoint
            // Inset from both edges so the highest and lowest points are not clipped by the bounds.
            val usable = size.height * (1f - VerticalInset * 2)
            return size.height - (size.height * VerticalInset) - (normalised * usable * appear)
        }

        val line = Path().apply {
            moveTo(0f, yOf(points.first()))
            points.forEachIndexed { index, value ->
                if (index > 0) lineTo(index * stepX, yOf(value))
            }
        }

        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = FillAlpha), Color.Transparent),
                startY = 0f,
                endY = size.height,
            ),
        )

        drawPath(path = line, color = color, style = Stroke(width = StrokeWidth.toPx()))

        if (scrubIndex != null && scrubIndex in points.indices) {
            drawScrubber(
                x = scrubIndex * stepX,
                y = yOf(points[scrubIndex]),
                color = color,
            )
        } else {
            drawCircle(
                color = color,
                radius = EndDotRadius.toPx(),
                center = Offset(size.width, yOf(points.last())),
            )
        }
    }
}

private fun DrawScope.drawScrubber(x: Float, y: Float, color: Color) {
    drawLine(
        color = color.copy(alpha = GuideAlpha),
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = GuideWidth.toPx(),
    )
    // Ringed rather than plain, so the marker stays legible over the filled area beneath it.
    drawCircle(color = color.copy(alpha = HaloAlpha), radius = MarkerHaloRadius.toPx(), center = Offset(x, y))
    drawCircle(color = color, radius = MarkerRadius.toPx(), center = Offset(x, y))
}

/** Maps a horizontal position to the nearest sample, clamped so the ends stay reachable. */
private fun indexAt(x: Float, width: Int, count: Int): Int {
    if (width <= 0 || count <= 1) return 0
    val fraction = (x / width).coerceIn(0f, 1f)
    return (fraction * (count - 1)).roundToInt()
}

private const val MinimumPoints = 2
private const val AppearDurationMillis = 700
private const val MidPoint = 0.5f
private const val FillAlpha = 0.22f
private const val GuideAlpha = 0.35f
private const val HaloAlpha = 0.3f
private const val VerticalInset = 0.08f
private val StrokeWidth = 2.dp
private val EndDotRadius = 3.dp
private val GuideWidth = 1.dp
private val MarkerRadius = 4.dp
private val MarkerHaloRadius = 9.dp
