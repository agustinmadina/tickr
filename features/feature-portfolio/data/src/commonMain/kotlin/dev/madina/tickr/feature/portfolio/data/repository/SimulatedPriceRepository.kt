package dev.madina.tickr.feature.portfolio.data.repository

import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A price feed that moves on its own, without a network.
 *
 * It exists so the UI can be built and demonstrated against a live-looking stream before the
 * WebSocket client lands, and so the app still shows something when run with no connectivity. It
 * implements the same contract the real feed will, so swapping it is a one-line change in the DI
 * module.
 *
 * Movements are a random walk bounded to a small step, which looks like a market; large jumps would
 * make the change indicator flash constantly and read as broken rather than as live.
 */
internal class SimulatedPriceRepository(
    private val random: Random = Random.Default,
) : PriceRepository {

    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> = flow {
        val prices = symbols.associateWith { StartingPrices[it] ?: FallbackPrice }.toMutableMap()

        // The reference is a plausible price from 24h ago rather than today's opening value. When
        // the two were the same, every row opened at exactly +0.00%, which reads as a dead screen
        // in the first seconds — the worst possible moment for it.
        val reference = prices.mapValues { (_, price) ->
            price * (1 + (random.nextDouble() - HALF) * TWO * OpeningSpreadFraction)
        }

        var tick = 0
        while (true) {
            emit(
                prices.map { (symbol, price) ->
                    val opening = reference.getValue(symbol)
                    symbol to PriceTick(
                        symbol = symbol,
                        price = price,
                        changePercent24h = (price - opening) / opening * PERCENT,
                    )
                }.toMap(),
            )

            // The first ticks run fast so the sparklines have a shape almost immediately, then it
            // settles to a rate that looks like a market rather than a stress test.
            delay(if (tick < WarmUpTicks) WarmUpIntervalMillis else TickIntervalMillis)
            tick++

            if (tick < WarmUpTicks) {
                // Warming up, every symbol moves, so no row is left flat while the others fill in.
                prices.keys.forEach { symbol -> prices[symbol] = prices.getValue(symbol).nudge() }
            } else {
                // Afterwards one symbol moves per tick: moving all of them at once makes every row
                // animate in lockstep, which looks synthetic.
                val moving = prices.keys.randomOrNull(random) ?: return@flow
                prices[moving] = prices.getValue(moving).nudge()
            }
        }
    }

    private fun Double.nudge(): Double {
        val drift = (random.nextDouble() - HALF) * MaxStepFraction * TWO
        return abs(this * (1 + drift))
    }
}

private const val PERCENT = 100
private const val HALF = 0.5
private const val TWO = 2
private const val MaxStepFraction = 0.004
private const val OpeningSpreadFraction = 0.03
private const val TickIntervalMillis = 900L
private const val WarmUpIntervalMillis = 110L
private const val WarmUpTicks = 14
private const val FallbackPrice = 1.0

private val StartingPrices = mapOf(
    "BTC" to 78_601.02,
    "ETH" to 2_282.15,
    "SOL" to 102.40,
    "XRP" to 2.08,
)
