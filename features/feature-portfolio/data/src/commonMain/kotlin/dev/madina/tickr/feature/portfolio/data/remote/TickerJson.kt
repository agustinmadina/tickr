package dev.madina.tickr.feature.portfolio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A ticker message from Coinbase's public feed.
 *
 * Every field is nullable with a default: this is somebody else's wire format, arriving over a
 * socket that also carries subscription acknowledgements and errors, and a missing field must not
 * throw in the middle of a stream. Prices come as strings because the exchange sends them that way,
 * to avoid the precision loss of a JSON number.
 */
@Serializable
internal data class TickerJson(
    val type: String? = null,
    @SerialName("product_id") val productId: String? = null,
    val price: String? = null,
    @SerialName("open_24h") val open24h: String? = null,
)
