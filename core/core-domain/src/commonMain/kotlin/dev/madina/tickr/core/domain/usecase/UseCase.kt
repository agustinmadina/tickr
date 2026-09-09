package dev.madina.tickr.core.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Base class for a one-shot piece of business logic.
 *
 * Subclasses implement [execute] and throw on failure; this class moves the work off the caller's
 * dispatcher and wraps the outcome in a [Result], so no subclass repeats either concern and no
 * caller has to remember which of the two it is responsible for.
 */
abstract class UseCase<in PARAMS, out RESULT>(
    private val coroutineDispatcher: CoroutineDispatcher,
) {
    protected abstract suspend fun execute(parameters: PARAMS): RESULT

    suspend operator fun invoke(parameters: PARAMS): Result<RESULT> =
        withContext(coroutineDispatcher) {
            // runCatching catches Throwable, cancellation included, which would turn a caller
            // walking away into an ordinary failure the UI then reports as an error.
            runCatching { execute(parameters) }
                .onFailure { if (it is CancellationException) throw it }
        }
}

/** Lets a no-argument use case be called as `useCase()` rather than `useCase(Unit)`. */
suspend operator fun <RESULT> UseCase<Unit, RESULT>.invoke(): Result<RESULT> = invoke(Unit)
