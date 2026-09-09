package dev.madina.tickr.feature.portfolio.domain.repository

import dev.madina.tickr.feature.portfolio.domain.model.PriceTick
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    /**
     * Latest price per symbol, keyed by symbol and updated as quotes arrive.
     *
     * Emits a whole map rather than individual ticks so a collector always sees a consistent
     * snapshot, and so a late subscriber immediately gets every price known so far instead of
     * waiting for each asset to tick once.
     */
    fun observePrices(symbols: Set<String>): Flow<Map<String, PriceTick>>
}
