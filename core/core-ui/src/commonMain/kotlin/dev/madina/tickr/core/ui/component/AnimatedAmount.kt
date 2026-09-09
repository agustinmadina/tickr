package dev.madina.tickr.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Positive
import kotlinx.coroutines.delay

/** Mutable holder that is deliberately not snapshot state: written during composition to carry the
 *  last rendered figure across recompositions without invalidating anything. */
private class AmountHolder(
    var lastRendered: Double,
)

/**
 * A number that counts to its new value instead of jumping, and briefly takes the colour of the
 * direction it moved.
 *
 * The count animates a 0..1 progress and interpolates between the previous and the new value as
 * `Double`, rather than animating the value itself. Compose's numeric animations run on `Float`,
 * whose ~7 significant digits are not enough for a currency amount: a total in the tens of
 * thousands with cents would visibly wobble in its last digits on every tick.
 *
 * Interpolation starts from what is currently on screen, not from the previous target, so a tick
 * arriving mid-animation continues smoothly instead of snapping backwards.
 */
@Composable
fun AnimatedAmount(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    baseColor: Color = MaterialTheme.colorScheme.onBackground,
    flashOnChange: Boolean = true,
) {
    val holder = remember { AmountHolder(value) }
    var from by remember { mutableStateOf(value) }
    var to by remember { mutableStateOf(value) }
    var direction by remember { mutableStateOf(0) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(value) {
        if (value == to) return@LaunchedEffect
        from = holder.lastRendered
        to = value
        direction = if (to > from) 1 else -1
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = CountDurationMillis))
    }

    // The tint is released on its own short timer rather than when the count finishes. Tying the
    // two together left the figure tinted for as long as it took to count, so with a fast feed it
    // was permanently red or green and contradicted the percentage next to it.
    LaunchedEffect(direction) {
        if (direction == 0) return@LaunchedEffect
        delay(FlashHoldMillis)
        direction = 0
    }

    val displayed = from + (to - from) * progress.value
    holder.lastRendered = displayed

    val color by animateColorAsState(
        targetValue =
            when {
                !flashOnChange || direction == 0 -> baseColor
                direction > 0 -> Positive
                else -> Negative
            },
        animationSpec = tween(durationMillis = FlashDurationMillis),
    )

    Text(
        text = format(displayed),
        style = style,
        color = color,
        modifier = modifier,
    )
}

private const val CountDurationMillis = 550
private const val FlashDurationMillis = 200
private const val FlashHoldMillis = 260L
