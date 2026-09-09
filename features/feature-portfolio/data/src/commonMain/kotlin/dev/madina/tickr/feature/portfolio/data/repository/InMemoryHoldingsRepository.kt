package dev.madina.tickr.feature.portfolio.data.repository

import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holdings kept in memory, seeded with a sample portfolio.
 *
 * Deliberately the first implementation: it satisfies the whole domain contract, so the feature can
 * be built and tested end to end before deciding how each platform persists. The persistent version
 * replaces this class and nothing else, which is the point of the interface living in `domain`.
 */
internal class InMemoryHoldingsRepository : HoldingsRepository {

    private val holdings = MutableStateFlow(SampleHoldings)

    override fun observeHoldings(): Flow<List<Holding>> = holdings.asStateFlow()

    override suspend fun addHolding(holding: Holding) {
        holdings.update { current ->
            // Adding a symbol that is already held replaces it, rather than producing two rows for
            // one asset that would then be summed twice in the total.
            current.filterNot { it.symbol == holding.symbol } + holding
        }
    }

    override suspend fun removeHolding(symbol: String) {
        holdings.update { current -> current.filterNot { it.symbol == symbol } }
    }
}

private val SampleHoldings = listOf(
    Holding(symbol = "BTC", name = "Bitcoin", quantity = 0.241, averageCost = 62_100.0),
    Holding(symbol = "ETH", name = "Ethereum", quantity = 1.80, averageCost = 2_010.0),
    Holding(symbol = "SOL", name = "Solana", quantity = 12.4, averageCost = 88.20),
    Holding(symbol = "XRP", name = "XRP", quantity = 940.0, averageCost = 1.74),
)
