---
description: All use cases must extend UseCase<PARAMS, RESULT> or FlowUseCase<PARAMS, RESULT> from core-domain, live in the domain sub-module, and be public.
paths:
  - "features/**/domain/**/*.kt"
  - "shared/**/domain/**/*.kt"
  - "sdks/**/domain/**/*.kt"
---

# Use Case Pattern

## Rule

All use cases must extend one of the two base classes in `dev.madina.tickr.core.domain.usecase` and implement its `execute(parameters: PARAMS)` method. Both enforce dispatcher injection and a uniform call interface across the codebase.

- **`UseCase<PARAMS, RESULT>`** for one-shot work. `execute` returns `RESULT` and throws on failure; the base class moves the work onto the injected dispatcher and wraps the outcome in `Result<RESULT>`.
- **`FlowUseCase<PARAMS, RESULT>`** for work that produces a stream. `execute` returns `Flow<RESULT>` and the base class applies `flowOn` with the injected dispatcher.

`FlowUseCase` deliberately does **not** wrap emissions in `Result`. A stream that fails is not the same as a value that fails: the failure is a single terminal event, so making every element a `Result` would force collectors to unwrap on every tick to handle something that can happen once. Collectors use `catch`.

## What to Check

### MUST BLOCK

- A class that represents business logic (named `*UseCase`) but does NOT extend `UseCase<PARAMS, RESULT>` or `FlowUseCase<PARAMS, RESULT>`
  - **Fix**: Extend the one matching its shape (one-shot vs stream), accept a `CoroutineDispatcher` as a constructor parameter, and move the business logic body into `execute(parameters: PARAMS)`
- A use case that returns `Result<RESULT>` from `execute()` — the base class wraps the return value automatically
  - **Fix**: Return `RESULT` directly from `execute()` and throw exceptions on failure; the base class `invoke` catches `Throwable` and wraps it in `Result.failure`
- A use case defined outside the `domain/` sub-module (i.e., in `data/`, `ui/`, or `di/`)
  - **Fix**: Move the class to `features/<name>/domain/src/commonMain/kotlin/` or `shared/shared-domain/src/commonMain/kotlin/`

### MUST FLAG

- A use case that overrides `invoke(parameters: PARAMS)` directly instead of implementing `execute(parameters: PARAMS)`
  - **Fix**: Override `execute()` only. The base class `invoke` handles dispatcher switching and `Result` wrapping — do not duplicate that logic
- A use case that accepts a parameter object but uses `Unit` as `PARAMS` when it does need input
  - **Fix**: Define a dedicated parameters data class (e.g., `GetUserProfileUseCase.Params`) and use it as the `PARAMS` type
- A use case that takes no parameters but declares a custom type as `PARAMS` instead of `Unit`
  - **Fix**: Use `UseCase<Unit, RESULT>`. The extension function `suspend operator fun <R> UseCase<Unit, R>.invoke()` in `UseCase.kt` allows callers to omit the argument entirely — no parameters wrapper needed
- A use case that is `internal` — use cases are consumed by ViewModels in the sibling `ui/` sub-module and must be `public`
  - **Acceptable**: A use case that is only used within the same `domain/` module (rare) may be `internal`, but this must be explicitly justified
- A use case that injects a `CoroutineDispatcher` using a hardcoded value (e.g., `Dispatchers.Default` passed at the call site) rather than injecting it via Koin
  - **Fix**: In the `di/` module, inject the dispatcher from `DispatcherProvider` (from `core-common`) so the test dispatcher can be substituted in tests

## Common Mistakes

