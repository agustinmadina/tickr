package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.madina.tickr.core.ui.component.AnimatedAmount
import dev.madina.tickr.core.ui.component.BackChevron
import dev.madina.tickr.core.ui.component.InteractiveLineChart
import dev.madina.tickr.core.ui.component.LiveDot
import dev.madina.tickr.core.ui.format.formatPercent
import dev.madina.tickr.core.ui.format.formatPrice
import dev.madina.tickr.core.ui.format.formatQuantity
import dev.madina.tickr.core.ui.format.formatSignedUsd
import dev.madina.tickr.core.ui.format.formatUsd
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Positive
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Sizing
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.SurfaceElevated
import dev.madina.tickr.core.ui.theme.TextSecondary
import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi

@Composable
internal fun HoldingDetailScreen(
    holding: HoldingUi?,
    scrubIndex: Int?,
    onAction: (PortfolioAction) -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .widthIn(max = Sizing.ContentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.Large)
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
        ) {
            if (showBack) {
                TextButton(onClick = { onAction(PortfolioAction.BackClicked) }) {
                    BackChevron(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.Small))
                    Text(text = "Back", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Spacer(Modifier.height(Spacing.Large))
            }

            // The holding can vanish while its detail is open, if it is removed from another
            // surface. Rendering a message beats rendering a screen full of dashes.
            if (holding == null) {
                Text(
                    text = "This asset is no longer in your portfolio.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = Spacing.ExtraLarge),
                )
                return@Column
            }

            val scrubbedPrice = scrubIndex?.let { holding.history.getOrNull(it)?.toDouble() }

            Spacer(Modifier.height(Spacing.Large))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(color = holding.accent, diameter = Spacing.Medium)
                Spacer(Modifier.width(Spacing.Small))
                Text(
                    text = holding.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(Modifier.height(Spacing.Small))

            if (scrubbedPrice != null) {
                // Direct, not animated: while the pointer moves, a count animation per sample would
                // trail behind the finger instead of tracking it.
                Text(
                    text = scrubbedPrice.formatPrice(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                // "at this point" on its own said nothing: consecutive samples differ by cents, so
                // the headline looked static even though it was tracking the finger. Reporting the
                // move against the start of the series, in money and percent, is what makes a point
                // worth reading, and it matches what the overview shows while scrubbing.
                val start = holding.history.firstOrNull()?.toDouble()
                val changeSinceStart = start?.let { scrubbedPrice - it }
                val percentSinceStart =
                    start
                        ?.takeIf { it != 0.0 }
                        ?.let { changeSinceStart?.div(it)?.times(Percent) }

                if (changeSinceStart == null) {
                    Text(
                        text = "at this point",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                    )
                } else {
                    val percentSuffix = percentSinceStart?.let { " (${it.formatPercent()})" }.orEmpty()
                    Text(
                        text = "${changeSinceStart.formatSignedUsd()}$percentSuffix since this chart started",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (changeSinceStart >= 0) Positive else Negative,
                    )
                }
            } else {
                holding.price?.let { price ->
                    AnimatedAmount(
                        value = price,
                        format = { it.formatPrice() },
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
                holding.changePercent24h?.let { change ->
                    Text(
                        text = "${change.formatPercent()} today",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (change >= 0) Positive else Negative,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.ExtraLarge))

            InteractiveLineChart(
                points = holding.history,
                color = if ((holding.changePercent24h ?: 0.0) >= 0) Positive else Negative,
                scrubIndex = scrubIndex,
                onScrub = { index -> onAction(PortfolioAction.Scrubbed(PortfolioAction.Chart.Detail, index)) },
                modifier = Modifier.fillMaxWidth().height(Sizing.DetailChartHeight),
            )

            Spacer(Modifier.height(Spacing.ExtraLarge))

            Surface(color = SurfaceElevated, shape = RoundedCornerShape(Radius.Large)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    DetailRow("Holdings", "${holding.quantity.formatQuantity()} ${holding.symbol}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    DetailRow("Market value", holding.value?.formatUsd() ?: Pending)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    DetailRow(
                        label = "Profit",
                        value = holding.profit?.formatSignedUsd() ?: Pending,
                        valueColor = holding.profit?.let { if (it >= 0) Positive else Negative },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    DetailRow(
                        label = "Return since you bought",
                        value = holding.returnPercent?.formatPercent() ?: Pending,
                        valueColor = holding.returnPercent?.let { if (it >= 0) Positive else Negative },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.ExtraLarge))

            OutlinedButton(
                onClick = { onAction(PortfolioAction.RemoveClicked(holding.symbol)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Remove from portfolio", color = Negative)
            }

            Spacer(Modifier.height(Spacing.Huge))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** ASCII on purpose: an em dash is another glyph the web build's bundled font may not carry. */
private const val Pending = "--"
private const val Percent = 100
