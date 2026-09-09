package dev.madina.tickr.feature.portfolio.domain.model

/** The latest known price for one asset, in USD. */
data class PriceTick(
    val symbol: String,
    val price: Double,
    val changePercent24h: Double,
)
