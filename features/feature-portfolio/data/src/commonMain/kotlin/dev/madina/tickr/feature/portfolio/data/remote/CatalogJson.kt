package dev.madina.tickr.feature.portfolio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An entry from Coinbase's `/products`: one tradable pair, such as `BTC-USD`. */
@Serializable
internal data class ProductJson(
    val id: String? = null,
    @SerialName("base_currency") val baseCurrency: String? = null,
    @SerialName("quote_currency") val quoteCurrency: String? = null,
    val status: String? = null,
    @SerialName("trading_disabled") val tradingDisabled: Boolean? = null,
)

/**
 * An entry from Coinbase's `/currencies`, which is where the human readable name lives.
 *
 * `/products` carries only symbols, so a selector built from it alone would offer the user a list
 * of tickers with no indication of what any of them are.
 */
@Serializable
internal data class CurrencyJson(
    val id: String? = null,
    val name: String? = null,
)
