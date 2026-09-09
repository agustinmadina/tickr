---
description: ViewModel state (BaseViewModel STATE) must be the single source of truth — all mutations go through updateState() and preserve existing fields via copy()
paths:
  - "sharedLib/src/**/*ViewModel*.kt"
  - "features/*/ui/**/*ViewModel*.kt"
  - "shared/*/ui/**/*ViewModel*.kt"
---

# ViewModel State Source of Truth

## Rule

`BaseViewModel<ACTION, EFFECT, STATE>` exposes a single `StateFlow<STATE>` via `val state`. All mutations to that state must go through `updateState { oldState -> oldState.copy(...) }`. This ensures:

1. State mutations are logged automatically (the base class logs every new state at debug level)
2. Mutations are atomic via `MutableStateFlow.update { }` which prevents race conditions
3. Unchanged fields are preserved — a partial update via `copy()` never accidentally resets unrelated fields to defaults

No secondary `MutableStateFlow` or `MutableLiveData` should exist alongside `BaseViewModel.state`. If a value needs to be tracked over time in a ViewModel, it belongs in the `STATE` data class.

## What to Check

### MUST BLOCK

- Direct assignment to `_state.value = ...` outside of `updateState { }` — `_state` is `private` in `BaseViewModel`, so this is only possible if a subclass declares its own `_state`
  - **Fix**: Remove the secondary `_state` field and use `updateState { }` from `BaseViewModel`
- `updateState { _ -> NewState(...) }` that ignores the old state entirely when `STATE` is a `data class` with multiple fields — replacing all fields loses any concurrently set values
  - **Fix**: Use `it.copy(fieldToChange = newValue)` to preserve all other fields

### MUST FLAG

- A secondary `MutableStateFlow<T>` declared alongside `BaseViewModel.state` that tracks UI-relevant data
  - **Fix**: Move the tracked value into the `STATE` data class as a property and update it via `updateState { it.copy(yourValue = newValue) }`
- Two consecutive `updateState { }` calls with no suspension point between them — the second call may overwrite changes from the first before the UI observes the intermediate state
  - **Fix**: Merge the two calls into a single `updateState { it.copy(field1 = v1, field2 = v2) }`
- Calling `submitAction` inside `handleAction` before state is updated — this enqueues a new action before the current action's state change is committed, which can cause ordering surprises
  - **Fix**: Update state first, then emit any derived actions or effects

## Common Mistakes

- Overriding or shadowing `_state` in a subclass — `BaseViewModel._state` is `private`, so a subclass that declares `private val _state = MutableStateFlow(...)` silently creates a second, independent flow that the `val state` property does not reflect
- Calling `updateState { NewState() }` without `copy()` when `STATE` has multiple fields — a loading flag set in one coroutine is reset to the default by another coroutine that replaces the full state object
- Using a separate `MutableStateFlow<Boolean>` for `isLoading` because "it is simpler" — it is not simpler; it creates two sources of truth that can diverge, and observers must combine two flows to reconstruct the full UI state
- Setting state with a hardcoded default via `updateState { _ -> ProfileState() }` to "reset" on logout — this is safe only if every field should return to its default; if any field (like a cached config) should survive the reset, `copy()` must be used explicitly

## Examples

### Good

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt
// GOOD: All state mutations go through updateState { it.copy(...) }.
// Unchanged fields (e.g., avatarUrl) are preserved when only displayName changes.
internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState()) {

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Load -> loadProfile()
            is ProfileAction.UpdateName -> updateName(action.newName)
        }
    }

    private suspend fun loadProfile() {
        updateState { it.copy(isLoading = true) }
        getUserProfileUseCase()
            .onSuccess { profile ->
                updateState { it.copy(isLoading = false, profile = profile.toUi()) }
            }
            .onFailure { error ->
                updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) }
            }
    }

    private suspend fun updateName(newName: String) {
        // GOOD: Single updateState merging both fields — no race between two separate calls.
        updateState { it.copy(isLoading = true, errorMessage = null) }
        updateDisplayNameUseCase(UpdateDisplayNameUseCase.Params(newName))
            .onSuccess { updateState { it.copy(isLoading = false) } }
            .onFailure { error -> updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) } }
    }
}
```

### Bad

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt
// BAD: Secondary MutableStateFlow alongside BaseViewModel.state — two sources of truth
// for loading and profile state that can diverge when updated independently.
internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState()) {

    // violation — secondary flow; isLoading belongs in ProfileState
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Load -> {
                _isLoading.value = true // violation — direct assignment outside updateState
                getUserProfileUseCase()
                    .onSuccess { profile ->
                        updateState { it.copy(profile = profile.toUi()) }
                        _isLoading.value = false // violation — secondary flow mutation
                    }
                    .onFailure { _isLoading.value = false } // violation
            }
        }
    }
}
```

```kotlin
// BAD: updateState ignores old state — any value set by a concurrent coroutine is lost.
// If a background refresh set isLoading = true just before this line runs,
// the result is an inconsistent state that shows stale data with no loading indicator.
internal class ProfileViewModel(...) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState()) {

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Reset -> {
                updateState { _ -> ProfileState() } // violation — ignores oldState; use copy()
            }
        }
    }
}
```

```kotlin
// BAD: Two consecutive updateState calls with no suspension — the second may overwrite
// the first before the UI collects the intermediate Loading state.
private suspend fun loadProfile() {
    updateState { it.copy(isLoading = true) }  // first update
    updateState { it.copy(profile = null) }    // violation — immediately overwrites; merge into one call
    // ... network call
}
```

## Severity

- `🚫 Blocking` — Direct mutation of a secondary `_state` MutableStateFlow in a ViewModel subclass (bypasses `BaseViewModel.updateState`)
- `🚫 Blocking` — `updateState { _ -> NewState(...) }` that replaces all fields on a multi-field `data class` state without using `copy()`, erasing concurrent state changes
- `⚠️ Change requested` — Secondary `MutableStateFlow` declared alongside `BaseViewModel.state` for UI-relevant data
- `⚠️ Change requested` — Two or more consecutive `updateState` calls with no suspension point between them
- `💡 Suggestion` — `updateState` call that could be merged with an adjacent `updateState` for clarity
