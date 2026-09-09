package dev.madina.tickr.feature.portfolio.data.repository

import co.touchlab.kermit.Logger
import dev.madina.tickr.core.network.TickrJson
import dev.madina.tickr.feature.portfolio.data.remote.TickerJson
import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

/**
 * Live prices from Coinbase's public market data feed.
 *
 * No API key and no account: the feed is open, which is what lets the web build subscribe straight
 * from the browser. Web sockets are exempt from CORS preflight, so there is no proxy in between on
 * any of the three platforms.
 *
 * `channelFlow` rather than `flow`: the socket session runs in its own coroutine context, and
 * emitting across contexts from a plain flow builder is a runtime failure. `channelFlow` is built
 * for exactly this.
 */
internal class CoinbasePriceRepository(
    private val httpClient: HttpClient,
    private val logger: Logger = Logger.withTag("CoinbasePriceRepository"),
) : PriceRepository {
    /**
     * Accumulated rather than emitted per tick, so a subscriber receives every price known so far
     * instead of one asset at a time.
     *
     * It belongs to the repository and not to the flow because adding or removing a holding changes
     * the symbol set, and the caller's `flatMapLatest` tears the flow down when it does. A per-flow
     * accumulator started empty every time, so adding one asset dropped every other row back to
     * unpriced until it ticked again.
     */
    private val cache = MutableStateFlow<Map<String, PriceTick>>(emptyMap())

    override fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>> =
        channelFlow {
            // Emitted before the socket is even attempted. This flow is combined with the holdings
            // upstream, and combine produces nothing until both sides have emitted once, so waiting
            // for the first frame left the whole screen on its loading skeleton, for ever when the
            // feed was unreachable. An empty map is the unpriced state the model already represents.
            send(cache.value.filterKeys { it in symbols })
            if (symbols.isEmpty()) return@channelFlow

            val bySymbol = symbols.associateBy { productIdFor(it) }
            var failures = 0

            while (isActive) {
                try {
                    httpClient.webSocket(FeedUrl) {
                        send(Frame.Text(subscribeMessage(bySymbol.keys)))

                        for (frame in incoming) {
                            val text = (frame as? Frame.Text)?.readText() ?: continue
                            val ticker = parse(text) ?: continue
                            if (ticker.type != TickerType) continue

                            val symbol = bySymbol[ticker.productId] ?: continue
                            val tick = ticker.toPriceTick(symbol) ?: continue
                            // Reset here rather than on connect: a socket that is accepted and then
                            // dropped reset the count every attempt, which turned the capped backoff
                            // into a reconnect every second for as long as the app was open.
                            failures = 0
                            val updated = cache.updateAndGet { it + (symbol to tick) }
                            this@channelFlow.send(updated.filterKeys { it in symbols })
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    // A dropped feed is expected on mobile, so it is logged and retried rather than
                    // surfaced as an error: the UI keeps showing the last known prices meanwhile.
                    logger.w(failure) { "Price feed disconnected, retrying" }
                }

                if (!isActive) break
                delay(backoffMillis(failures))
                failures++
            }
        }

    private fun parse(text: String): TickerJson? =
        runCatching {
            TickrJson.decodeFromString<TickerJson>(text)
        }.getOrElse { failure ->
            logger.w(failure) { "Unparseable frame from the price feed" }
            null
        }

    private fun TickerJson.toPriceTick(symbol: String): PriceTick? {
        val current = price?.toDoubleOrNull() ?: return null
        val opening = open24h?.toDoubleOrNull()
        return PriceTick(
            symbol = symbol,
            price = current,
            // Null, not zero: without an opening price there is no basis for a daily change, and
            // zero is a claim the feed never made. It used to return zero anyway, directly under
            // this comment, which reported the position as precisely flat rather than unknown.
            changePercent24h =
                if (opening != null && opening > 0) {
                    (current - opening) / opening * Percent
                } else {
                    null
                },
        )
    }
}

private fun subscribeMessage(productIds: Set<String>): String {
    val ids = productIds.joinToString(",") { "\"$it\"" }
    return """{"type":"subscribe","product_ids":[$ids],"channels":["ticker"]}"""
}

/** The feed quotes against USD, and holdings are stored as the bare asset symbol. */
private fun productIdFor(symbol: String): String = "${symbol.uppercase()}-USD"

/** Exponential, capped: a feed that is down stays down, and hammering it helps nobody. */
private fun backoffMillis(failures: Int): Long =
    min(InitialBackoffMillis shl min(failures, MaxBackoffShift), MaxBackoffMillis)

private const val FeedUrl = "wss://ws-feed.exchange.coinbase.com"
private const val TickerType = "ticker"
private const val Percent = 100
private const val InitialBackoffMillis = 1_000L
private const val MaxBackoffMillis = 30_000L
private const val MaxBackoffShift = 5
