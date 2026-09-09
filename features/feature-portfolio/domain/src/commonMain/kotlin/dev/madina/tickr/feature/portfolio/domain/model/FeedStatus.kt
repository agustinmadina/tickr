package dev.madina.tickr.feature.portfolio.domain.model

/**
 * Whether prices are arriving.
 *
 * Offline the app still has the holdings, because those are persisted, but every value on the
 * screen is blank. Without this the blanks read as a broken app rather than as an absent feed, and
 * the honest thing is to say which one it is.
 *
 * Prices themselves are deliberately not persisted: a quote with no timestamp beside it is a claim
 * about right now, and showing yesterday's as if it were current is worse than showing nothing.
 */
enum class FeedStatus {
    /** Never yet connected in this session. Distinct from [Disconnected]: nothing has failed. */
    Connecting,

    /** Quotes are arriving. */
    Live,

    /** The socket is down and being retried. Any prices on screen are from before it dropped. */
    Disconnected,
}
