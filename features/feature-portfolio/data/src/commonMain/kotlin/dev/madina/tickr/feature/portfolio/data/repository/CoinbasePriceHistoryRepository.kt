package dev.madina.tickr.feature.portfolio.data.repository

import co.touchlab.kermit.Logger
import dev.madina.tickr.feature.portfolio.domain.model.PricePoint
import dev.madina.tickr.feature.portfolio.domain.repository.PriceHistoryRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A day of hourly closes per asset, from Coinbase's public candles endpoint.
 *
 * Keyless and CORS-open like the other two, which is what lets the web build read it straight from
 * the browser. Candles arrive newest first as positional arrays, `[time, low, high, open, close,
 * volume]`, so they are reversed and reduced to the close here rather than anywhere a reader would
 * have to remember that index 4 means something.
 *
 * Cached per symbol behind a `Mutex`: four holdings resolve on the same frame at launch, and the
 * cache is what stops that being four requests per holding.
 */
internal class CoinbasePriceHistoryRepository(
    private val httpClient: HttpClient,
    private val logger: Logger = Logger.withTag("CoinbasePriceHistoryRepository"),
) : PriceHistoryRepository {
    private val mutex = Mutex()
    private val cached = mutableMapOf<String, List<PricePoint>>()

    override suspend fun recentDay(symbol: String): List<PricePoint> =
        mutex.withLock {
            cached.getOrPut(symbol) { fetch(symbol) }
        }

    private suspend fun fetch(symbol: String): List<PricePoint> =
        runCatching {
            val candles: List<List<Double>> =
                httpClient
                    .get("$BaseUrl/products/${symbol.uppercase()}-USD/candles?granularity=$HourlyGranularity")
                    .body()

            candles
                .asSequence()
                .filter { it.size > CloseIndex }
                .map { PricePoint(epochSeconds = it[TimeIndex].toLong(), close = it[CloseIndex]) }
                .sortedBy { it.epochSeconds }
                .toList()
                .takeLast(HoursInDay)
        }.getOrElse { failure ->
            // An empty series draws no chart, which beats drawing a day the exchange never
            // reported. The price feed is unaffected, so the row keeps its live number.
            logger.e(failure) { "Could not load the last day for $symbol" }
            emptyList()
        }
}

private const val BaseUrl = "https://api.exchange.coinbase.com"
private const val HourlyGranularity = 3600
private const val HoursInDay = 24
private const val TimeIndex = 0
private const val CloseIndex = 4
