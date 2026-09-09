package com.example.app.feature.{{FEATURE_PACKAGE}}.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import example.xyz.mobiledesignsystem.designsystem.components.loader.YesLoader

@Composable
internal fun {{PASCAL}}Screen(
    viewModel: {{PASCAL}}ViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.submitAction({{PASCAL}}Action.Load)
    }

    {{PASCAL}}Content(state = state)
}

@Composable
private fun {{PASCAL}}Content(
    state: {{PASCAL}}UiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isLoading) {
            YesLoader()
        }
    }
}
