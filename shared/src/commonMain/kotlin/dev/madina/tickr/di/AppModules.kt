package dev.madina.tickr.di

import dev.madina.tickr.core.common.DefaultDispatcherProvider
import dev.madina.tickr.core.common.DispatcherProvider
import dev.madina.tickr.feature.portfolio.di.portfolioModule
import org.koin.core.module.Module
import org.koin.dsl.module

private val coreModule: Module =
    module {
        single<DispatcherProvider> { DefaultDispatcherProvider() }
    }

/** The composition root: every feature is added here and nowhere else. */
val appModules: List<Module> = listOf(coreModule, portfolioModule)
