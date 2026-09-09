package dev.madina.tickr.feature.portfolio.ui.di

import dev.madina.tickr.feature.portfolio.ui.PortfolioViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The ViewModel is `internal`, so it can only be registered from inside this Gradle module. That is
 * why the wiring is split: `di/` aggregates modules rather than naming implementation types, which
 * lets every layer keep its internals internal.
 */
val portfolioUiModule: Module =
    module {
        viewModel { PortfolioViewModel(get(), get(), get(), get()) }
    }
