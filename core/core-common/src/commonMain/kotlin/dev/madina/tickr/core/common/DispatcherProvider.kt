package dev.madina.tickr.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatchers reach the rest of the app through this rather than being referenced directly, so a
 * test can substitute a deterministic dispatcher without the code under test knowing.
 *
 * There is no `io` here on purpose: `Dispatchers.IO` is JVM-only and does not compile for iOS or
 * wasm, and this project has to build for all three.
 */
interface DispatcherProvider {
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
