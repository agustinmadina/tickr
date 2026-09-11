package dev.madina.tickr.feature.portfolio.domain.model

/**
 * The last day, per asset and for the portfolio as a whole.
 *
 * [total] is empty unless every holding has history, for the same reason
 * [Portfolio.totalHistory]-style sums are: a total that silently leaves out a position is not a
 * smaller total, it is a different portfolio, and a reader has no way to tell which one they are
 * looking at.
 */
data class PortfolioHistory(
    val perSymbol: Map<String, List<Double>> = emptyMap(),
    val total: List<Double> = emptyList(),
)
