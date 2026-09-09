---
description: Reduce deep nesting in function bodies — use early returns and guard clauses to keep the happy path at the outermost indentation level; more than 3 levels is a suggestion to refactor
paths:
  - "**/*.kt"
---

# Method Nesting & Early Return

## Rule

Function bodies with more than 3 levels of nesting are hard to read and hard to reason about. Each additional nesting level forces the reader to hold more context in their head. Deep nesting is almost always a sign that the function is doing too much or that guard clauses are missing.

Prefer early returns and guard clauses to keep the happy path at the leftmost indentation level. Extract deeply nested blocks into named helper functions.

**Nesting level counting**: Each of the following adds one level of nesting to the block inside it:
- `if` / `else if` / `else`
- `when` branch body
- `try` / `catch` / `finally`
- `for` / `while` / `do-while`
- Lambda arguments (`.let { }`, `.also { }`, `.apply { }`, `.run { }`, `.map { }`, `.filter { }`)

## What to Check

### MUST BLOCK

None — nesting depth is a readability concern, not a correctness issue. All enforcement is at suggestion level.

### MUST FLAG (all at Suggestion level)

- A function body that reaches 4 or more levels of nesting
  - **Fix**: Apply the early-return pattern, extract inner blocks into named private functions, or split the function by responsibility
- A `when` expression nested inside another `when` expression
  - **Fix**: Extract the inner `when` into a named function so each `when` can be understood independently
- A lambda chain with three or more chained `.let { }`, `.also { }`, `.run { }`, or similar scope functions on the same expression
  - **Fix**: Extract the chain into named local variables and intermediate functions; lambda chains hide the control flow
- A `try/catch` block nested inside a `when` branch or an `if/else` block
  - **Fix**: Extract the `try/catch` into a named function that returns `Result<T>` and call it from the outer block

## Recommended Patterns

**Early return instead of nesting:**
```kotlin
// Instead of:
fun process(input: String?): Result {
    if (input != null) {
        if (input.isNotBlank()) {
            return doWork(input)
        }
    }
    return Result.Empty
}

// Prefer:
fun process(input: String?): Result {
    val safeInput = input?.takeIf { it.isNotBlank() } ?: return Result.Empty
    return doWork(safeInput)
}
```

**Guard clause with Elvis:**
```kotlin
val userId = session?.userId ?: return  // early return instead of wrapping in if (session != null)
```

**Extract inner blocks:**
```kotlin
// Instead of a 4-level-deep when { when { try { if { } } } }
// Extract the inner logic:
private fun handleCartEvent(event: CartEvent): CartResult = when (event) {
    is CartEvent.Apply -> applyPromo(event.code)   // each branch is a named call
    is CartEvent.Remove -> removeItem(event.itemId)
}
```

## Common Mistakes

- Wrapping the entire function body in `if (condition) { ... }` instead of returning early when `!condition` — this pushes every line of the happy path one level deeper
- Using `.let { ... }` as a null-check replacement when a simple `?: return` would flatten the code
- Nesting `when` expressions because the outer `when` "sets up context" for the inner one — extract the inner `when` into a function that takes that context as a parameter
- Deeply nesting coroutine `launch` blocks and `withContext` calls — restructure as `suspend` functions that call each other linearly

## Examples

### Good

```kotlin
// features/feature-checkout/ui/src/commonMain/kotlin/.../CheckoutViewModel.kt
// GOOD: Guard clauses and early returns keep nesting at 1-2 levels.
internal class CheckoutViewModel(
    private val placeOrderUseCase: PlaceOrderUseCase,
) : BaseViewModel<CheckoutAction, CheckoutEffect, CheckoutState>(CheckoutState()) {

    override suspend fun handleAction(action: CheckoutAction) {
        when (action) {
            is CheckoutAction.Confirm -> confirmOrder(action.cartId)
            CheckoutAction.Cancel -> emitEffect(CheckoutEffect.NavigateBack)
        }
    }

    private suspend fun confirmOrder(cartId: String) {
        val validCartId = cartId.takeIf { it.isNotBlank() } ?: run {
            updateState { it.copy(errorMessage = "Invalid cart") }
            return
        }
        updateState { it.copy(isLoading = true) }
        placeOrderUseCase(PlaceOrderUseCase.Params(validCartId))
            .onSuccess { order -> updateState { it.copy(isLoading = false, order = order.toUi()) } }
            .onFailure { error -> updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) } }
    }
}
```

### Bad

```kotlin
// features/feature-checkout/ui/src/commonMain/kotlin/.../CheckoutViewModel.kt
// BAD: 4+ levels of nesting — hard to track which condition applies at each line.
internal class CheckoutViewModel(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val validateCartUseCase: ValidateCartUseCase,
) : BaseViewModel<CheckoutAction, CheckoutEffect, CheckoutState>(CheckoutState()) {

    override suspend fun handleAction(action: CheckoutAction) {
        if (action is CheckoutAction.Confirm) {           // level 1
            if (action.cartId.isNotBlank()) {             // level 2
                val validation = validateCartUseCase(ValidateCartUseCase.Params(action.cartId))
                if (validation.isSuccess) {               // level 3
                    try {                                 // level 4 — too deep
                        val order = placeOrderUseCase(PlaceOrderUseCase.Params(action.cartId)).getOrThrow()
                        updateState { it.copy(order = order.toUi()) }
                    } catch (e: Exception) {
                        updateState { it.copy(errorMessage = e.message.orEmpty()) }
                    }
                } else {
                    updateState { it.copy(errorMessage = "Cart is invalid") }
                }
            }
        }
    }
}
```

```kotlin
// BAD: when nested inside when — outer and inner cases must be traced simultaneously.
fun describeStatus(account: Account): String = when (account.tier) {
    Tier.PREMIUM -> when (account.kycStatus) { // nested when — extract to a function
        KycStatus.VERIFIED -> "Premium verified"
        KycStatus.PENDING -> "Premium pending"
        KycStatus.REJECTED -> "Premium rejected"
    }
    Tier.BASIC -> "Basic account"
}
```

## Severity

- `💡 Suggestion` — Function body with 4 or more levels of nesting
- `💡 Suggestion` — `when` expression nested inside another `when` expression
- `💡 Suggestion` — Three or more chained scope functions (`.let { }.also { }.run { }`) on the same expression
- `💡 Suggestion` — `try/catch` block nested inside a conditional branch
