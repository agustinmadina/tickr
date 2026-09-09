package dev.madina.tickr.feature.portfolio.domain.model

/**
 * A holding priced at a point in time.
 *
 * [price] is null when no quote has arrived yet, which is the normal state for the first moments
 * after launch and whenever the feed drops. That case is represented rather than defaulted to zero,
 * because a position worth an unknown amount and a position worth nothing are different things and
 * the UI shows them differently.
 */
data class ValuedHolding(
    val holding: Holding,
    val price: PriceTick?,
) {
    val value: Double? = price?.let { holding.quantity * it.price }

    val cost: Double = holding.quantity * holding.averageCost

    val profit: Double? = value?.minus(cost)

    /** Profit as a percentage of what was paid. Null when unpriced, or when the cost basis is zero. */
    val returnPercent: Double? =
        if (cost == 0.0) null else profit?.div(cost)?.times(PERCENT)
}

data class Portfolio(
    val holdings: List<ValuedHolding>,
) {
    /** Sum of the holdings that have a price. Unpriced ones contribute nothing rather than blocking the total. */
    val totalValue: Double = holdings.sumOf { it.value ?: 0.0 }

    val totalCost: Double = holdings.sumOf { it.cost }

    val totalProfit: Double = totalValue - totalCost

    val totalReturnPercent: Double? =
        if (totalCost == 0.0) null else totalProfit / totalCost * PERCENT

    /** True while any holding is still waiting for its first quote, so the UI can say the total is partial. */
    val isPartiallyPriced: Boolean = holdings.any { it.price == null }

    val isEmpty: Boolean = holdings.isEmpty()
}

private const val PERCENT = 100
