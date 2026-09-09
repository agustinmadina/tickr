package dev.madina.tickr.feature.portfolio.data.remote

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The arithmetic that decides what a position is worth on screen, against somebody else's wire
 * format. State is built inside each `Then`: Kotest's isolation modes are silently ignored on
 * Kotlin/Native, so anything shared would leak between blocks and fail on iOS only.
 */
internal class TickerMapperSpec :
    BehaviorSpec({

        Given("a ticker frame from the feed") {

            When("it carries a price and an opening price") {
                Then("the daily change is the move from the open, as a percentage") {
                    val tick =
                        TickerJson(price = "110.0", open24h = "100.0").toPriceTick("BTC")

                    tick.shouldNotBe(null)
                    tick?.price shouldBe 110.0
                    tick?.changePercent24h?.shouldBe(10.0 plusOrMinus Tolerance)
                }

                Then("a fall reads as a negative change") {
                    val tick = TickerJson(price = "80.0", open24h = "100.0").toPriceTick("BTC")

                    tick?.changePercent24h?.shouldBe(-20.0 plusOrMinus Tolerance)
                }
            }

            When("the opening price is missing") {
                Then("the change is unknown rather than zero") {
                    // Zero would say the asset is exactly flat today, which is a claim the feed
                    // never made. The screen renders unknown and flat differently.
                    val tick = TickerJson(price = "110.0", open24h = null).toPriceTick("BTC")

                    tick?.price shouldBe 110.0
                    tick?.changePercent24h.shouldBeNull()
                }
            }

            When("the opening price is zero") {
                Then("the change is unknown, not an infinity") {
                    val tick = TickerJson(price = "110.0", open24h = "0").toPriceTick("BTC")

                    tick?.changePercent24h.shouldBeNull()
                }
            }

            When("it carries no price at all") {
                Then("nothing is produced, so the holding stays unpriced") {
                    TickerJson(price = null, open24h = "100.0").toPriceTick("BTC").shouldBeNull()
                }

                Then("an unparseable price is treated the same way") {
                    TickerJson(price = "", open24h = "100.0").toPriceTick("BTC").shouldBeNull()
                }
            }

            When("a field overflows a Double") {
                Then("it is rejected rather than carried through as an infinity") {
                    // Kotlin's parser saturates rather than failing, and an infinite price would
                    // propagate into every total on the screen.
                    val huge = "1" + "0".repeat(400)

                    TickerJson(price = huge, open24h = "100.0").toPriceTick("BTC").shouldBeNull()
                    TickerJson(price = "110.0", open24h = huge)
                        .toPriceTick("BTC")
                        ?.changePercent24h
                        .shouldBeNull()
                }
            }

            When("the symbol is supplied by the caller") {
                Then("it is carried onto the tick rather than read from the product id") {
                    // The feed speaks in pairs, BTC-USD, and holdings are stored as the bare asset.
                    val tick = TickerJson(productId = "BTC-USD", price = "1.0").toPriceTick("BTC")

                    tick?.symbol shouldBe "BTC"
                }
            }
        }
    })

private const val Tolerance = 1e-9
