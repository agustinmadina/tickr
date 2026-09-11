package dev.madina.tickr.core.ui.component

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * State is created inside each `Then`: Kotest's isolation modes are silently ignored on
 * Kotlin/Native, so anything shared would leak between blocks and fail on iOS only.
 */
internal class ChartRangeSpec :
    BehaviorSpec({

        Given("a series that moved enough to be worth drawing") {

            When("it is mapped onto the chart") {
                Then("the extremes reach the top and the bottom") {
                    // A 10% swing is far past the floor, so nothing is compressed.
                    val scale = ChartRange(listOf(90f, 95f, 100f))

                    scale.fractionOf(90f) shouldBe 0f
                    scale.fractionOf(100f) shouldBe 1f
                    scale.fractionOf(95f).toDouble() shouldBe (0.5 plusOrMinus Tolerance)
                }
            }
        }

        Given("a series that barely moved") {

            When("it is mapped onto the chart") {
                Then("it stays near the middle instead of filling the height") {
                    // 0.05% of the value, the kind of wobble a few minutes of ticking produces.
                    // Plain min/max normalisation drew this as a full-height mountain, so a reader
                    // could not tell it from a real move.
                    val scale = ChartRange(listOf(78_000f, 78_040f))

                    scale.fractionOf(78_000f) shouldBeGreaterThan 0.4f
                    scale.fractionOf(78_040f) shouldBeLessThan 0.6f
                }
            }
        }

        Given("a series that never changed at all") {

            When("it is mapped onto the chart") {
                Then("every point sits on the centre line") {
                    val scale = ChartRange(listOf(42f, 42f, 42f))

                    scale.fractionOf(42f).toDouble() shouldBe (0.5 plusOrMinus Tolerance)
                }
            }
        }

        Given("a series of zeroes, which has no magnitude to scale against") {

            When("it is mapped onto the chart") {
                Then("it is centred rather than dividing by zero") {
                    val scale = ChartRange(listOf(0f, 0f))

                    scale.fractionOf(0f).toDouble() shouldBe (0.5 plusOrMinus Tolerance)
                }
            }
        }

        Given("a value outside the series") {

            When("it is mapped onto the chart") {
                Then("it is clamped to the chart rather than drawn off it") {
                    val scale = ChartRange(listOf(90f, 100f))

                    scale.fractionOf(1_000f) shouldBe 1f
                    scale.fractionOf(0f) shouldBe 0f
                }
            }
        }
    })

private const val Tolerance = 1e-6
