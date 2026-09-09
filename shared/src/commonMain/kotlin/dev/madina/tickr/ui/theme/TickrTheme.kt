package dev.madina.tickr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Bg = Color(0xFF0B0D10)
internal val Surface = Color(0xFF14181D)
internal val SurfaceElevated = Color(0xFF1C2229)
internal val Accent = Color(0xFF5B8DEF)
internal val Positive = Color(0xFF22C55E)
internal val Negative = Color(0xFFEF4444)
internal val TextPrimary = Color(0xFFF2F5F9)
internal val TextSecondary = Color(0xFF8A94A6)

private val TickrColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = Negative,
)

@Composable
fun TickrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TickrColors,
        content = content,
    )
}
