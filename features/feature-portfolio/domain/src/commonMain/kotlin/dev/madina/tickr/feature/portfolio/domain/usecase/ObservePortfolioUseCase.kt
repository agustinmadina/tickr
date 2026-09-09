package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.FlowUseCase
import dev.madina.tickr.feature.portfolio.domain.model.Portfolio
import dev.madina.tickr.feature.portfolio.domain.model.ValuedHolding
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * The portfolio as one stream: what the user holds, priced with whatever the feed has sent so far.
 *
 * This is the only place the two sources are merged. The ViewModel observes a single pre-merged
 * stream, because combines in a ViewModel accumulate until nobody can say where a value came from.
 *
 * The nesting matters: the set of symbols to subscribe to is derived from the holdings, so a change
 * to the holdings has to re-subscribe the price feed. `flatMapLatest` cancels the previous
 * subscription when that happens, which is what stops a removed asset from still streaming.
 * `distinctUntilChanged` on the symbol set keeps an unrelated edit, such as correcting a quantity,
 * from tearing down a healthy connection.
 */
class ObservePortfolioUseCase(
    private val holdingsRepository: HoldingsRepository,
    private val priceRepository: PriceRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : FlowUseCase<Unit, Portfolio>(coroutineDispatcher) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(parameters: Unit): Flow<Portfolio> {
        val holdings = holdingsRepository.observeHoldings()
        val prices = holdings
            .map { current -> current.map { it.symbol }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest(priceRepository::observePrices)

        return combine(holdings, prices) { currentHoldings, currentPrices ->
            Portfolio(
                holdings = currentHoldings.map { holding ->
                    ValuedHolding(holding = holding, price = currentPrices[holding.symbol])
                },
            )
        }
    }
}
