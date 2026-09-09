package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.ui.graphics.Color
import dev.madina.tickr.core.ui.theme.Accent
import dev.madina.tickr.core.ui.theme.Positive

// A table of crypto tickers is a business concept, so it does not belong in core/, which has to
// stay shippable in an unrelated app. It lives with the only feature that knows what a BTC is.

/**
 * Brand colours per asset, used for the dot and the allocation bar.
 *
 * XRP's real brand colour is near-black and disappears on this background, so it gets a light
 * neutral instead: recognisability matters less than being able to see the thing.
 */
private val AssetColors =
    mapOf(
        "BTC" to Color(0xFFF7931A),
        "ETH" to Color(0xFF7B9CF5),
        "SOL" to Color(0xFF14F195),
        "XRP" to Color(0xFFB4BCC8),
        "ADA" to Color(0xFF3468D1),
        "DOGE" to Color(0xFFC3A634),
        "AVAX" to Color(0xFFE84142),
        "DOT" to Color(0xFFE6007A),
        "LINK" to Color(0xFF2A5ADA),
        "MATIC" to Color(0xFF8247E5),
    )

private val FallbackAssetColors = listOf(Accent, Positive, Color(0xFFB07BF5), Color(0xFFF5A97B))

/** Falls back to a stable colour derived from the symbol, so an unknown asset is still consistent. */
internal fun assetColor(symbol: String): Color =
    AssetColors[symbol.uppercase()]
        ?: FallbackAssetColors[symbol.hashCode().mod(FallbackAssetColors.size)]
