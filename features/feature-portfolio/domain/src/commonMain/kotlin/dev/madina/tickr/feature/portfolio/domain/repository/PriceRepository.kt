package dev.madina.tickr.feature.portfolio.domain.repository

import dev.madina.tickr.feature.portfolio.domain.model.FeedStatus
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

    /**
     * Whether the feed is currently delivering.
     *
     * Separate from [observePrices] because it outlives any one subscription: the symbol set
     * changes whenever a holding is added, which tears that flow down, and the state of the
     * connection is not reset by an edit to the portfolio.
     */
    fun observeStatus(): Flow<FeedStatus>
}
