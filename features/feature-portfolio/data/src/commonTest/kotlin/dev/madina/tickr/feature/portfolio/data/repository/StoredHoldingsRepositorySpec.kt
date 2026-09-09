package dev.madina.tickr.feature.portfolio.data.repository

import com.russhwolf.settings.MapSettings
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

/**
 * Persistence is verified by building a second repository over the same store, which is what a
 * restart amounts to. Asserting on the first instance would only prove it remembers its own
 * in-memory state.
 */
internal class StoredHoldingsRepositorySpec :
    BehaviorSpec({

        Given("a store that has never been written to") {
            When("the repository is created") {
                Then("it seeds a sample portfolio, so a first run is not an empty screen") {
                    runTest {
                        val repository = StoredHoldingsRepository(MapSettings(), TestJson)

                        repository.observeHoldings().first().shouldNotBeEmpty()
                    }
                }
            }
        }

        Given("a holding added in one session") {
            When("a new repository opens the same store") {
                Then("the holding is still there") {
                    runTest {
                        val settings = MapSettings()
                        StoredHoldingsRepository(settings, TestJson)
                            .addHolding(Holding("DOGE", "Dogecoin", quantity = 5.0, averageCost = 0.1))

                        val reopened = StoredHoldingsRepository(settings, TestJson)

                        reopened.observeHoldings().first().map { it.symbol } shouldContain "DOGE"
                    }
                }

                Then("its numbers survive the round trip intact") {
                    runTest {
                        val settings = MapSettings()
                        StoredHoldingsRepository(settings, TestJson)
                            .addHolding(Holding("DOGE", "Dogecoin", quantity = 5.5, averageCost = 0.125))

                        val restored =
                            StoredHoldingsRepository(settings, TestJson)
                                .observeHoldings()
                                .first()
                                .first { it.symbol == "DOGE" }

                        restored.quantity shouldBe 5.5
                        restored.averageCost shouldBe 0.125
                        restored.name shouldBe "Dogecoin"
                    }
                }
            }
        }

        Given("every holding removed") {
            When("a new repository opens the same store") {
                Then("it stays empty rather than resurrecting the samples") {
                    runTest {
                        val settings = MapSettings()
                        val repository = StoredHoldingsRepository(settings, TestJson)
                        repository.observeHoldings().first().forEach { repository.removeHolding(it.symbol) }

                        val reopened = StoredHoldingsRepository(settings, TestJson)

                        reopened.observeHoldings().first() shouldContainExactly emptyList()
                    }
                }
            }
        }

        Given("a symbol that is already held") {
            When("it is added again with different numbers") {
                Then("it replaces the position instead of duplicating the asset") {
                    runTest {
                        val settings = MapSettings()
                        val repository = StoredHoldingsRepository(settings, TestJson)
                        repository.addHolding(Holding("BTC", "Bitcoin", quantity = 1.0, averageCost = 10.0))
                        repository.addHolding(Holding("BTC", "Bitcoin", quantity = 2.0, averageCost = 20.0))

                        val stored = repository.observeHoldings().first().filter { it.symbol == "BTC" }

                        stored.size shouldBe 1
                        stored.single().quantity shouldBe 2.0
                    }
                }
            }
        }

        Given("stored data that cannot be parsed, as an older version might have written") {
            When("the repository is created") {
                Then("it starts empty rather than crashing on launch") {
                    runTest {
                        val settings = MapSettings()
                        settings.putString("holdings", "{not json at all")

                        val repository = StoredHoldingsRepository(settings, TestJson)

                        repository.observeHoldings().first() shouldContainExactly emptyList()
                    }
                }
            }
        }
    })

private val TestJson = Json { ignoreUnknownKeys = true }
