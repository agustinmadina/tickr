---
description: Result unwrapping methods (getOrThrow, getOrDefault, getOrNull) are only permitted inside UseCase.execute() — they must not appear in ViewModels, repositories, data sources, or Composables
paths:
  - "features/*/domain/**/*.kt"
  - "features/*/data/**/*.kt"
  - "features/*/ui/**/*.kt"
  - "shared/**/*.kt"
---

# Result Method Restrictions

## Rule

`getOrThrow()`, `getOrDefault()`, and `getOrNull()` are only permitted inside `UseCase.execute()` bodies. These methods either throw exceptions directly or silently discard failure information — both of which are dangerous outside the controlled context of a use case, where the base class `invoke()` catches `Throwable` and wraps it into `Result.failure`.

ViewModels and Composables must handle `Result<T>` using `onSuccess { }`, `onFailure { }`, or `fold()`. These methods make both the success and failure paths explicit and do not throw.

**Repositories and data sources**: per `repository-error-propagation.md`, repositories and data sources do not return `Result<T>` — they throw on failure, so `execute()` calls them directly with nothing to unwrap. The restrictions below still apply in full to the pre-existing `Result`-returning repositories that remain in the codebase (see the note at the end of this file).

## What to Check

### MUST BLOCK

- `getOrThrow()` called in a ViewModel `launch` block, `handleAction`, or `init`
  - **Fix**: Replace with `onSuccess { ... }.onFailure { ... }` and update state accordingly in each branch
- `getOrThrow()` called inside a repository implementation or data source
  - **Fix**: Per `repository-error-propagation.md`, repositories and data sources must throw on failure — they must not return `Result<T>` at all. A `getOrThrow()` inside a repository usually means it is unwrapping a `Result` from a pre-existing dependency; migrate that dependency to throw directly (a file under review is by definition being touched, which is when migration is due)
- `getOrThrow()` called inside a `@Composable` function
  - **Fix**: Composables must not perform `Result` unwrapping at all; pass already-resolved UI state from the ViewModel

### MUST FLAG

- `getOrDefault()` or `getOrNull()` used outside `UseCase.execute()` — these silently discard error information, creating invisible failure paths
  - **Fix**: Use `onFailure { }` to log or handle the error explicitly, then provide the default value; do not use `getOrNull() ?: default` as a one-liner that swallows failures
- `getOrElse { }` in a ViewModel that re-throws or returns a sentinel value
  - **Fix**: Use `fold(onSuccess = { ... }, onFailure = { ... })` to handle both branches with equal explicitness
- Deeply chained `getOrThrow()` calls within `UseCase.execute()` where a single failure mid-chain leaves the state undefined
  - **Fix**: Assign each intermediate result to a named `val` and call `getOrThrow()` on each individually, so the failure point is clear from the stack trace

## Common Mistakes

- Using `getOrThrow()` in a ViewModel `launch` block believing that `BaseViewModel`'s handleAction catch block will catch it — `BaseViewModel` only catches exceptions thrown from `handleAction`, not from coroutines launched manually with `viewModelScope.launch { }`
- Using `getOrNull()` to convert a failure to `null` and then checking for null downstream — this loses the original exception and makes it impossible to show the user an accurate error message
- Using `getOrDefault(emptyList())` in a ViewModel to avoid writing an error branch — the screen then silently shows an empty state when a network error occurred, with no way for the user to know a failure happened

## Examples

### Good

```kotlin
// features/feature-profile/domain/src/commonMain/kotlin/.../GetUserProfileUseCase.kt
// NOTE: this example calls pre-existing Result-returning repositories; NEW repositories
// throw instead, making getOrThrow() unnecessary — see repository-error-propagation.md.
// GOOD: getOrThrow() is used inside UseCase.execute() — the base class invoke() wraps
// any thrown exception into Result.failure before it reaches the ViewModel.
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<Unit, UserProfile>(coroutineDispatcher) {

    override suspend fun execute(parameters: Unit): UserProfile {
        val session = sessionRepository.getActiveSession().getOrThrow() // safe here
        return userRepository.getProfile(session.userId).getOrThrow()   // safe here
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt
// GOOD: ViewModel uses onSuccess/onFailure — both paths are handled explicitly.
internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState.Loading) {

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Load -> loadProfile()
        }
    }

    private suspend fun loadProfile() {
        getUserProfileUseCase()
            .onSuccess { profile ->
                updateState { it.copy(profile = profile.toUi(), isLoading = false) }
            }
            .onFailure { error ->
                updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) }
            }
    }
}
```

```kotlin
// features/feature-cart/data/src/commonMain/kotlin/.../CartRepositoryImpl.kt
// GOOD: Repository throws on failure — no Result<T>, no getOrThrow() in data layer.
// See repository-error-propagation.md for the full repository-throws convention.
internal class CartRepositoryImpl(
    private val cartApiClient: CartApiClient,
) : CartRepository {

    override suspend fun getCart(userId: String): Cart =
        cartApiClient.fetchCart(userId).toDomain() // no runCatching — exceptions propagate to UseCase.invoke()
}
```

### Bad

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt
// BAD: getOrThrow() in a ViewModel launch block — if getUserProfileUseCase() returns
// Result.failure, getOrThrow() throws an exception that is NOT caught by BaseViewModel's
// init block. The ViewModel's state is never updated and the app may crash.
internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState.Loading) {

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Load -> {
                val profile = getUserProfileUseCase().getOrThrow() // violation — throws in ViewModel
                updateState { it.copy(profile = profile.toUi()) }
            }
        }
    }
}
```

```kotlin
// features/feature-cart/data/src/commonMain/kotlin/.../CartRepositoryImpl.kt
// BAD: getOrThrow() unwraps a local data source's Result just to rewrap it via Result.success —
// classic double-wrapping. Per repository-error-propagation.md, CartRepository returning
// Result<Cart> here is itself the pre-rule shape for NEW code; both cartLocalDataSource and
// CartRepositoryImpl should throw directly, with no Result involved at all.
internal class CartRepositoryImpl(
    private val cartLocalDataSource: CartLocalDataSource,
    private val cartApiClient: CartApiClient,
) : CartRepository {

    override suspend fun getCart(userId: String): Result<Cart> {
        val cached = cartLocalDataSource.getCart(userId).getOrThrow() // violation
        return Result.success(cached)
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileScreen.kt
// BAD: getOrNull() inside a Composable — Composables must never unwrap Result.
// All resolution must happen in the ViewModel before the value reaches the Composable.
@Composable
internal fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profileResult.getOrNull() // violation — silent failure discard in Composable
    Text(profile?.displayName ?: "")
}
```

## Note on pre-existing repositories

Repositories predating the repository-throws convention still return `Result<T>`. The restrictions in this file apply to them unchanged — `getOrThrow()`/`getOrDefault()`/`getOrNull()` outside `UseCase.execute()` remain violations regardless of which repository shape a file uses. Such repositories migrate when next substantively touched; see `repository-error-propagation.md`.

## Severity

- `🚫 Blocking` — `getOrThrow()` called in a ViewModel, repository implementation, data source, or Composable
- `⚠️ Change requested` — `getOrDefault()` or `getOrNull()` used outside `UseCase.execute()` (silently discards failure information)
- `💡 Suggestion` — `getOrElse { }` in a ViewModel that produces a sentinel value without logging the underlying failure

See `repository-error-propagation.md` for the separate (and now primary) rule governing whether a repository should return `Result<T>` at all.
