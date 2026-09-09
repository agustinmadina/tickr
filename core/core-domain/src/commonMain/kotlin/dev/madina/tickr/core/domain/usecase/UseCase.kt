package dev.madina.tickr.core.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

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
            runCatching { execute(parameters) }
        }
}

/** Lets a no-argument use case be called as `useCase()` rather than `useCase(Unit)`. */
suspend operator fun <RESULT> UseCase<Unit, RESULT>.invoke(): Result<RESULT> = invoke(Unit)
