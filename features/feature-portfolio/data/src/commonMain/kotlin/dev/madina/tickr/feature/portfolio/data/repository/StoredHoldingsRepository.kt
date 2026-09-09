package dev.madina.tickr.feature.portfolio.data.repository

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import dev.madina.tickr.feature.portfolio.data.local.HoldingEntity
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Holdings that survive a restart, kept as one serialised document in the platform's key-value
 * store: SharedPreferences, NSUserDefaults or localStorage.
 *
 * The whole list is read once at construction into a [MutableStateFlow] and written back on every
 * change. That is the right shape for this data, which is a handful of rows always read and written
 * whole, and it means observers get an in-memory flow rather than a read from disk per emission,
 * with the price feed ticking several times a second above it.
 *
 * First run seeds a sample portfolio. Opening a demo to an empty screen tells a visitor nothing,
 * and the seed is only ever written when the store has never been written to, so removing every
 * holding leaves it empty rather than resurrecting the samples on next launch.
 */
internal class StoredHoldingsRepository(
    private val settings: Settings,
    private val json: Json,
    private val logger: Logger = Logger.withTag("StoredHoldingsRepository"),
) : HoldingsRepository {
    private val writeMutex = Mutex()
    private val holdings = MutableStateFlow(load())

    override fun observeHoldings(): Flow<List<Holding>> = holdings.asStateFlow()

    override suspend fun addHolding(holding: Holding) =
        mutate { current ->
            // Adding a symbol already held replaces it, rather than producing two rows for one asset
            // that would then be counted twice in the total.
            current.filterNot { it.symbol == holding.symbol } + holding
        }

    override suspend fun removeHolding(symbol: String) =
        mutate { current ->
            current.filterNot { it.symbol == symbol }
        }

    private suspend fun mutate(transform: (List<Holding>) -> List<Holding>) {
        writeMutex.withLock {
            val previous = holdings.value
            val updated = transform(previous)
            holdings.value = updated
            try {
                persist(updated)
            } catch (failure: Exception) {
                // Rolled back and rethrown, so a store that refuses the write surfaces as a message
                // instead of leaving a row on screen that is gone on the next launch.
                holdings.value = previous
                logger.e(failure) { "Could not save holdings" }
                throw failure
            }
        }
    }

    private fun persist(updated: List<Holding>) {
        val entities = updated.map { HoldingEntity(it.symbol, it.name, it.quantity, it.averageCost) }
        settings.putString(HoldingsKey, json.encodeToString(entities))
    }

    private fun load(): List<Holding> {
        // A store that cannot be read at all, rather than one that is merely empty, is treated as a
        // first run. Reading it used to be outside any guard, in a property initializer, so a
        // browser with storage blocked took the app down while Koin was still resolving.
        val stored =
            runCatching { settings.getStringOrNull(HoldingsKey) }
                .getOrElse { failure ->
                    logger.e(failure) { "Holdings store is unreadable, seeding the sample portfolio" }
                    null
                } ?: return SampleHoldings

        return runCatching { json.decodeFromString<List<HoldingEntity>>(stored) }
            .map { entities -> entities.map { Holding(it.symbol, it.name, it.quantity, it.averageCost) } }
            .getOrElse { failure ->
                // Stored data written by an older version can fail to parse. Starting empty beats
                // crashing on launch with no way for the user to recover.
                logger.e(failure) { "Stored holdings could not be read, starting empty" }
                emptyList()
            }
    }
}

private const val HoldingsKey = "holdings"

private val SampleHoldings =
    listOf(
        Holding(symbol = "BTC", name = "Bitcoin", quantity = 0.241, averageCost = 62_100.0),
        Holding(symbol = "ETH", name = "Ethereum", quantity = 1.80, averageCost = 2_010.0),
        Holding(symbol = "SOL", name = "Solana", quantity = 12.4, averageCost = 88.20),
        Holding(symbol = "XRP", name = "XRP", quantity = 940.0, averageCost = 1.74),
    )