- Writing `execute()` to catch exceptions internally and return `Result<RESULT>` — the base class already catches `Throwable` in `invoke()` and wraps it; double-wrapping produces `Result<Result<RESULT>>`
- Forgetting that no-parameter use cases have an extension `invoke()` — callers do not need to pass `Unit` explicitly; `myUseCase()` works directly
- Defining a parameters wrapper as a top-level class when it should be a nested `data class` inside the use case — nested placement keeps the API surface clean and the parameter type clearly scoped
- Placing domain logic directly in a ViewModel `init` block or `launch` instead of a use case — orchestration logic belongs in `execute()`, not in the UI layer
- Making the `CoroutineDispatcher` parameter default to `Dispatchers.Default` in the constructor — use a no-default constructor and inject via Koin so the test dispatcher can always be substituted

## Examples

### Good

```kotlin
// features/feature-profile/domain/src/commonMain/kotlin/com/example/app/feature/profile/domain/usecase/GetUserProfileUseCase.kt

// No-parameter use case: use Unit as PARAMS. Callers invoke with myUseCase() — no Unit needed.
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<Unit, UserProfile>(coroutineDispatcher) {

    override suspend fun execute(parameters: Unit): UserProfile =
        userRepository.getCurrentUserProfile()
}
```

```kotlin
// features/feature-search/domain/src/commonMain/kotlin/com/example/app/feature/search/domain/usecase/SearchProductsUseCase.kt

// Use case that requires input: nest the params as a data class inside the use case.
class SearchProductsUseCase(
    private val productRepository: ProductRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<SearchProductsUseCase.Params, List<Product>>(coroutineDispatcher) {

    data class Params(val query: String, val categoryId: String?)

    override suspend fun execute(parameters: Params): List<Product> =
        productRepository.search(parameters.query, parameters.categoryId)
}
```

```kotlin
// features/feature-profile/di/src/commonMain/kotlin/com/example/app/feature/profile/di/ProfileModule.kt

// Wiring in the DI module — inject the background dispatcher from DispatcherProvider.
val profileDomainModule = module {
    factory { GetUserProfileUseCase(get(), get<DispatcherProvider>().default) }
    factory { SearchProductsUseCase(get(), get<DispatcherProvider>().default) }
}
```

### Bad

```kotlin
// BAD: does not extend UseCase — no dispatcher injection, no Result wrapping, not testable uniformly
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(): UserProfile =
        userRepository.getCurrentUserProfile()
}
```

```kotlin
// BAD: overrides invoke() instead of execute() — bypasses the base class dispatcher and Result wrapping
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<Unit, UserProfile>(dispatcher) {

    override suspend fun invoke(parameters: Unit): Result<UserProfile> =
        Result.success(userRepository.getCurrentUserProfile()) // double-wraps Result
}
```

```kotlin
// BAD: execute() returns Result<UserProfile> — double-wraps because base class invoke() wraps again
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<Unit, Result<UserProfile>>(dispatcher) {

    override suspend fun execute(parameters: Unit): Result<UserProfile> =
        runCatching { userRepository.getCurrentUserProfile() } // use Result<T> as RESULT type is wrong
}
```

```kotlin
// BAD: use case defined in the ui/ sub-module — violates clean architecture layer boundaries
// features/feature-profile/ui/src/commonMain/kotlin/com/example/app/feature/profile/ui/GetUserProfileUseCase.kt
class GetUserProfileUseCase(...) : UseCase<Unit, UserProfile>(dispatcher) { ... }
```

## Severity

- `🚫 Blocking` — Use case does not extend `UseCase<PARAMS, RESULT>` from `dev.madina.tickr.core.domain.usecase`
- `🚫 Blocking` — Use case defined outside the `domain/` sub-module
- `🚫 Blocking` — `execute()` returns `Result<RESULT>` (causes double-wrapping at call sites)
- `⚠️ Change requested` — `invoke()` overridden instead of `execute()`
- `⚠️ Change requested` — Use case is `internal` without explicit justification
- `⚠️ Change requested` — `CoroutineDispatcher` hardcoded at the use-case constructor instead of injected via Koin
- `💡 Suggestion` — Parameters defined as a top-level class instead of a nested `data class` inside the use case
