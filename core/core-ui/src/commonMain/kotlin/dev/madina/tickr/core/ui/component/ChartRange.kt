package dev.madina.tickr.core.ui.component

import kotlin.math.abs
import kotlin.math.max

/**
 * Maps a series onto the 0..1 height of a chart, with a floor on how small a move may look big.
 *
 * Plain min/max normalisation stretches whatever it is handed to the full height, so a price that
 * wobbled 0.05% over a few minutes drew exactly the same mountain as one that moved 5%, and the
 * chart said nothing at all about scale. A reader cannot tell drama from rounding, which in a
 * finance app is worse than showing no chart.
 *
 * So the span is at least [MinimumSpanFraction] of the values themselves, and the series is centred
 * rather than anchored to its minimum: below that floor the line sits flat in the middle, which is
 * what a still market should look like.
 */
internal class ChartRange(
    points: List<Float>,
) {
    private val midpoint = (points.min() + points.max()) / 2f

    private val span =
        max(points.max() - points.min(), abs(midpoint) * MinimumSpanFraction)
            .takeIf { it > 0f }

    /** 0 is the bottom of the chart, 1 the top. */
    fun fractionOf(value: Float): Float =
        span?.let { (Centre + (value - midpoint) / it).coerceIn(0f, 1f) } ?: Centre
}

private const val Centre = 0.5f

/**
 * Half a percent. A typical 24h crypto move is a couple of percent and still fills the chart; a
 * few minutes of ticking is a fraction of a percent and now reads as the flat line it is.
 */
private const val MinimumSpanFraction = 0.005f
