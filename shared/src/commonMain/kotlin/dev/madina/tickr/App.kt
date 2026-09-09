package dev.madina.tickr

import androidx.compose.runtime.Composable
import dev.madina.tickr.ui.portfolio.PortfolioScreen
import dev.madina.tickr.ui.theme.TickrTheme

@Composable
fun App() {
    TickrTheme {
        PortfolioScreen()
    }
}
