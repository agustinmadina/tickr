package dev.madina.tickr.feature.portfolio.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

internal class PortfolioSpec : BehaviorSpec({

    Given("a holding priced above its average cost") {
        When("its value is read") {
            val valued = ValuedHolding(
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
            val valued = ValuedHolding(
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
            val valued = ValuedHolding(
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
            val portfolio = Portfolio(
                holdings = listOf(
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
