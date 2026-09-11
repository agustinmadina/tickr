package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.SurfaceElevated
import dev.madina.tickr.core.ui.theme.TextSecondary
import dev.madina.tickr.feature.portfolio.domain.model.HistoryRange

/**
 * Four small buttons, not a dropdown.
 *
 * There are only four windows and they are the whole point of the chart above, so hiding them
 * behind a menu costs a tap to discover something the screen should be advertising. At this size
 * they also read as a caption rather than as a control competing with the figures.
 */
@Composable
internal fun RangePicker(
    selected: HistoryRange,
    onSelect: (HistoryRange) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        HistoryRange.entries.forEach { range ->
            val isSelected = range == selected
            Surface(
                onClick = { onSelect(range) },
                shape = RoundedCornerShape(Radius.Small),
                color = if (isSelected) SurfaceElevated else Color.Transparent,
            ) {
                Text(
                    text = range.shortLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else TextSecondary,
                    modifier =
                        Modifier.padding(
                            horizontal = Spacing.Small,
                            vertical = Spacing.ExtraSmall,
                        ),
                )
            }
        }
    }
}
