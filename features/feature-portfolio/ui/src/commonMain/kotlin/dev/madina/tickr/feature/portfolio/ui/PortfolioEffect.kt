package dev.madina.tickr.feature.portfolio.ui

internal sealed interface PortfolioEffect {
    data class ShowMessage(
        val message: UiMessage,
    ) : PortfolioEffect
}
