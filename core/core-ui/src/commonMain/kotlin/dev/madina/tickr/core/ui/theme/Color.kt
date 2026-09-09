package dev.madina.tickr.core.ui.theme

import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0A0C10)
val BgElevated = Color(0xFF12161C)
val Surface = Color(0xFF171C24)
val SurfaceElevated = Color(0xFF1E242E)
val Outline = Color(0xFF2A313C)

val Accent = Color(0xFF5B8DEF)
val AccentSoft = Color(0xFF7BA4F5)

val Positive = Color(0xFF2ECC71)
val Negative = Color(0xFFF2555A)

val TextPrimary = Color(0xFFF3F6FA)
val TextSecondary = Color(0xFF8B95A7)
val TextTertiary = Color(0xFF5C6577)

/**
 * Brand colours per asset, used for the dot and the allocation bar.
 *
 * XRP's real brand colour is near-black and disappears on this background, so it gets a light
 * neutral instead: recognisability matters less than being able to see the thing.
 */
private val AssetColors = mapOf(
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
fun assetColor(symbol: String): Color =
    AssetColors[symbol.uppercase()]
        ?: FallbackAssetColors[symbol.hashCode().mod(FallbackAssetColors.size)]
