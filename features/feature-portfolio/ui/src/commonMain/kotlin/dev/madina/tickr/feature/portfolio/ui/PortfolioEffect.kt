package dev.madina.tickr.feature.portfolio.ui

sealed interface PortfolioEffect {
    data class ShowMessage(val message: String) : PortfolioEffect
}
