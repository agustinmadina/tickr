package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

internal class AddHoldingUseCaseSpec : BehaviorSpec({

    Given("a symbol typed in lower case with surrounding spaces") {
        When("it is added") {
            Then("it is stored upper case and trimmed, so it matches the feed's product ids") {
                runTest {
                    val repository = RecordingHoldingsRepository()
                    val useCase = AddHoldingUseCase(repository, UnconfinedTestDispatcher(testScheduler))

                    useCase(
                        AddHoldingUseCase.Params(
                            symbol = "  btc ",
                            name = "Bitcoin",
                            quantity = 1.0,
                            averageCost = 100.0,
                        ),
                    ).isSuccess shouldBe true

                    repository.added.single().symbol shouldBe "BTC"
                }
            }
        }
    }

    Given("no name for the asset") {
        When("it is added") {
            Then("the symbol stands in, rather than storing a blank name") {
                runTest {
                    val repository = RecordingHoldingsRepository()
                    val useCase = AddHoldingUseCase(repository, UnconfinedTestDispatcher(testScheduler))

                    useCase(
                        AddHoldingUseCase.Params(symbol = "sol", name = "   ", quantity = 1.0, averageCost = 1.0),
                    )

                    repository.added.single().name shouldBe "SOL"
                }
            }
        }
    }

    Given("a quantity of zero") {
        When("it is added") {
            Then("it is rejected, since it would sit in the portfolio contributing nothing") {
                runTest {
                    val repository = RecordingHoldingsRepository()
                    val useCase = AddHoldingUseCase(repository, UnconfinedTestDispatcher(testScheduler))

                    val result = useCase(
                        AddHoldingUseCase.Params(symbol = "BTC", name = "Bitcoin", quantity = 0.0, averageCost = 1.0),
                    )

                    result.isFailure shouldBe true
                    repository.added.isEmpty() shouldBe true
                }
            }
        }
    }

    Given("a negative average cost") {
        When("it is added") {
            Then("it is rejected, since profit against it would be meaningless") {
                runTest {
                    val repository = RecordingHoldingsRepository()
                    val useCase = AddHoldingUseCase(repository, UnconfinedTestDispatcher(testScheduler))

                    val result = useCase(
                        AddHoldingUseCase.Params(
                            symbol = "BTC",
                            name = "Bitcoin",
                            quantity = 1.0,
                            averageCost = -5.0,
                        ),
                    )

                    result.isFailure shouldBe true
                }
            }
        }
    }

    Given("a blank symbol") {
        When("it is added") {
            Then("it is rejected") {
                runTest {
                    val repository = RecordingHoldingsRepository()
                    val useCase = AddHoldingUseCase(repository, UnconfinedTestDispatcher(testScheduler))

                    useCase(
                        AddHoldingUseCase.Params(symbol = "   ", name = "x", quantity = 1.0, averageCost = 1.0),
                    ).isFailure shouldBe true
                }
            }
        }
    }
})

private class RecordingHoldingsRepository : HoldingsRepository {
    val added = mutableListOf<Holding>()

    override fun observeHoldings(): Flow<List<Holding>> = MutableStateFlow(added.toList())

    override suspend fun addHolding(holding: Holding) {
        added += holding
    }

    override suspend fun removeHolding(symbol: String) {
        added.removeAll { it.symbol == symbol }
    }
}
