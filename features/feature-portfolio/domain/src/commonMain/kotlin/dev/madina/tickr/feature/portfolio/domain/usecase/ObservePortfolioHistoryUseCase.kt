package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.FlowUseCase
import dev.madina.tickr.feature.portfolio.domain.model.HistoryRange
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.model.PortfolioHistory
import dev.madina.tickr.feature.portfolio.domain.model.PricePoint
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * What the chosen window looked like, for every holding and for the portfolio as a whole.
 *
 * A separate stream from [ObservePortfolioUseCase] rather than a field on it: this is fetched once
 * per symbol over REST and changes only when the portfolio does, while prices arrive several times
 * a second. Merging them would refetch a day of candles on every tick.
 *
 * The charts used to draw whatever the app had seen since launch, which was minutes of movement
 * normalised to its own range: a tenth of a percent rendered as a mountain, next to a percentage
 * covering a full day. Drawing the day itself is what makes the line and the number beside it
 * describe the same thing.
 */
class ObservePortfolioHistoryUseCase(
    private val holdingsRepository: HoldingsRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : FlowUseCase<HistoryRange, PortfolioHistory>(coroutineDispatcher) {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(parameters: HistoryRange): Flow<PortfolioHistory> =
        holdingsRepository
            .observeHoldings()
            // Keyed on the holdings themselves, not just the symbols: a corrected quantity changes
            // what the total is worth at every point in the day, even though the symbol set did not
            // move. distinctUntilChanged still keeps a repeated emission from refetching.
            .distinctUntilChanged()
            .map { holdings -> historyFor(holdings, parameters) }

    private suspend fun historyFor(holdings: List<Holding>, range: HistoryRange): PortfolioHistory {
        if (holdings.isEmpty()) return PortfolioHistory()

        val bySymbol = holdings.associate { it.symbol to priceHistoryRepository.history(it.symbol, range) }

        return PortfolioHistory(
            perSymbol = bySymbol.mapValues { (_, points) -> points.map { it.close } },
            total = totalSeries(holdings, bySymbol),
        )
    }
}

/**
 * The portfolio's value at each instant every holding has a price for.
 *
 * Intersecting the timestamps rather than zipping by position is the point: two assets can be
 * missing different candles, and adding the nth close of one to the nth of another would sum two
 * different moments and call it a portfolio.
 */
private fun totalSeries(
    holdings: List<Holding>,
    bySymbol: Map<String, List<PricePoint>>,
): List<Double> {
    if (holdings.any { bySymbol[it.symbol].isNullOrEmpty() }) return emptyList()

    val shared =
        holdings
            .map { holding -> bySymbol.getValue(holding.symbol).map { it.epochSeconds }.toSet() }
            .reduce { common, next -> common intersect next }
            .sorted()

    return shared.map { instant ->
        holdings.sumOf { holding ->
            val close = bySymbol.getValue(holding.symbol).first { it.epochSeconds == instant }.close
            holding.quantity * close
        }
    }
}
