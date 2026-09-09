package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.feature.portfolio.domain.model.Asset
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

internal class SearchAssetsUseCaseSpec :
    BehaviorSpec({

        Given("a catalogue containing an exact symbol and things merely resembling it") {

            When("the query is that exact symbol") {
                Then("the exact match comes first, ahead of longer symbols containing it") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "btc")).getOrThrow()

                        results.first().symbol shouldBe "BTC"
                    }
                }
            }

            When("the query matches the start of a name rather than a symbol") {
                Then("it is found by name") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "ethe")).getOrThrow()

                        results.map { it.symbol } shouldContain "ETH"
                    }
                }
            }

            When("the query matches nothing") {
                Then("the result is empty rather than everything") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "zzzzz")).getOrThrow()

                        results shouldBe emptyList()
                    }
                }
            }

            When("there is no query at all") {
                Then("well known assets lead, rather than whatever sorts first alphabetically") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "")).getOrThrow()

                        results.first().symbol shouldBe "BTC"
                    }
                }

                Then("the list has no duplicates, even though popular assets are promoted") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "")).getOrThrow()

                        results.size shouldBe results.distinct().size
                    }
                }
            }

            When("a limit is given and there is a query to rank against") {
                Then("no more than that many results come back") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "b", limit = 2)).getOrThrow()

                        results.size shouldBe 2
                    }
                }
            }

            When("there is no query") {
                Then("the whole catalogue comes back, limit or not") {
                    runTest {
                        // Browsing is not ranked, so a cap is not a relevance judgement: it hides
                        // rows already in memory and makes the list stop at an arbitrary letter.
                        val useCase = useCase(testScheduler)

                        val results = useCase(SearchAssetsUseCase.Params(query = "", limit = 2)).getOrThrow()

                        results.size shouldBe Catalogue.size
                    }
                }
            }
        }

        Given("an asset already in the portfolio") {
            When("the catalogue is searched") {
                Then("it is not offered, so it cannot be added twice") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results =
                            useCase(
                                SearchAssetsUseCase.Params(query = "btc", excludedSymbols = setOf("BTC")),
                            ).getOrThrow()

                        results.map { it.symbol } shouldNotContain "BTC"
                    }
                }

                Then("other matches are still offered") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results =
                            useCase(
                                SearchAssetsUseCase.Params(query = "btc", excludedSymbols = setOf("BTC")),
                            ).getOrThrow()

                        results.map { it.symbol } shouldContain "WBTC"
                    }
                }

                Then("the exclusion is case insensitive, since a symbol can be typed either way") {
                    runTest {
                        val useCase = useCase(testScheduler)

                        val results =
                            useCase(
                                SearchAssetsUseCase.Params(query = "", excludedSymbols = setOf("btc")),
                            ).getOrThrow()

                        results.map { it.symbol } shouldNotContain "BTC"
                    }
                }
            }
        }

        Given("a catalogue that cannot be reached") {
            When("a search runs") {
                Then("the failure is returned rather than thrown at the caller") {
                    runTest {
                        val useCase =
                            SearchAssetsUseCase(
                                assetCatalogRepository = FailingCatalog,
                                coroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
                            )

                        useCase(SearchAssetsUseCase.Params(query = "btc")).isFailure shouldBe true
                    }
                }
            }
        }
    })

private val Catalogue =
    listOf(
        Asset(symbol = "AAVE", name = "Aave"),
        Asset(symbol = "BTC", name = "Bitcoin"),
        Asset(symbol = "ETH", name = "Ethereum"),
        Asset(symbol = "WBTC", name = "Wrapped Bitcoin"),
    )

private fun useCase(scheduler: TestCoroutineScheduler) =
    SearchAssetsUseCase(
        assetCatalogRepository =
            object : AssetCatalogRepository {
                override suspend fun tradableAssets(): List<Asset> = Catalogue
            },
        coroutineDispatcher = UnconfinedTestDispatcher(scheduler),
    )

private val FailingCatalog =
    object : AssetCatalogRepository {
        override suspend fun tradableAssets(): List<Asset> = error("offline")
    }
