package dev.madina.tickr.feature.portfolio.ui.mapper

import dev.madina.tickr.feature.portfolio.domain.model.Portfolio
import dev.madina.tickr.feature.portfolio.ui.assetColor
import dev.madina.tickr.feature.portfolio.ui.model.HoldingUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps the domain portfolio onto what the screen draws.
 *
 * [day] is the last 24 hourly closes per symbol, fetched separately. It is passed in rather than
 * held in a field because this runs inside the ViewModel's state reducer, which must stay pure.
 */
internal fun Portfolio.toUi(day: Map<String, ImmutableList<Float>>): ImmutableList<HoldingUi> =
    holdings
        .map { valued ->
            val symbol = valued.holding.symbol
            HoldingUi(
                symbol = symbol,
                name = valued.holding.name,
                quantity = valued.holding.quantity,
                price = valued.price?.price,
                value = valued.value,
                profit = valued.profit,
                returnPercent = valued.returnPercent,
                changePercent24h = valued.price?.changePercent24h,
                accent = assetColor(symbol),
                history = withLatest(day[symbol] ?: persistentListOf(), valued.price?.price),
            )
        }.toImmutableList()

/**
 * Replaces the day's final, still-forming hour with the live price.
 *
 * Appending instead would stretch the window past a day on every tick, and the label beside the
 * chart says 24h. Returns the day untouched while there is no live price, and stays empty until the
 * day itself arrives: one point is not a chart, and seeding it with a single live price is how the
 * old version ended up drawing minutes of noise.
 */
internal fun withLatest(day: ImmutableList<Float>, live: Double?): ImmutableList<Float> {
    if (day.isEmpty() || live == null) return day
    val sample = live.toFloat()
    if (day.lastOrNull() == sample) return day
    return (day.dropLast(1) + sample).toImmutableList()
}
