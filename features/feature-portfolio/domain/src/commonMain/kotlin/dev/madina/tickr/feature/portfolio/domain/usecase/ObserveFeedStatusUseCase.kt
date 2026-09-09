package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.FlowUseCase
import dev.madina.tickr.feature.portfolio.domain.model.FeedStatus
import dev.madina.tickr.feature.portfolio.domain.repository.PriceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether prices are arriving, as a stream the screen can show.
 *
 * A second concern from the portfolio itself, so a second stream rather than a `combine` in the
 * ViewModel: the two change at completely different rates, and merging them would recompute the
 * whole portfolio every time the socket blinked.
 */
class ObserveFeedStatusUseCase(
    private val priceRepository: PriceRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : FlowUseCase<Unit, FeedStatus>(coroutineDispatcher) {
    override fun execute(parameters: Unit): Flow<FeedStatus> =
        priceRepository.observeStatus().distinctUntilChanged()
}
