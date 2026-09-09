---
description: The lambda passed to updateState {} must be a pure state transformation — no side effects, coroutine launches, effect emissions, or I/O inside the lambda
paths:
  - "sharedLib/src/**/*ViewModel*.kt"
  - "features/*/ui/**/*ViewModel*.kt"
  - "shared/*/ui/**/*ViewModel*.kt"
---

# updateState Purity

## Rule

`BaseViewModel.updateState { }` delegates to `MutableStateFlow.update { }`, which guarantees atomic state replacement. To achieve this atomicity, `MutableStateFlow.update` may retry the lambda multiple times under contention — calling the lambda again with the latest state value when a concurrent update is detected between reading the old state and writing the new one.

This retry behaviour means the lambda **must be a pure function**: it takes the old state as input and returns a new state. It must have no observable side effects. A side-effecting lambda that is called twice will emit two effects, launch two coroutines, or log twice.

**Allowed inside the lambda**: field access, `copy()`, arithmetic, string operations, conditional expressions — anything that derives the new state purely from the old state.

**Forbidden inside the lambda**: `emitEffect()`, `viewModelScope.launch { }`, repository calls, I/O, logging, and any mutation of external mutable state.

## What to Check

### MUST BLOCK

- `emitEffect(...)` called inside an `updateState { }` lambda
  - **Fix**: Call `emitEffect(...)` immediately after `updateState { }`, in the same `suspend` function, outside the lambda
- `viewModelScope.launch { }` called inside an `updateState { }` lambda
  - **Fix**: Move the `launch` outside the lambda; sequence it after the `updateState` call
- `Logger.withTag(...).d { }` / `log.e { }` / any logging call inside an `updateState { }` lambda
  - **Fix**: Remove the log; `BaseViewModel` already logs every new state automatically via `updateState`'s `.also { newState -> log.d { "[STATE] $newState" } }` wrapper

### MUST FLAG

- A repository or use case `suspend` function called from inside an `updateState { }` lambda
  - **Fix**: Call the repository/use case before `updateState`, capture the result in a local variable, and derive the new state from that local variable inside the lambda
- Mutation of an external `MutableList`, `MutableMap`, or `MutableStateFlow` inside an `updateState { }` lambda
  - **Fix**: Build the new value outside the lambda and close over it as a `val` inside
- `.also { }` or `.apply { }` blocks on the returned state that perform side effects inside the lambda
  - **Fix**: Move the side effect after the `updateState { }` call, outside the lambda

## Common Mistakes

- Calling `emitEffect` inside `updateState` because "it reads better" as a combined state-and-effect update — the effect may fire twice under contention; always sequence effects after state
- Launching a coroutine inside `updateState` to "kick off the next step" immediately — the coroutine may launch twice, causing duplicate network requests or duplicate navigation events
- Logging `"Setting state to X"` inside the lambda for debugging — `BaseViewModel` already logs every state transition; adding a log inside the lambda produces duplicate or out-of-order log lines when the lambda is retried
- Calling `copy()` and then calling a function on the result inside the lambda: `updateState { it.copy(items = loadItems()) }` — `loadItems()` is called inside the lambda and will be called again on retry

## Examples

### Good

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeViewModel.kt
// GOOD: All side effects are sequenced AFTER updateState — not inside the lambda.
internal class HomeViewModel(
    private val getItemsUseCase: GetItemsUseCase,
) : BaseViewModel<HomeAction, HomeEffect, HomeState>(HomeState.Loading) {

    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.Load -> loadItems()
            is HomeAction.SelectItem -> selectItem(action.itemId)
        }
    }

    private suspend fun loadItems() {
        getItemsUseCase()
            .onSuccess { items ->
                // GOOD: pure copy() inside lambda — no side effects
                updateState { it.copy(items = items.map { item -> item.toUi() }, isLoading = false) }
                // GOOD: emitEffect called AFTER updateState, not inside the lambda
                emitEffect(HomeEffect.ScrollToTop)
            }
            .onFailure { error ->
                updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) }
            }
    }

    private suspend fun selectItem(itemId: String) {
        // GOOD: state updated first, navigation effect emitted after
        updateState { it.copy(selectedItemId = itemId) }
        emitEffect(HomeEffect.NavigateToDetail(itemId))
    }
}
```

### Bad

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeViewModel.kt
// BAD: Logging inside updateState lambda — BaseViewModel already logs state changes;
// this produces a duplicate log line when the lambda executes, and a second duplicate
// if the lambda is retried under contention.
internal class HomeViewModel(
    private val getItemsUseCase: GetItemsUseCase,
) : BaseViewModel<HomeAction, HomeEffect, HomeState>(HomeState.Loading) {

    override suspend fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.SelectItem -> {
                updateState { oldState ->
                    log.d { "Selecting item ${action.itemId}" } // violation — side effect inside lambda
                    oldState.copy(selectedItemId = action.itemId)
                }
            }
        }
    }
}
```

```kotlin
// BAD: Repository call inside updateState lambda — suspend call inside a non-suspend
// lambda context; even if it compiled, the call would execute again on retry.
private fun refreshCount() {
    updateState { oldState ->
        val newCount = counterRepository.getCount() // violation — I/O inside lambda
        oldState.copy(count = newCount)
    }
}
```

```kotlin
// BAD: Logging inside updateState lambda — BaseViewModel already logs state changes;
// this produces a duplicate log line and adds noise when the lambda retries.
private suspend fun markLoading() {
    updateState { oldState ->
        log.d { "Setting loading = true" } // violation — side effect inside lambda
        oldState.copy(isLoading = true)
    }
}
```

```kotlin
// BAD: Launch inside updateState lambda — coroutine may start twice under contention,
// causing a duplicate network request.
private fun onRefreshClicked() {
    updateState { oldState ->
        viewModelScope.launch { fetchItems() } // violation — coroutine launch inside lambda
        oldState.copy(isLoading = true)
    }
}
```

## Severity

- `🚫 Blocking` — `emitEffect()` called inside an `updateState { }` lambda
- `🚫 Blocking` — `viewModelScope.launch { }` called inside an `updateState { }` lambda
- `🚫 Blocking` — Any logging call inside an `updateState { }` lambda (produces duplicate logs and noise on retry)
- `⚠️ Change requested` — Repository, data source, or use case call inside an `updateState { }` lambda
- `⚠️ Change requested` — Mutation of external mutable state inside an `updateState { }` lambda
- `💡 Suggestion` — `.also { }` or `.apply { }` on the returned state that performs a side effect inside the lambda
