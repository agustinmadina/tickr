---
description: ViewModels in feature ui/ sub-modules must be declared internal — they are UI-layer implementation details wired by Koin in the di/ aggregator, never referenced directly by external modules
paths:
  - "shared/*/ui/**/*.kt"
  - "features/*/ui/**/*.kt"
---

# ViewModel Visibility

## Rule

Every ViewModel class in a feature `ui/` sub-module must be declared `internal`. ViewModels are UI-layer implementation details; no module outside `ui/` ever needs to reference the concrete ViewModel type. The `di/` aggregator registers them via Koin and Compose screens inject them via `koinViewModel<T>()` — both call sites are inside modules that already have access to `internal` declarations.

See also: `visibility-modifiers.md` for the full module-level visibility policy.

## What to Check

### MUST BLOCK

- None — the Kotlin compiler does not enforce this at build time (a missing `internal` modifier compiles successfully). Enforcement is at review time.

### MUST FLAG

- A ViewModel class in `features/*/ui/` without an explicit `internal` modifier — Kotlin defaults to `public`, which leaks the concrete type as an importable dependency for any module in the project
  - **Fix**: Add `internal` to the class declaration. No call sites outside the `ui/` sub-module exist that would break, because the `di/` aggregator and Compose screens both have access to `internal` via the Gradle dependency graph (see below).
- A ViewModel that is `public` and imported directly by the `di/` sub-module without going through a Koin registration — this means external code is holding a direct type reference to the ViewModel rather than routing through the Koin graph
  - **Fix**: Register the ViewModel via `viewModel { FooViewModel(get()) }` in the Koin module and remove the direct import from any non-`ui/` consumer.

## Why `internal` Works With Koin

The 4-sub-module structure makes `internal` ViewModels safe across the board:

- **Compose screens** (`features/*/ui/`) call `koinViewModel<FooViewModel>()` — this is in the **same** Gradle module as `FooViewModel`, so `internal` is fully accessible.
- **Koin registration** lives in `features/*/di/`, which declares a Gradle `implementation` dependency on `features/*/ui/`. Kotlin's `internal` visibility is scoped to a Gradle module boundary, so the `di/` sub-module can reference `FooViewModel` directly in `viewModel { FooViewModel(get()) }`.
- **No other module** (`domain/`, `data/`, sibling features, `sharedLib`) has a compile dependency on `features/*/ui/`, so there is no risk of an external module importing the concrete ViewModel type.

## Common Mistakes

- Omitting the `internal` modifier entirely — Kotlin's default visibility is `public`, so a ViewModel class declared as `class FooViewModel : ViewModel()` is silently public
- Adding `public` explicitly "for clarity" — this is wrong; it intentionally exports a type that has no valid external consumer
- Believing that Koin's `viewModel { }` DSL requires the ViewModel to be `public` — it does not; the `di/` module's compile dependency on `ui/` already grants `internal` access
- Declaring a `public` ViewModel so that a `SharedViewModel` pattern can be used across features — the correct approach is to lift shared state to a shared domain use case or a `shared-data` repository, not to expose the ViewModel type

## Examples

### Good

```kotlin
// features/feature-auth/ui/src/commonMain/kotlin/com/example/app/feature/auth/ui/LoginViewModel.kt
// internal: no module outside feature-auth:ui needs to reference this type directly.
internal class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            loginUseCase(LoginUseCase.Params(email, password))
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure { _uiState.value = LoginUiState.Error(it.message.orEmpty()) }
        }
    }
}

// features/feature-auth/ui/src/commonMain/kotlin/com/example/app/feature/auth/ui/LoginScreen.kt
// koinViewModel<T>() is called from within the same ui/ module — internal access is valid.
@Composable
internal fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}

// features/feature-auth/di/src/commonMain/kotlin/com/example/app/feature/auth/di/AuthModule.kt
// The di/ sub-module depends on :features:feature-auth:ui, so it can reference internal LoginViewModel.
val authModule = module {
    includes(authDataModule, authUiModule)
}

// features/feature-auth/ui/src/commonMain/kotlin/com/example/app/feature/auth/di/authUiModule.kt
// Koin module val is public (required for di/ aggregator includes()), but the ViewModel itself is internal.
val authUiModule = module {
    viewModel { LoginViewModel(get()) }
}
```

### Bad

```kotlin
// features/feature-auth/ui/src/commonMain/kotlin/com/example/app/feature/auth/ui/LoginViewModel.kt
// BAD: No explicit modifier — defaults to public. Any module in the project can now import
// LoginViewModel directly, creating an unintended compile dependency on a UI implementation detail.
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() { ... }

// Also bad: explicitly public — signals intent to expose, but there is no valid external consumer.
public class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() { ... }

// Also bad: another feature's screen importing LoginViewModel directly instead of
// using its own ViewModel. This is both a visibility violation and an architecture violation.
// (In another feature's ui/ module:)
import dev.madina.tickr.feature.auth.ui.LoginViewModel
@Composable
fun SomeOtherScreen() {
    val authVm = koinViewModel<LoginViewModel>() // wrong — crossing feature boundaries
}
```

## Severity

- `⚠️ Change requested` — ViewModel class in `features/*/ui/` without an explicit `internal` modifier (defaults to public, leaks concrete type)
- `⚠️ Change requested` — ViewModel imported directly by any module other than its own `ui/` sub-module
- `💡 Suggestion` — `public` ViewModel in a shared module (`shared/`) that could be narrowed to `internal` if it has no cross-module consumers
