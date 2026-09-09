package com.example.app.feature.{{FEATURE_PACKAGE}}.data.di

import org.koin.dsl.module
import com.example.app.feature.{{FEATURE_PACKAGE}}.data.repository.{{PASCAL}}RepositoryImpl
import com.example.app.feature.{{FEATURE_PACKAGE}}.domain.repository.{{PASCAL}}Repository

val {{CAMEL}}DataModule = module {
    single<{{PASCAL}}Repository> { {{PASCAL}}RepositoryImpl() }
}
