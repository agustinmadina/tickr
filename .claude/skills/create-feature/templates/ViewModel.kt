package com.example.app.feature.{{FEATURE_PACKAGE}}.ui

import com.example.app.core.ui.base.BaseViewModel

internal class {{PASCAL}}ViewModel :
    BaseViewModel<{{PASCAL}}Action, {{PASCAL}}Effect, {{PASCAL}}UiState>({{PASCAL}}UiState()) {

    override suspend fun handleAction(action: {{PASCAL}}Action) {
        when (action) {
            is {{PASCAL}}Action.Load -> load()
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true) }
        // Load data here
        updateState { it.copy(isLoading = false) }
    }
}
