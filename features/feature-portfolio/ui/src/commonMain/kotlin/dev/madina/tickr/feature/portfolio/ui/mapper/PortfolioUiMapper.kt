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
 * [previousHistory] carries the price samples already collected, keyed by symbol, so the sparkline
 * grows as quotes arrive. It is passed in rather than held in a field because this runs inside the
 * ViewModel's state reducer, which must stay pure.
 */
internal fun Portfolio.toUi(previousHistory: Map<String, ImmutableList<Float>>): ImmutableList<HoldingUi> =
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
                history =
                    appendSample(
                        history = previousHistory[symbol] ?: persistentListOf(),
                        price = valued.price?.price,
                    ),
            )
        }.toImmutableList()

/**
 * Appends a sample, ignoring a repeat of the last value.
 *
 * That check is what makes this safe inside a state reducer: `MutableStateFlow.update` can re-run
 * the reducer under contention, and without it the same tick would be recorded twice.
 */
private fun appendSample(history: ImmutableList<Float>, price: Double?): ImmutableList<Float> {
    if (price == null) return history
    val sample = price.toFloat()
    if (history.lastOrNull() == sample) return history
    return (history + sample).takeLast(MaxSamples).toImmutableList()
}

/** Enough to show a shape at the widths the sparkline is drawn at; more would just be memory. */
private const val MaxSamples = 40
