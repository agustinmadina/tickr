package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.UseCase
import dev.madina.tickr.feature.portfolio.domain.model.Holding
import dev.madina.tickr.feature.portfolio.domain.repository.HoldingsRepository
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Adds a position, rejecting the inputs that would make the portfolio arithmetic meaningless rather
 * than storing them and producing a nonsensical total later.
 */
class AddHoldingUseCase(
    private val holdingsRepository: HoldingsRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<AddHoldingUseCase.Params, Unit>(coroutineDispatcher) {

    data class Params(
        val symbol: String,
        val name: String,
        val quantity: Double,
        val averageCost: Double,
    )

    override suspend fun execute(parameters: Params) {
        val symbol = parameters.symbol.trim().uppercase()
        require(symbol.isNotEmpty()) { "Symbol cannot be blank" }
        require(parameters.quantity > 0) { "Quantity must be greater than zero" }
        require(parameters.averageCost >= 0) { "Average cost cannot be negative" }

        holdingsRepository.addHolding(
            Holding(
                symbol = symbol,
                name = parameters.name.trim().ifEmpty { symbol },
                quantity = parameters.quantity,
                averageCost = parameters.averageCost,
            ),
        )
    }
}
