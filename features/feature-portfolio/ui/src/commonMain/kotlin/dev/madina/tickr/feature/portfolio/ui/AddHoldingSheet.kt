package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.TextSecondary

/**
 * Text-field contents live in `rememberSaveable` rather than in the ViewModel: they are what the
 * user is typing, not application state, and hoisting every keystroke into the state flow would
 * recompose the whole screen on each character while the price feed is also ticking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddHoldingSheet(
    onDismiss: () -> Unit,
    onConfirm: (PortfolioAction.AddConfirmed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var symbol by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var averageCost by rememberSaveable { mutableStateOf("") }

    val canSubmit by remember(symbol, quantity, averageCost) {
        mutableStateOf(
            symbol.isNotBlank() &&
                quantity.toDoubleOrNull()?.let { it > 0 } == true &&
                averageCost.toDoubleOrNull() != null,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(bottom = Spacing.Huge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Text(
                text = "Add an asset",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Prices are simulated, so use whatever numbers you like.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(Modifier.height(Spacing.ExtraSmall))

            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase() },
                label = { Text("Symbol") },
                placeholder = { Text("BTC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (optional)") },
                placeholder = { Text("Bitcoin") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.ExtraSmall))
                OutlinedTextField(
                    value = averageCost,
                    onValueChange = { averageCost = it },
                    label = { Text("Average cost") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Spacing.Small))

            Button(
                onClick = {
                    onConfirm(
                        PortfolioAction.AddConfirmed(
                            symbol = symbol,
                            name = name,
                            quantity = quantity,
                            averageCost = averageCost,
                        ),
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Add to portfolio")
            }
        }
    }
}
