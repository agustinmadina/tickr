package dev.madina.tickr.feature.portfolio.data.repository

import dev.madina.tickr.core.network.TickrJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

/**
 * The join between Coinbase's two catalogue endpoints, which is what the asset picker is built
 * from. State is created inside each `Then`: Kotest's isolation modes are silently ignored on
 * Kotlin/Native, so a shared engine would carry its request log between blocks and fail on iOS only.
 */
internal class CoinbaseAssetCatalogRepositorySpec :
    BehaviorSpec({

        Given("the two catalogue endpoints") {

            When("both respond") {
                Then("each tradable symbol is paired with its human readable name") {
                    runTest {
                        val repository = CoinbaseAssetCatalogRepository(catalogClient())

                        val assets = repository.tradableAssets()

                        assets.map { it.symbol } shouldContainExactly listOf("BTC", "ETH", "SOL")
                        assets.first { it.symbol == "BTC" }.name shouldBe "Bitcoin"
                    }
                }

                Then("results are sorted by symbol, so the picker has a stable order") {
                    runTest {
                        val repository = CoinbaseAssetCatalogRepository(catalogClient())

                        val symbols = repository.tradableAssets().map { it.symbol }

                        symbols shouldContainExactly symbols.sorted()
                    }
                }

                Then("a symbol with no entry in currencies falls back to the symbol itself") {
                    runTest {
                        // SOL is quoted by /products and absent from /currencies in the fixture.
                        // Dropping it would hide a tradable asset; showing a blank name is worse.
                        val repository = CoinbaseAssetCatalogRepository(catalogClient())

                        repository.tradableAssets().first { it.symbol == "SOL" }.name shouldBe "SOL"
                    }
                }
            }

            When("a pair is not one the user can actually buy") {
                Then("it is left out of the catalogue") {
                    runTest {
                        val repository = CoinbaseAssetCatalogRepository(catalogClient())

                        val symbols = repository.tradableAssets().map { it.symbol }

                        // Quoted in EUR rather than USD, so the app has no price for it.
                        symbols shouldNotContain "ADA"
                        // Delisted, and trading halted: both would sit in the list never ticking.
                        symbols shouldNotContain "OLD"
                        symbols shouldNotContain "HALTED"
                    }
                }
            }

            When("the same asset is quoted against USD more than once") {
                Then("it appears once") {
                    runTest {
                        val repository = CoinbaseAssetCatalogRepository(catalogClient())

                        repository.tradableAssets().count { it.symbol == "BTC" } shouldBe 1
                    }
                }
            }

            When("several callers ask at the same time") {
                Then("the endpoints are called once, not once per caller") {
                    runTest {
                        // The mutex is held across the fetch precisely so a burst of subscribers on
                        // launch does not become a burst of requests.
                        val engine = catalogEngine()
                        val repository = CoinbaseAssetCatalogRepository(clientFor(engine))

                        val results =
                            listOf(
                                async { repository.tradableAssets() },
                                async { repository.tradableAssets() },
                                async { repository.tradableAssets() },
                            ).awaitAll()

                        results.forEach {
                            it.map { asset -> asset.symbol } shouldContainExactly
                                listOf("BTC", "ETH", "SOL")
                        }
                        engine.requestHistory.size shouldBe 2
                    }
                }
            }

            When("the catalogue has already been read") {
                Then("a second read is served from memory") {
                    runTest {
                        val engine = catalogEngine()
                        val repository = CoinbaseAssetCatalogRepository(clientFor(engine))

                        repository.tradableAssets()
                        repository.tradableAssets()

                        engine.requestHistory.size shouldBe 2
                    }
                }
            }
        }
    })

private const val ProductsJson = """
[
  {"id":"BTC-USD","base_currency":"BTC","quote_currency":"USD","status":"online"},
  {"id":"BTC-USDC","base_currency":"BTC","quote_currency":"USD","status":"online"},
  {"id":"ETH-USD","base_currency":"ETH","quote_currency":"USD","status":"online"},
  {"id":"SOL-USD","base_currency":"SOL","quote_currency":"USD","status":"online"},
  {"id":"ADA-EUR","base_currency":"ADA","quote_currency":"EUR","status":"online"},
  {"id":"OLD-USD","base_currency":"OLD","quote_currency":"USD","status":"delisted"},
  {"id":"HALTED-USD","base_currency":"HALTED","quote_currency":"USD","status":"online","trading_disabled":true}
]
"""

private const val CurrenciesJson = """
[
  {"id":"BTC","name":"Bitcoin"},
  {"id":"ETH","name":"Ethereum"},
  {"id":"ADA","name":"Cardano"}
]
"""

private fun catalogEngine() =
    MockEngine { request ->
        val body = if (request.url.encodedPath.endsWith("/currencies")) CurrenciesJson else ProductsJson
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

private fun clientFor(engine: MockEngine) =
    HttpClient(engine) {
        install(ContentNegotiation) { json(TickrJson) }
    }

private fun catalogClient() = clientFor(catalogEngine())
