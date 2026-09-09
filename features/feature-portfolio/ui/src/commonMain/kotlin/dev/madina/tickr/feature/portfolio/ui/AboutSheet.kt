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
import org.jetbrains.compose.resources.stringResource
import tickr.features.feature_portfolio.ui.generated.resources.Res
import tickr.features.feature_portfolio.ui.generated.resources.about_demonstrates_title
import tickr.features.feature_portfolio.ui.generated.resources.about_link_email
import tickr.features.feature_portfolio.ui.generated.resources.about_link_linkedin
import tickr.features.feature_portfolio.ui.generated.resources.about_link_source
import tickr.features.feature_portfolio.ui.generated.resources.about_name
import tickr.features.feature_portfolio.ui.generated.resources.about_point_api
import tickr.features.feature_portfolio.ui.generated.resources.about_point_architecture
import tickr.features.feature_portfolio.ui.generated.resources.about_point_compose
import tickr.features.feature_portfolio.ui.generated.resources.about_point_persistence
import tickr.features.feature_portfolio.ui.generated.resources.about_point_tests
import tickr.features.feature_portfolio.ui.generated.resources.about_role
import tickr.features.feature_portfolio.ui.generated.resources.about_track_record
import tickr.features.feature_portfolio.ui.generated.resources.about_what_this_is

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
                text = stringResource(Res.string.about_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.about_role),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            // Ten years of work is not visible in a demo, and the reader who opens this sheet is
            // usually deciding whether the rest is worth their time. "Shipped for" rather than
            // "worked at": several of these were delivered through consultancies.
            Text(
                text = stringResource(Res.string.about_track_record),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(Spacing.ExtraSmall))

            Text(
                text = stringResource(Res.string.about_what_this_is),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(Spacing.ExtraSmall))

            Text(
                text = stringResource(Res.string.about_demonstrates_title),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Highlight(stringResource(Res.string.about_point_compose))
            Highlight(stringResource(Res.string.about_point_api))
            Highlight(stringResource(Res.string.about_point_architecture))
            Highlight(stringResource(Res.string.about_point_persistence))
            Highlight(stringResource(Res.string.about_point_tests))

            Spacer(Modifier.height(Spacing.Small))

            LinkRow(
                label = stringResource(Res.string.about_link_source),
                value = "github.com/agustinmadina/tickr",
                onClick = { uriHandler.openUri("https://github.com/agustinmadina/tickr") },
            )
            LinkRow(
                label = stringResource(Res.string.about_link_linkedin),
                value = "in/agustin-madina",
                onClick = { uriHandler.openUri("https://www.linkedin.com/in/agustin-madina/") },
            )
            LinkRow(
                label = stringResource(Res.string.about_link_email),
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
