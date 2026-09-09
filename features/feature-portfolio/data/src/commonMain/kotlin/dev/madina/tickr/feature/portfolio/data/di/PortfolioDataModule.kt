package dev.madina.tickr.feature.portfolio.data.di

import dev.madina.tickr.core.network.TickrJson
import dev.madina.tickr.core.network.createHttpClient
import dev.madina.tickr.core.storage.createSettings
import dev.madina.tickr.feature.portfolio.data.repository.CoinbaseAssetCatalogRepository
import dev.madina.tickr.feature.portfolio.data.repository.CoinbasePriceRepository
import dev.madina.tickr.feature.portfolio.data.repository.StoredHoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The implementations are `internal`, so this module is the only way out of the data layer. That is
 * what stops the ui module from reaching past the domain interfaces even by accident.
 *
 * Swapping the live feed for a fake is a one-line change here and nothing else in the app knows
 * the difference, which is the whole argument for the interface living in `domain`.
 */
val portfolioDataModule: Module =
    module {
        single<HttpClient> { createHttpClient() }
        single<HoldingsRepository> {
            StoredHoldingsRepository(
                settings = createSettings(name = "tickr.portfolio"),
                json = TickrJson,
            )
        }
        single<PriceRepository> { CoinbasePriceRepository(httpClient = get()) }
        single<AssetCatalogRepository> { CoinbaseAssetCatalogRepository(httpClient = get()) }
    }
