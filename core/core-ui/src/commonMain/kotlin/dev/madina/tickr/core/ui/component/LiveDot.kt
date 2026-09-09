package dev.madina.tickr.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import dev.madina.tickr.core.ui.theme.Sizing

/**
 * A dot with a halo that breathes, marking a value as live rather than merely last-known.
 *
 * The halo is drawn outside the dot's own radius, so the pulse does not change the space the
 * component occupies and neighbouring rows never shift while it animates.
 */
@Composable
fun LiveDot(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = Sizing.AssetDotSize,
) {
    val transition = rememberInfiniteTransition(label = "live-dot")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMillis),
            repeatMode = RepeatMode.Restart,
        ),
        label = "live-dot-pulse",
    )

    Canvas(modifier = modifier.size(diameter)) {
        val core = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = HaloAlpha * (1f - pulse)),
            radius = core * (1f + pulse * HaloGrowth),
        )
        drawCircle(color = color, radius = core)
    }
}

private const val PulseDurationMillis = 1800
private const val HaloAlpha = 0.45f
private const val HaloGrowth = 0.9f
