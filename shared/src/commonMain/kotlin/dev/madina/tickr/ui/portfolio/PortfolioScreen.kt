package dev.madina.tickr.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madina.tickr.ui.format.formatPercent
import dev.madina.tickr.ui.format.formatQuantity
import dev.madina.tickr.ui.format.formatSigned
import dev.madina.tickr.ui.format.formatUsd
import dev.madina.tickr.ui.theme.Negative
import dev.madina.tickr.ui.theme.Positive
import dev.madina.tickr.ui.theme.SurfaceElevated
import dev.madina.tickr.ui.theme.TextSecondary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class HoldingRow(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val price: Double,
    val averageCost: Double,
    val dayChangePercent: Double,
    val accent: Color,
) {
    val value: Double get() = quantity * price
    val returnPercent: Double get() = (price - averageCost) / averageCost * 100
}

private val MockHoldings = persistentListOf(
    HoldingRow("BTC", "Bitcoin", 0.241, 78_601.02, 62_100.0, 1.24, Color(0xFFF7931A)),
    HoldingRow("ETH", "Ethereum", 1.80, 2_282.15, 2_010.0, -0.42, Color(0xFF627EEA)),
    HoldingRow("SOL", "Solana", 12.4, 102.40, 88.20, 3.10, Color(0xFF14F195)),
    // XRP's brand black is invisible on this background, so the dot uses a light neutral.
    HoldingRow("XRP", "XRP", 940.0, 2.08, 1.74, 0.86, Color(0xFFB4BCC8)),
)

@Composable
internal fun PortfolioScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 560.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PortfolioHeader(MockHoldings) }
                item { AllocationBar(MockHoldings) }
                item { Spacer(Modifier.height(4.dp)) }
                items(MockHoldings, key = { it.symbol }) { HoldingCard(it) }
            }
        }
    }
}

@Composable
private fun PortfolioHeader(holdings: ImmutableList<HoldingRow>) {
    val total = holdings.sumOf { it.value }
    val cost = holdings.sumOf { it.quantity * it.averageCost }
    val gain = total - cost
    val gainPercent = if (cost == 0.0) 0.0 else gain / cost * 100

    Column {
        Text(
            text = "YOUR PORTFOLIO",
            color = TextSecondary,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = total.formatUsd(),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${gain.formatSigned()} (${gainPercent.formatPercent()})",
            color = signColor(gain),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AllocationBar(holdings: ImmutableList<HoldingRow>) {
    val total = holdings.sumOf { it.value }
    if (total <= 0.0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        holdings.forEach { holding ->
            Box(
                modifier = Modifier
                    .weight((holding.value / total).toFloat())
                    .fillMaxHeight()
                    .background(holding.accent),
            )
        }
    }
}

@Composable
private fun HoldingCard(holding: HoldingRow) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(holding.accent),
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = holding.symbol,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${holding.quantity.formatQuantity()} ${holding.symbol}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = holding.value.formatUsd(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = holding.dayChangePercent.formatPercent(),
                    color = signColor(holding.dayChangePercent),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun signColor(value: Double): Color = if (value >= 0) Positive else Negative
