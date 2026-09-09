package dev.madina.tickr.core.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

/**
 * Base class for business logic that produces a stream rather than a single value, which is most of
 * this app: prices arrive continuously and holdings change under the user.
 *
 * It does not wrap emissions in [Result] the way [UseCase] wraps its return value. A stream that
 * fails is not the same as a value that fails: the error is a terminal event on the flow, and
 * making every element a `Result` would force each collector to unwrap on every tick to handle a
 * condition that can only happen once. Collectors handle failure with `catch`.
 *
 * `flowOn` rather than wrapping the builder in `withContext`, so the dispatcher applies to
 * everything upstream and the collector keeps its own context.
 */
abstract class FlowUseCase<in PARAMS, out RESULT>(
    private val coroutineDispatcher: CoroutineDispatcher,
) {
    protected abstract fun execute(parameters: PARAMS): Flow<RESULT>

    operator fun invoke(parameters: PARAMS): Flow<RESULT> =
        execute(parameters).flowOn(coroutineDispatcher)
}

/** Lets a no-argument flow use case be called as `useCase()` rather than `useCase(Unit)`. */
operator fun <RESULT> FlowUseCase<Unit, RESULT>.invoke(): Flow<RESULT> = invoke(Unit)
