package dev.madina.tickr.feature.portfolio.domain.model

/**
 * How far back the charts and the percentages beside them look.
 *
 * One window drives both, which is the whole point: the chart and the number next to it have to be
 * talking about the same stretch of time or the screen contradicts itself.
 *
 * [granularitySeconds] is what the exchange accepts; [points] is how many of those fit the window.
 * Coinbase returns at most 350 candles per request, so every combination here stays under that.
 */
enum class HistoryRange(
    val granularitySeconds: Int,
    val points: Int,
) {
    Day(granularitySeconds = 3_600, points = 24),
    Week(granularitySeconds = 21_600, points = 28),
    Month(granularitySeconds = 21_600, points = 120),
    Year(granularitySeconds = 86_400, points = 350),
}
