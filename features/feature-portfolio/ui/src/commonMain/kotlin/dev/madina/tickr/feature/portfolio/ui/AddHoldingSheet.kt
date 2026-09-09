package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.madina.tickr.core.ui.theme.Negative
import dev.madina.tickr.core.ui.theme.Radius
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.SurfaceElevated
import dev.madina.tickr.core.ui.theme.TextSecondary
import dev.madina.tickr.feature.portfolio.ui.model.AssetUi
import org.jetbrains.compose.resources.stringResource
import tickr.features.feature_portfolio.ui.generated.resources.Res
import tickr.features.feature_portfolio.ui.generated.resources.add_change_asset
import tickr.features.feature_portfolio.ui.generated.resources.add_confirm
import tickr.features.feature_portfolio.ui.generated.resources.add_cost_explainer
import tickr.features.feature_portfolio.ui.generated.resources.add_cost_label
import tickr.features.feature_portfolio.ui.generated.resources.add_cost_placeholder
import tickr.features.feature_portfolio.ui.generated.resources.add_no_matches
import tickr.features.feature_portfolio.ui.generated.resources.add_quantity_label
import tickr.features.feature_portfolio.ui.generated.resources.add_search_label
import tickr.features.feature_portfolio.ui.generated.resources.add_search_placeholder
import tickr.features.feature_portfolio.ui.generated.resources.add_subtitle
import tickr.features.feature_portfolio.ui.generated.resources.add_title

/**
 * Adds a position, and only one the exchange actually quotes.
 *
 * The asset comes from a searchable list of what the feed carries, not from a free text field.
 * Typing a symbol by hand let you add something that would never receive a price and would sit in
 * the portfolio blank for ever, with nothing on screen explaining why.
 *
 * The amounts stay in `rememberSaveable` rather than in the ViewModel: they are what the user is
 * typing, not application state, and hoisting each keystroke would recompose the screen while the
 * price feed is also ticking. The search query is in the ViewModel precisely because it does drive
 * something else, the results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddHoldingSheet(
    state: PortfolioUiState,
    onAction: (PortfolioAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by rememberSaveable { mutableStateOf("") }
    var averageCost by rememberSaveable { mutableStateOf("") }

    val selected = state.selectedAsset
    // isFinite, not just parseable: Kotlin reads "NaN" and "Infinity" as valid doubles, and a long
    // enough digit string overflows to Infinity. KeyboardType.Decimal keeps them off an Android soft
    // keyboard and constrains nothing at all in a browser, which is the target this project exists
    // for. NaN then formats as $0.00 and Infinity as a nineteen-digit total.
    val canSubmit =
        selected != null &&
            quantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true &&
            averageCost.toDoubleOrNull()?.isFinite() == true

    ModalBottomSheet(
        onDismissRequest = { onAction(PortfolioAction.AddDismissed) },
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
                text = stringResource(Res.string.add_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (selected == null) {
                Text(
                    text = stringResource(Res.string.add_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                OutlinedTextField(
                    value = state.assetQuery,
                    onValueChange = { onAction(PortfolioAction.AssetQueryChanged(it)) },
                    label = { Text(stringResource(Res.string.add_search_label)) },
                    placeholder = { Text(stringResource(Res.string.add_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                when {
                    state.catalogError != null ->
                        Text(
                            text = stringResource(state.catalogError.resource),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Negative,
                        )

                    state.isCatalogLoading && state.assetResults.isEmpty() ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.Large),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }

                    state.assetResults.isEmpty() ->
                        Text(
                            text = stringResource(Res.string.add_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )

                    else ->
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = ResultsMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                        ) {
                            items(state.assetResults, key = { it.symbol }) { asset ->
                                AssetRow(
                                    asset = asset,
                                    onClick = { onAction(PortfolioAction.AssetSelected(asset.symbol)) },
                                )
                            }
                        }
                }
            } else {
                SelectedAsset(
                    asset = selected,
                    onChange = { onAction(PortfolioAction.AssetSelectionCleared) },
                )

                // The obvious question when every other number is fetched live is why this one is
                // typed. Answering it here is cheaper than leaving the user to wonder.
                Text(
                    text = stringResource(Res.string.add_cost_explainer),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(stringResource(Res.string.add_quantity_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    OutlinedTextField(
                        value = averageCost,
                        onValueChange = { averageCost = it },
                        label = { Text(stringResource(Res.string.add_cost_label)) },
                        placeholder = { Text(stringResource(Res.string.add_cost_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(Spacing.Small))

                Button(
                    onClick = {
                        onAction(
                            PortfolioAction.AddConfirmed(
                                quantity = quantity,
                                averageCost = averageCost,
                            ),
                        )
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.add_confirm, selected.symbol))
                }
            }
        }
    }
}

@Composable
private fun AssetRow(asset: AssetUi, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = SurfaceElevated,
        shape = RoundedCornerShape(Radius.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(Spacing.Medium).clip(CircleShape).background(asset.accent))
            Spacer(Modifier.width(Spacing.Medium))
            Text(
                text = asset.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.Small))
            Text(
                text = asset.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun SelectedAsset(asset: AssetUi, onChange: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(Spacing.Medium).clip(CircleShape).background(asset.accent))
        Spacer(Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.symbol,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = asset.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        TextButton(onClick = onChange) {
            Text(
                text = stringResource(Res.string.add_change_asset),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private val ResultsMaxHeight = 280.dp
