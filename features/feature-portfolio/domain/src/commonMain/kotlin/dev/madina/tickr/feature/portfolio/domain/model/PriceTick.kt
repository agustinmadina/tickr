package dev.madina.tickr.feature.portfolio.domain.model

/**
 * The latest known price for one asset, in USD.
 *
 * [changePercent24h] is null when the quote arrived without an opening price. Zero would be a claim
 * the feed never made, and it is the same distinction [price] draws on [ValuedHolding]: an unknown
 * move and a flat one are different things, and the screen says so.
 */
data class PriceTick(
    val symbol: String,
    val price: Double,
    val changePercent24h: Double?,
)
