package dev.madina.tickr.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One state holder, one entry point for input, one channel for things that happen once.
 *
 * [updateState] is the only way to mutate state, which keeps every change atomic and stops a
 * partial `copy()` from resetting unrelated fields. The reducer **must be pure**: `update` may
 * re-run it under contention, so a lambda that emits an effect or launches a coroutine would do it
 * twice.
 *
 * Effects are a `MutableSharedFlow` with no replay rather than a `StateFlow`, because a navigation
 * event or a message must not be redelivered when the screen is recreated.
 */
abstract class BaseViewModel<ACTION, EFFECT, STATE>(
    initialState: STATE,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = mutableState.asStateFlow()

    private val mutableEffects =
        MutableSharedFlow<EFFECT>(
            replay = 0,
            extraBufferCapacity = EffectBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val effects: Flow<EFFECT> = mutableEffects.asSharedFlow()

    protected val currentState: STATE get() = mutableState.value

    abstract fun onAction(action: ACTION)

    protected fun updateState(reducer: (STATE) -> STATE) {
        mutableState.update(reducer)
    }

    protected fun emitEffect(effect: EFFECT) {
        viewModelScope.launch { mutableEffects.emit(effect) }
    }
}

private const val EffectBufferCapacity = 8
