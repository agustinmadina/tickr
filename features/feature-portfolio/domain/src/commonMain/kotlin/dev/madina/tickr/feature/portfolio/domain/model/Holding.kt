package dev.madina.tickr.feature.portfolio.domain.model

/**
 * A position the user holds, as they entered it. Carries no market data: what it is worth depends
 * on a price that changes by the second, and is computed in [Portfolio].
 *
 * @param averageCost what one unit cost on average, in USD. Used to work out profit.
 */
data class Holding(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCost: Double,
)
