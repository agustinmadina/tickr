package dev.madina.tickr.feature.portfolio.domain.repository

import dev.madina.tickr.feature.portfolio.domain.model.PricePoint

interface PriceHistoryRepository {
    /**
     * The last day of hourly closes for one asset, oldest first.
     *
     * Empty when the exchange has no history for the symbol, or when it cannot be reached. An empty
     * series is a chart that does not draw, which is the honest outcome: the alternative is drawing
     * the handful of ticks collected since launch, which is a fraction of a percent of movement
     * stretched to full height and says nothing about the day.
     */
    suspend fun recentDay(symbol: String): List<PricePoint>
}
