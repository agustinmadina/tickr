package dev.madina.tickr.feature.portfolio.ui.model

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList

/**
 * A holding as the screen needs it.
 *
 * Amounts stay as `Double` rather than pre-formatted strings because the screen animates between
 * them; a string cannot be interpolated. Formatting happens at the point of display.
 */
data class HoldingUi(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val price: Double?,
    val value: Double?,
    val profit: Double?,
    val returnPercent: Double?,
    val changePercent24h: Double?,
    val accent: Color,
    val history: ImmutableList<Float>,
) {
    val isPriced: Boolean = price != null

    /**
     * The move across the samples the chart actually draws.
     *
     * A different span from [changePercent24h], which is the point: colouring the line by the daily
     * change painted a session that had drifted down green whenever the day was up, so the chart
     * contradicted its own shape. The overview chart already colours itself this way.
     */
    val sessionChange: Float = if (history.size < 2) 0f else history.last() - history.first()
}
