package dev.madina.tickr.feature.portfolio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.madina.tickr.core.ui.theme.Spacing
import dev.madina.tickr.core.ui.theme.TextSecondary
import org.koin.compose.viewmodel.koinViewModel

/**
 * Host for the feature.
 *
 * Two layouts from one state. On a phone the destination in [PortfolioUiState] drives an
 * `AnimatedContent` that swaps whole screens; past [WideBreakpoint] the same destination instead
 * decides which asset the second pane shows, and nothing navigates. This is why the destination
 * lives in the ViewModel rather than in a navigation library's back stack: the wide layout is not
 * a different navigation graph, it is the same state drawn differently.
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= WideBreakpoint

            Box(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    TwoPaneLayout(state = state, onAction = viewModel::onAction)
                } else {
                    SinglePaneLayout(state = state, onAction = viewModel::onAction)
                }

                if (state.isAddSheetVisible) {
                    AddHoldingSheet(state = state, onAction = viewModel::onAction)
                }

                if (state.isAboutVisible) {
                    AboutSheet(onDismiss = { viewModel.onAction(PortfolioAction.AboutDismissed) })
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SinglePaneLayout(
    state: PortfolioUiState,
    onAction: (PortfolioAction) -> Unit,
) {
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
            PortfolioDestination.Overview -> OverviewScreen(state = state, onAction = onAction)

            is PortfolioDestination.Detail ->
                HoldingDetailScreen(
                    holding = state.selectedHolding,
                    scrubIndex = state.scrubIndex,
                    onAction = onAction,
                )
        }
    }
}

@Composable
private fun TwoPaneLayout(
    state: PortfolioUiState,
    onAction: (PortfolioAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        OverviewScreen(
            state = state,
            onAction = onAction,
            modifier = Modifier.weight(ListPaneWeight).fillMaxHeight(),
            selectedSymbol = state.selectedHolding?.symbol,
        )

        Box(modifier = Modifier.weight(DetailPaneWeight).fillMaxHeight()) {
            if (state.selectedHolding == null) {
                EmptyDetailPane()
            } else {
                HoldingDetailScreen(
                    holding = state.selectedHolding,
                    scrubIndex = state.scrubIndex,
                    onAction = onAction,
                    // Nothing to go back to: the list is right there on the left.
                    showBack = false,
                )
            }
        }
    }
}

@Composable
private fun EmptyDetailPane() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Pick an asset",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Its chart and figures appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

private const val TransitionMillis = 320

/** Material's expanded width class: below it two panes leave neither one usable. */
private val WideBreakpoint = 840.dp
private const val ListPaneWeight = 1f
private const val DetailPaneWeight = 1.1f
