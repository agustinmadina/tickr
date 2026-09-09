package dev.madina.tickr.feature.portfolio.ui.model

import androidx.compose.ui.graphics.Color

/** An asset offered in the picker, with the colour it will carry once held. */
internal data class AssetUi(
    val symbol: String,
    val name: String,
    val accent: Color,
)
