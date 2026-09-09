package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.SurfaceElevated
import dev.madina.tickr.core.ui.theme.TextSecondary

/**
 * Who built this and what it is meant to show.
 *
 * A demo like this is read by two people who want different things: one wants to know whose work it
 * is, the other wants to know what it demonstrates. Both are one tap from the main screen rather
 * than buried in a repository somebody may never open.
 *
 * Links go through `LocalUriHandler`, which Compose Multiplatform implements on all three
 * platforms, so opening a URL needs no expect/actual of its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraLarge)
                    .padding(bottom = Spacing.Huge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                text = "Agustin Madina",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Android and Kotlin Multiplatform engineer",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(Spacing.ExtraSmall))

            Text(
                text =
                    "You are looking at one Kotlin codebase running on Android, iOS and the " +
                        "web. Not just the data layer: every screen, chart and animation here is a " +
                        "single implementation, and the platform-specific code is three launchers of " +
                        "about twenty lines each.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(Spacing.ExtraSmall))

            Text(
                text = "WHAT IT DEMONSTRATES",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Highlight("Compose Multiplatform, including WebAssembly")
            Highlight("Live prices over a WebSocket, plus REST for the asset catalogue")
            Highlight("Clean architecture the build system enforces, not the reviewer")
            Highlight("Persistence per platform behind one expect/actual")
            Highlight("Kotest specs and CI across all three targets")

            Spacer(Modifier.height(Spacing.Small))

            LinkRow(
                label = "Source code",
                value = "github.com/agustinmadina/tickr",
                onClick = { uriHandler.openUri("https://github.com/agustinmadina/tickr") },
            )
            LinkRow(
                label = "LinkedIn",
                value = "in/agustin-madina",
                onClick = { uriHandler.openUri("https://www.linkedin.com/in/agustin-madina/") },
            )
            LinkRow(
                label = "Email",
                value = "agustinmadina@gmail.com",
                onClick = { uriHandler.openUri("mailto:agustinmadina@gmail.com") },
            )
        }
    }
}

@Composable
private fun Highlight(text: String) {
    Row {
        Text(text = "-", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.width(Spacing.Small))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = SurfaceElevated,
        shape = RoundedCornerShape(Radius.Medium),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Medium)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(Spacing.ExtraSmall / 2))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
