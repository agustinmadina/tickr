package dev.madina.tickr.feature.portfolio.data.local

import kotlinx.serialization.Serializable

/**
 * A holding as it is stored.
 *
 * Separate from the domain model even though the fields match today: what is written to a user's
 * device outlives any given version of the app, so a rename in the domain must not silently orphan
 * everything already saved.
 */
@Serializable
internal data class HoldingEntity(
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCost: Double,
)
