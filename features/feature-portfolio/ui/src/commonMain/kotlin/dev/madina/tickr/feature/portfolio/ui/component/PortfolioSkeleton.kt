package dev.madina.tickr.feature.portfolio.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.SurfaceElevated

/**
 * Placeholder row shown while the first prices are still arriving.
 *
 * It matches the real card's height so the list does not jump when content replaces it, which is
 * the only reason a skeleton is better than a spinner.
 */
@Composable
internal fun PortfolioSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = MinAlpha,
        targetValue = MaxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BreathDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RoundedCornerShape(Radius.Large))
            .background(SurfaceElevated.copy(alpha = alpha)),
    )
}

private const val MinAlpha = 0.35f
private const val MaxAlpha = 0.85f
private const val BreathDurationMillis = 900
private val RowHeight = 76.dp
