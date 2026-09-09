package dev.madina.tickr.feature.portfolio.data.repository

import dev.madina.tickr.feature.portfolio.data.remote.CurrencyJson
import dev.madina.tickr.feature.portfolio.data.remote.ProductJson
import dev.madina.tickr.feature.portfolio.domain.model.Asset
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The tradable universe, read once from Coinbase's public REST endpoints and kept in memory.
 *
 * Two calls rather than one: `/products` says what is tradable and `/currencies` says what each
 * symbol is called. Neither alone is enough for a usable picker.
 *
 * A `Mutex` guards the cache, not `synchronized`, which is JVM only and would not compile for iOS
 * or wasm. Holding it across the fetch means several callers arriving at once produce one request
 * rather than one each.
 */
internal class CoinbaseAssetCatalogRepository(
    private val httpClient: HttpClient,
) : AssetCatalogRepository {
    private val mutex = Mutex()
    private var cached: List<Asset>? = null

    override suspend fun tradableAssets(): List<Asset> =
        mutex.withLock {
            cached ?: fetchCatalog().also { cached = it }
        }

    private suspend fun fetchCatalog(): List<Asset> {
        val products: List<ProductJson> = httpClient.get("$BaseUrl/products").body()
        val currencies: List<CurrencyJson> = httpClient.get("$BaseUrl/currencies").body()

        val nameBySymbol =
            currencies
                .mapNotNull { currency ->
                    val id = currency.id ?: return@mapNotNull null
                    id to (currency.name ?: id)
                }.toMap()

        return products
            .asSequence()
            .filter { it.quoteCurrency == UsdQuote }
            .filter { it.status == OnlineStatus }
            .filter { it.tradingDisabled != true }
            .mapNotNull { it.baseCurrency }
            .distinct()
            .map { symbol -> Asset(symbol = symbol, name = nameBySymbol[symbol] ?: symbol) }
            .sortedBy { it.symbol }
            .toList()
    }
}

private const val BaseUrl = "https://api.exchange.coinbase.com"
private const val UsdQuote = "USD"
private const val OnlineStatus = "online"
