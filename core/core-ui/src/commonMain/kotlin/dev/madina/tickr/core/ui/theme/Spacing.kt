package dev.madina.tickr.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale. Named for role rather than value, so changing what "medium" means is one edit
 * rather than a search for every `16.dp`.
 */
object Spacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Huge = 32.dp
}

object Radius {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 20.dp
}

object Sizing {
    /** Width past which the layout stops stretching, so the web build does not span a desktop monitor. */
    val ContentMaxWidth = 560.dp
    val AllocationBarHeight = 10.dp
    val AssetDotSize = 10.dp
    val AssetBadgeSize = 40.dp
    val HeaderChartHeight = 120.dp
    val SparklineHeight = 64.dp
    val DetailChartHeight = 180.dp
}
