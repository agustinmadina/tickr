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

    /**
     * What this position was worth 24 hours ago, derived from the price and its daily change.
     *
     * The feed sends today's move as a percentage rather than yesterday's price, so it is recovered
     * by dividing it out. A change of exactly -100% would mean the asset went to zero and leaves
     * nothing to divide by, so that case yields null rather than an infinity that would poison the
     * portfolio total.
     */
    val valueYesterday: Double? =
        price?.changePercent24h?.let { change ->
            val factor = 1 + (change / PERCENT)
            if (factor <= 0) null else holding.quantity * (price.price / factor)
        }

    val dayChange: Double? =
        if (value == null || valueYesterday == null) null else value - valueYesterday
}

data class Portfolio(
    val holdings: List<ValuedHolding>,
) {
    /** Sum of the holdings that have a price. Unpriced ones contribute nothing rather than blocking the total. */
    val totalValue: Double = holdings.sumOf { it.value ?: 0.0 }

    val totalCost: Double = holdings.sumOf { it.cost }

    /**
     * The cost of the holdings [totalValue] actually covers.
     *
     * Measuring a partial value against the full cost basis reported a loss the size of every
     * position still waiting for its first quote, so the headline read tens of percent down for the
     * first seconds after launch and then snapped to the real figure.
     */
    private val pricedCost: Double = holdings.sumOf { if (it.value == null) 0.0 else it.cost }

    val totalProfit: Double = totalValue - pricedCost

    val totalReturnPercent: Double? =
        if (pricedCost == 0.0) null else totalProfit / pricedCost * PERCENT

    /**
     * Yesterday's value, counting only the holdings that also have a value today.
     *
     * Summing `valueYesterday ?: 0.0` against the full [totalValue] treated "yesterday unknown" as
     * "yesterday worthless", so a holding whose opening price the feed never sent contributed its
     * entire market value to today's gain. That is the defect [ValuedHolding.valueYesterday] returns
     * null to avoid, undone one line later.
     */
    private val comparableValueYesterday: Double =
        holdings.sumOf { if (it.dayChange == null) 0.0 else it.valueYesterday ?: 0.0 }

    /**
     * How much the whole portfolio moved today.
     *
     * This is the number people look for first in a finance app, and the one that is comparable
     * with the per-row percentages. Return against cost answers a different question, over a
     * different span, and depends on what the user typed in rather than on the market.
     */
    val dayChange: Double = holdings.sumOf { it.dayChange ?: 0.0 }

    val dayChangePercent: Double? =
        if (comparableValueYesterday == 0.0) {
            null
        } else {
            dayChange / comparableValueYesterday * PERCENT
        }

    /** True while any holding is still waiting for its first quote, so the UI can say the total is partial. */
    val isPartiallyPriced: Boolean = holdings.any { it.price == null }

    val isEmpty: Boolean = holdings.isEmpty()
}

private const val PERCENT = 100
