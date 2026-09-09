package com.example.app.feature.{{FEATURE_PACKAGE}}.di

import org.koin.dsl.module
import com.example.app.feature.{{FEATURE_PACKAGE}}.data.di.{{CAMEL}}DataModule
import com.example.app.feature.{{FEATURE_PACKAGE}}.ui.di.{{CAMEL}}UiModule

val {{CAMEL}}Module = module {
    includes({{CAMEL}}DataModule, {{CAMEL}}UiModule)
}
