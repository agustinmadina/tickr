package dev.madina.tickr.feature.portfolio.domain.model

/**
 * One asset's closing price at one point in time.
 *
 * The timestamp is carried rather than dropped because the portfolio total has to add assets up at
 * the same instant. Two series of bare numbers cannot be summed: they would only line up by luck.
 */
data class PricePoint(
    val epochSeconds: Long,
    val close: Double,
)
