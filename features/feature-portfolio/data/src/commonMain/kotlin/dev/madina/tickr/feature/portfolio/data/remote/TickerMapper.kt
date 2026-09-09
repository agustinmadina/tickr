package dev.madina.tickr.feature.portfolio.data.remote

import dev.madina.tickr.feature.portfolio.domain.model.PriceTick

/**
 * Turns one ticker frame into a domain price, or null if it does not carry one.
 *
 * A separate file from the repository because this is the arithmetic that decides what a position
 * is worth on screen, and the repository around it is a WebSocket loop that a test cannot easily
 * drive. Everything worth being sure about lives here.
 */
internal fun TickerJson.toPriceTick(symbol: String): PriceTick? {
    val current = price?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    val opening = open24h?.toDoubleOrNull()?.takeIf { it.isFinite() }
    return PriceTick(
        symbol = symbol,
        price = current,
        // Null, not zero: without an opening price there is no basis for a daily change, and zero
        // is a claim the feed never made. A non-positive opening price has the same problem and
        // would divide by zero besides.
        changePercent24h =
            if (opening != null && opening > 0) {
                (current - opening) / opening * Percent
            } else {
                null
            },
    )
}

private const val Percent = 100
