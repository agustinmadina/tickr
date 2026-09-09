---
description: Prefer .launchIn(viewModelScope) over viewModelScope.launch { flow.collect { } } for ongoing Flow observation in ViewModels — code after collect may silently not execute when an exception is thrown
paths:
  - "sharedLib/src/**/*ViewModel*.kt"
  - "features/*/ui/**/*ViewModel*.kt"
  - "shared/*/ui/**/*ViewModel*.kt"
---

# ViewModel Flow Collection Pattern

## Rule

ViewModels that observe ongoing Flows must use `.onEach { }.launchIn(viewModelScope)` rather than `viewModelScope.launch { flow.collect { } }`. When `collect` throws an uncaught exception the enclosing `launch` block crashes and any statement after the `collect` call is never reached — a silent, hard-to-detect bug. `.launchIn()` makes the observation the terminal operation and eliminates the ambiguity entirely.

## What to Check

### MUST BLOCK

- None — the Kotlin compiler does not prevent either pattern. Enforcement is at review time.

### MUST FLAG

- `viewModelScope.launch { someFlow.collect { ... }; doSomethingAfter() }` — code after `collect` that may never execute if the collect block throws
  - **Fix**: Move the post-collect work into a separate `launch` or, if it must run after the flow completes, use a finite flow and accept the `launch { collect {} }` form with an explicit comment (see Exceptions below)
- `viewModelScope.launch { someFlow.collect { ... } }` for ongoing state observation (hot flows, `StateFlow`, `SharedFlow`, `stateIn`, `shareIn`)
  - **Fix**: Replace with `.onEach { ... }.launchIn(viewModelScope)`
- A Flow chain without `.catch { }` when using `.launchIn()` — an uncaught exception in the upstream will cancel the job silently
  - **Fix**: Always pair `.launchIn()` with `.catch { }` between `.onEach { }` and `.launchIn()`, or use `.onEach { }.catch { }.launchIn(viewModelScope)`

## Exceptions

`viewModelScope.launch { flow.collect { } }` is acceptable when:
- The flow is **finite** (e.g., a one-shot `flow { emit(…) }` that completes) and work must execute **after** the flow completes in the same coroutine
- The calling code explicitly comments why `launch { collect {} }` is used instead of `launchIn`

In these cases the pattern must still protect against exceptions in the collect block — use a `try/catch` around the `collect` call if the body can throw.

## Common Mistakes

- Wrapping a `StateFlow` or `SharedFlow` observation in `launch { collect { } }` — these flows never complete, so there is no valid reason for code after `collect` in the same `launch` block
- Omitting `.catch { }` after converting to `.launchIn()` — the safety improvement disappears if upstream exceptions are left unhandled and silently cancel the observation job
- Using `viewModelScope.launch { someFlow.collect { updateState(it) }; _loaded.value = true }` to signal load completion — `_loaded.value = true` will never be reached if `updateState` throws or if the flow itself emits an error
- Chaining multiple `.collect { }` calls in the same `launch` block — each collect suspends until the flow completes, so the second collect never starts if the first flow is infinite

## Examples

### Good

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/com/example/app/feature/home/ui/HomeViewModel.kt

internal class HomeViewModel(
    private val observeItemsUseCase: ObserveItemsUseCase,
    private val observeUserUseCase: ObserveUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // GOOD: .onEach { }.catch { }.launchIn(viewModelScope) — the observation is the
        // terminal operation. An exception in onEach is caught before it can cancel the job.
        observeItemsUseCase()
            .onEach { items -> _uiState.value = HomeUiState.Content(items) }
            .catch { error -> _uiState.value = HomeUiState.Error(error.message.orEmpty()) }
            .launchIn(viewModelScope)

        // GOOD: Multiple independent flows each get their own launchIn — they are observed
        // concurrently and independently; an error in one does not cancel the other.
        observeUserUseCase()
            .onEach { user -> _userName.value = user.displayName }
            .catch { /* handle */ }
            .launchIn(viewModelScope)
    }
}
```

```kotlin
// features/feature-checkout/ui/src/commonMain/kotlin/com/example/app/feature/checkout/ui/CheckoutViewModel.kt

internal class CheckoutViewModel(
    private val placeOrderUseCase: PlaceOrderUseCase,
) : ViewModel() {

    fun onConfirmClicked(cartId: String) {
        // GOOD: launch { collect {} } is acceptable here because the flow is finite
        // (a one-shot operation that emits a result and completes) and we need to act
        // after collection. This is explicitly commented to justify the pattern.
        viewModelScope.launch {
            // placeOrderUseCase returns a finite flow that emits one Result and completes.
            placeOrderUseCase(cartId).collect { result ->
                result
                    .onSuccess { _navEvent.value = NavEvent.OrderConfirmation }
                    .onFailure { _uiState.value = CheckoutUiState.Error(it.message.orEmpty()) }
            }
            // Code after collect is valid here — the flow above is finite and terminates.
            analytics.track("checkout_attempted")
        }
    }
}
```

### Bad

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/com/example/app/feature/home/ui/HomeViewModel.kt

internal class HomeViewModel(
    private val observeItemsUseCase: ObserveItemsUseCase,
) : ViewModel() {

    init {
        // BAD: observeItemsUseCase() is an infinite StateFlow-backed stream.
        // If _uiState.value = HomeUiState.Content(items) throws, the coroutine crashes.
        // Any code placed after this collect call would never execute — silently.
        viewModelScope.launch {
            observeItemsUseCase().collect { items ->
                _uiState.value = HomeUiState.Content(items)
            }
            // This line is unreachable in practice — the flow above never completes.
            _loadComplete.value = true // BAD: never reached
        }
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/com/example/app/feature/profile/ui/ProfileViewModel.kt

internal class ProfileViewModel(
    private val observeProfileUseCase: ObserveProfileUseCase,
    private val observeSettingsUseCase: ObserveSettingsUseCase,
) : ViewModel() {

    init {
        // BAD: Two infinite flows chained in a single launch block.
        // observeSettingsUseCase().collect { } will NEVER be reached because
        // observeProfileUseCase().collect { } suspends indefinitely (the flow is infinite).
        viewModelScope.launch {
            observeProfileUseCase().collect { _profile.value = it }  // suspends forever
            observeSettingsUseCase().collect { _settings.value = it } // never reached
        }
    }
}
```

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/com/example/app/feature/home/ui/HomeViewModel.kt

internal class HomeViewModel(
    private val observeItemsUseCase: ObserveItemsUseCase,
) : ViewModel() {

    init {
        // BAD: .launchIn() without .catch { } — an exception in onEach silently cancels
        // the observation job, leaving uiState stuck in Loading with no error shown.
        observeItemsUseCase()
            .onEach { items -> _uiState.value = HomeUiState.Content(items) }
            .launchIn(viewModelScope) // missing .catch { }
    }
}
```

## Severity

- `⚠️ Change requested` — `viewModelScope.launch { flow.collect { ... }; codeAfterCollect() }` where code after `collect` may never execute due to exception or infinite flow
- `⚠️ Change requested` — `.launchIn(viewModelScope)` without `.catch { }` for flows that can emit errors (most production flows)
- `💡 Suggestion` — `viewModelScope.launch { infiniteFlow.collect { ... } }` for ongoing state observation that has no code after collect (correct but prefer `.launchIn()` for clarity and consistency)
