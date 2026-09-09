package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Host for the feature. Navigation is state driven rather than backed by a navigation library:
 * `AnimatedContent` switches on the destination held in [PortfolioUiState], which behaves
 * identically on Android, iOS and wasm and keeps the back stack in the ViewModel where a
 * configuration change cannot lose it.
 */
@Composable
fun PortfolioRoot(modifier: Modifier = Modifier) {
    val viewModel: PortfolioViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PortfolioEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.destination,
                transitionSpec = {
                    val forward = targetState is PortfolioDestination.Detail
                    val offset = if (forward) 1 else -1
                    (
                        slideInHorizontally(tween(TransitionMillis)) { width -> offset * width } +
                            fadeIn(tween(TransitionMillis))
                        ) togetherWith (
                        slideOutHorizontally(tween(TransitionMillis)) { width -> -offset * width } +
                            fadeOut(tween(TransitionMillis))
                        )
                },
                label = "portfolio-destination",
            ) { destination ->
                when (destination) {
                    PortfolioDestination.Overview -> OverviewScreen(
                        state = state,
                        onAction = viewModel::onAction,
                    )

                    is PortfolioDestination.Detail -> HoldingDetailScreen(
                        holding = state.selectedHolding,
                        scrubIndex = state.scrubIndex,
                        onAction = viewModel::onAction,
                    )
                }
            }

            if (state.isAddSheetVisible) {
                AddHoldingSheet(
                    onDismiss = { viewModel.onAction(PortfolioAction.AddDismissed) },
                    onConfirm = viewModel::onAction,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private const val TransitionMillis = 320
