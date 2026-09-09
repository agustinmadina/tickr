package dev.madina.tickr.feature.portfolio.ui

import androidx.lifecycle.viewModelScope
import dev.madina.tickr.core.ui.mvi.BaseViewModel
import dev.madina.tickr.feature.portfolio.domain.usecase.AddHoldingUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.ObservePortfolioUseCase
import dev.madina.tickr.feature.portfolio.domain.usecase.RemoveHoldingUseCase
import dev.madina.tickr.feature.portfolio.ui.mapper.toUi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class PortfolioViewModel(
    private val observePortfolio: ObservePortfolioUseCase,
    private val addHolding: AddHoldingUseCase,
    private val removeHolding: RemoveHoldingUseCase,
) : BaseViewModel<PortfolioAction, PortfolioEffect, PortfolioUiState>(PortfolioUiState()) {

    init {
        observePortfolio(Unit)
            .onEach { portfolio ->
                updateState { previous ->
                    previous.copy(
                        // The previous holdings carry the price history, so the reducer derives the
                        // new samples from the old state rather than from a field, keeping it pure.
                        holdings = portfolio.toUi(previous.holdings.associate { it.symbol to it.history }),
                        totalValue = portfolio.totalValue,
                        totalProfit = portfolio.totalProfit,
                        totalReturnPercent = portfolio.totalReturnPercent,
                        isPartiallyPriced = portfolio.isPartiallyPriced,
                        isLoading = false,
                    )
                }
            }
            .catch { throwable ->
                updateState { it.copy(isLoading = false, errorMessage = throwable.readableMessage()) }
            }
            // launchIn, not launch { collect { } }: with collect, an exception kills the enclosing
            // coroutine and anything after it never runs, silently.
            .launchIn(viewModelScope)
    }

    override fun onAction(action: PortfolioAction) {
        when (action) {
            is PortfolioAction.HoldingClicked ->
                updateState { it.copy(destination = PortfolioDestination.Detail(action.symbol)) }

            PortfolioAction.BackClicked ->
                updateState { it.copy(destination = PortfolioDestination.Overview) }

            PortfolioAction.AddClicked ->
                updateState { it.copy(isAddSheetVisible = true) }

            PortfolioAction.AddDismissed ->
                updateState { it.copy(isAddSheetVisible = false) }

            is PortfolioAction.AddConfirmed -> confirmAdd(action)

            is PortfolioAction.RemoveClicked -> remove(action.symbol)

            PortfolioAction.ErrorDismissed ->
                updateState { it.copy(errorMessage = null) }
        }
    }

    private fun confirmAdd(action: PortfolioAction.AddConfirmed) {
        val quantity = action.quantity.trim().toDoubleOrNull()
        val averageCost = action.averageCost.trim().toDoubleOrNull()
        if (quantity == null || averageCost == null) {
            emitEffect(PortfolioEffect.ShowMessage("Quantity and average cost must be numbers"))
            return
        }

        viewModelScope.launch {
            addHolding(
                AddHoldingUseCase.Params(
                    symbol = action.symbol,
                    name = action.name,
                    quantity = quantity,
                    averageCost = averageCost,
                ),
            ).onSuccess {
                updateState { it.copy(isAddSheetVisible = false) }
            }.onFailure { throwable ->
                emitEffect(PortfolioEffect.ShowMessage(throwable.readableMessage()))
            }
        }
    }

    private fun remove(symbol: String) {
        viewModelScope.launch {
            removeHolding(RemoveHoldingUseCase.Params(symbol)).onSuccess {
                // Leaving the detail open would show a position that no longer exists.
                updateState { it.copy(destination = PortfolioDestination.Overview) }
            }.onFailure { throwable ->
                emitEffect(PortfolioEffect.ShowMessage(throwable.readableMessage()))
            }
        }
    }
}

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
