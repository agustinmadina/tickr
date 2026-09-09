package dev.madina.tickr.feature.portfolio.ui

sealed interface PortfolioAction {
    data class HoldingClicked(val symbol: String) : PortfolioAction

    data object BackClicked : PortfolioAction

    data object AddClicked : PortfolioAction

    data object AddDismissed : PortfolioAction

    data class AddConfirmed(
        val symbol: String,
        val name: String,
        val quantity: String,
        val averageCost: String,
    ) : PortfolioAction

    data class RemoveClicked(val symbol: String) : PortfolioAction

    data object ErrorDismissed : PortfolioAction
}
