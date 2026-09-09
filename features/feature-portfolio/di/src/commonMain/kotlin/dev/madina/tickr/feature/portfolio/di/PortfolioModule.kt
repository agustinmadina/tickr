package dev.madina.tickr.feature.portfolio.di

import dev.madina.tickr.core.common.DispatcherProvider
import dev.madina.tickr.feature.portfolio.data.di.portfolioDataModule
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.RemoveHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.SearchAssetsUseCase
import dev.madina.tickr.feature.portfolio.ui.di.portfolioUiModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The feature's single entry point for the composition root: including this wires every layer.
 *
 * Use cases are declared here rather than in `domain/`, because a Koin module is a framework
 * dependency and the domain module must not have one. The dispatcher is injected rather than
 * defaulted, so tests substitute a deterministic one.
 */
val portfolioModule: Module =
    module {
        includes(portfolioDataModule, portfolioUiModule)

        factory { ObservePortfolioUseCase(get(), get(), get<DispatcherProvider>().default) }
        factory { AddHoldingUseCase(get(), get<DispatcherProvider>().default) }
        factory { RemoveHoldingUseCase(get(), get<DispatcherProvider>().default) }
        factory { SearchAssetsUseCase(get(), get<DispatcherProvider>().default) }
    }
