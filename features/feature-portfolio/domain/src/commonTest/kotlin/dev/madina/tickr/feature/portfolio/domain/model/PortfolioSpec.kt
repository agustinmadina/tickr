package dev.madina.tickr.feature.portfolio.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

internal class PortfolioSpec :
    BehaviorSpec({

        Given("a holding priced above its average cost") {
            When("its value is read") {
                val valued =
                    ValuedHolding(
                        holding = Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 100.0),
                        price = PriceTick(symbol = "BTC", price = 150.0, changePercent24h = 1.0),
                    )

                Then("value, profit and return follow from quantity and cost") {
                    valued.value shouldBe 300.0
                    valued.cost shouldBe 200.0
                    valued.profit shouldBe 100.0
                    valued.returnPercent!! shouldBe (50.0 plusOrMinus TOLERANCE)
                }
            }
        }

        Given("a holding with no quote yet") {
            When("its value is read") {
                val valued =
                    ValuedHolding(
                        holding = Holding(symbol = "ETH", name = "Ethereum", quantity = 1.0, averageCost = 10.0),
                        price = null,
                    )

                Then("value and profit are unknown rather than zero") {
                    valued.value.shouldBeNull()
                    valued.profit.shouldBeNull()
                    valued.returnPercent.shouldBeNull()
                }

                Then("cost is still known, since it does not depend on the market") {
                    valued.cost shouldBe 10.0
                }
            }
        }

        Given("a holding acquired at no cost, such as an airdrop") {
            When("its return is read") {
                val valued =
                    ValuedHolding(
                        holding = Holding(symbol = "SOL", name = "Solana", quantity = 5.0, averageCost = 0.0),
                        price = PriceTick(symbol = "SOL", price = 20.0, changePercent24h = 0.0),
                    )

                Then("return is undefined rather than infinite") {
                    valued.returnPercent.shouldBeNull()
                }

                Then("profit is still the whole value") {
                    valued.profit shouldBe 100.0
                }
            }
        }

        Given("a portfolio where one asset is priced and another is not") {
            When("the totals are read") {
                val portfolio =
                    Portfolio(
                        holdings =
                            listOf(
                                ValuedHolding(
                                    holding = Holding("BTC", "Bitcoin", quantity = 1.0, averageCost = 100.0),
                                    price = PriceTick("BTC", price = 250.0, changePercent24h = 0.0),
                                ),
                                ValuedHolding(
                                    holding = Holding("ETH", "Ethereum", quantity = 1.0, averageCost = 50.0),
                                    price = null,
                                ),
                            ),
                    )

                Then("the unpriced holding contributes nothing to the value") {
                    portfolio.totalValue shouldBe 250.0
                }

                Then("but it does contribute to the cost, which is already known") {
                    portfolio.totalCost shouldBe 150.0
                }

                Then("the portfolio reports that it is only partly priced") {
                    portfolio.isPartiallyPriced shouldBe true
                }
            }
        }

        Given("a holding that rose 25% today") {
            When("today's move is read") {
                val valued =
                    ValuedHolding(
                        holding = Holding(symbol = "BTC", name = "Bitcoin", quantity = 2.0, averageCost = 1.0),
                        price = PriceTick(symbol = "BTC", price = 125.0, changePercent24h = 25.0),
                    )

                Then("yesterday's value is recovered by dividing the change out") {
                    valued.valueYesterday!! shouldBe (200.0 plusOrMinus TOLERANCE)
                }

                Then("the day change is the difference, not the percentage of today's price") {
                    valued.dayChange!! shouldBe (50.0 plusOrMinus TOLERANCE)
                }
            }
        }

        Given("an asset that lost all of its value today") {
            When("yesterday's value is read") {
                val valued =
                    ValuedHolding(
                        holding = Holding(symbol = "ZZZ", name = "Gone", quantity = 1.0, averageCost = 1.0),
                        price = PriceTick(symbol = "ZZZ", price = 0.0, changePercent24h = -100.0),
                    )

                Then("it is unknown rather than infinite, which would poison the portfolio total") {
                    valued.valueYesterday.shouldBeNull()
                    valued.dayChange.shouldBeNull()
                }
            }
        }

        Given("a portfolio of two holdings moving in opposite directions today") {
            When("the day change is read") {
                val portfolio =
                    Portfolio(
                        holdings =
                            listOf(
                                ValuedHolding(
                                    holding = Holding("BTC", "Bitcoin", quantity = 1.0, averageCost = 1.0),
                                    price = PriceTick("BTC", price = 110.0, changePercent24h = 10.0),
                                ),
                                ValuedHolding(
                                    holding = Holding("ETH", "Ethereum", quantity = 1.0, averageCost = 1.0),
                                    price = PriceTick("ETH", price = 90.0, changePercent24h = -10.0),
                                ),
                            ),
                    )

                Then("the moves net off against each other") {
                    // 100 -> 110 is +10, and 100 -> 90 is -10, so the portfolio is flat in money.
                    portfolio.dayChange shouldBe (0.0 plusOrMinus TOLERANCE)
                }

                Then("the percentage is against yesterday's total, not an average of the two") {
                    portfolio.dayChangePercent!! shouldBe (0.0 plusOrMinus TOLERANCE)
                }
            }
        }

        Given("an empty portfolio") {
            When("the totals are read") {
                val portfolio = Portfolio(holdings = emptyList())

                Then("it reports empty and has no return to speak of") {
                    portfolio.isEmpty shouldBe true
                    portfolio.totalValue shouldBe 0.0
                    portfolio.totalReturnPercent.shouldBeNull()
                }

                Then("nothing is pending a price") {
                    portfolio.isPartiallyPriced shouldBe false
                }
            }
        }
    })

private const val TOLERANCE = 0.0001
