package dev.madina.tickr.feature.portfolio.data.di

import dev.madina.tickr.feature.portfolio.data.repository.InMemoryHoldingsRepository
import dev.madina.tickr.feature.portfolio.data.repository.SimulatedPriceRepository
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The implementations are `internal`, so this module is the only way out of the data layer. That is
 * what stops the ui module from reaching past the domain interfaces even by accident.
 */
val portfolioDataModule: Module = module {
    single<HoldingsRepository> { InMemoryHoldingsRepository() }
    single<PriceRepository> { SimulatedPriceRepository() }
}
