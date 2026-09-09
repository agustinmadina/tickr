package dev.madina.tickr.feature.portfolio.domain.model

/** An asset the exchange actually quotes, and therefore one the app can price. */
data class Asset(
    val symbol: String,
    val name: String,
)
