---
description: Use cases must be called with direct call syntax (useCase(params)) not explicit .invoke(params).
paths:
  - "features/**/ui/**/*.kt"
  - "features/**/domain/**/*.kt"
  - "shared/**/domain/**/*.kt"
  - "sdks/**/domain/**/*.kt"
---

# Use Case Invocation Syntax

## Rule

Use cases must be called using direct call syntax — `useCase(params)` — not the explicit `.invoke(params)` form. Kotlin's `operator fun invoke` is designed specifically so that callable objects can be invoked like functions; writing `.invoke()` explicitly defeats that purpose and adds noise with no benefit.

## What to Check

### MUST BLOCK

_No blocking violations — this is a style concern, not a correctness issue._

### MUST FLAG

- A call site that uses `useCase.invoke(params)` or `useCase.invoke()` in production code
  - **Fix**: Replace with `useCase(params)` or `useCase()` respectively. The semantics are identical; direct call syntax is the idiomatic Kotlin form

## Common Mistakes

- Writing `.invoke()` because it mirrors how the method is declared on the base class — `invoke` is declared as an `operator fun`, which by definition allows omitting `.invoke` at the call site
- Using `.invoke()` in test code for "clarity" — direct call syntax is equally clear and consistent with production code; prefer `useCase(params)` in tests as well
- Calling a no-parameter use case as `useCase.invoke(Unit)` or `useCase(Unit)` — the `UseCase.kt` extension `suspend operator fun <R> UseCase<Unit, R>.invoke()` means the correct call is simply `useCase()` with no argument at all

## Examples

### Good

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt

class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
) : ViewModel() {

    fun loadProfile() {
        viewModelScope.launch {
            // No-param use case: call with no arguments.
            val profile = getUserProfileUseCase()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            // Parameterised use case: pass the params object directly.
            val results = searchProductsUseCase(SearchProductsUseCase.Params(query, categoryId = null))
        }
    }
}
```

```kotlin
// features/feature-checkout/domain/src/commonMain/kotlin/.../PlaceOrderUseCase.kt

// Use cases that orchestrate other use cases also use direct call syntax.
class PlaceOrderUseCase(
    private val validateCartUseCase: ValidateCartUseCase,
    private val cartRepository: CartRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<PlaceOrderUseCase.Params, Order>(coroutineDispatcher) {

    data class Params(val userId: String)

    override suspend fun execute(parameters: Params): Order {
        val cart = cartRepository.getCart(parameters.userId).getOrThrow()
        validateCartUseCase(ValidateCartUseCase.Params(cart)).getOrThrow() // direct call syntax
        return cartRepository.checkout(cart).getOrThrow()
    }
}
```

### Bad

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt

class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
) : ViewModel() {

    fun loadProfile() {
        viewModelScope.launch {
            // BAD: .invoke() is redundant — the operator already enables direct call syntax.
            val profile = getUserProfileUseCase.invoke()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            // BAD: same violation with a parameterised use case.
            val results = searchProductsUseCase.invoke(SearchProductsUseCase.Params(query, null))
        }
    }
}
```

```kotlin
// BAD: no-parameter use case called with an explicit Unit argument — Unit should never be passed.
val profile = getUserProfileUseCase(Unit)

// BAD: same problem combined with the .invoke() violation.
val profile = getUserProfileUseCase.invoke(Unit)
```

## Severity

- `💡 Suggestion` — Any call site using `.invoke()` on a use case instead of direct call syntax
- `💡 Suggestion` — No-parameter use case called with an explicit `Unit` argument