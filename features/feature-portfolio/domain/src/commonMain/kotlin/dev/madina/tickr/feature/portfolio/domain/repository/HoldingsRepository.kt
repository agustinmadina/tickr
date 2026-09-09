package dev.madina.tickr.feature.portfolio.domain.repository

import dev.madina.tickr.feature.portfolio.domain.model.Holding
import kotlinx.coroutines.flow.Flow

interface HoldingsRepository {
    fun observeHoldings(): Flow<List<Holding>>

    suspend fun addHolding(holding: Holding)

    suspend fun removeHolding(symbol: String)
}
