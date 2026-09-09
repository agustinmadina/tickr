package com.example.app.feature.{{FEATURE_PACKAGE}}.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.example.app.feature.{{FEATURE_PACKAGE}}.ui.{{PASCAL}}ViewModel

val {{CAMEL}}UiModule = module {
    viewModel { {{PASCAL}}ViewModel() }
}
