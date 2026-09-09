package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.UseCase
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import kotlinx.coroutines.CoroutineDispatcher

class RemoveHoldingUseCase(
    private val holdingsRepository: HoldingsRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<RemoveHoldingUseCase.Params, Unit>(coroutineDispatcher) {
    data class Params(
        val symbol: String,
    )

    override suspend fun execute(parameters: Params) {
        holdingsRepository.removeHolding(parameters.symbol.trim().uppercase())
    }
}
