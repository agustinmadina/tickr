package dev.madina.tickr.feature.portfolio.ui

import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Which screen is showing is part of the state, not of the composition. Keeping it here means the
 * back stack survives a configuration change, and the same state drives all three platforms.
 */
sealed interface PortfolioDestination {
    data object Overview : PortfolioDestination

    data class Detail(val symbol: String) : PortfolioDestination
}

data class PortfolioUiState(
    val holdings: ImmutableList<HoldingUi> = persistentListOf(),
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalReturnPercent: Double? = null,
    val isLoading: Boolean = true,
    val isPartiallyPriced: Boolean = false,
    val destination: PortfolioDestination = PortfolioDestination.Overview,
    val isAddSheetVisible: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean = !isLoading && holdings.isEmpty()

    val selectedHolding: HoldingUi?
        get() = (destination as? PortfolioDestination.Detail)
            ?.let { detail -> holdings.firstOrNull { it.symbol == detail.symbol } }
}
