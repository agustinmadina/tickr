---
description: "@Composable parameters and UiState properties holding collections must use kotlinx.collections.immutable types to ensure Compose stability"
paths:
  - "sharedLib/src/**/*.kt"
  - "features/**/ui/**/*.kt"
  - "core/core-ui/**/*.kt"
  - "sdks/**/ui/**/*.kt"
---

# Compose Immutable Collections

## Rule

All `@Composable` function parameters that accept collection types and all `UiState` data class properties that hold collections must use `kotlinx.collections.immutable` types (`ImmutableList`, `ImmutableSet`, `ImmutableMap`, etc.) instead of standard Kotlin collection interfaces (`List`, `Set`, `Map`). Standard Kotlin collections are not `@Stable` in Compose's type system — Compose cannot guarantee they will not mutate, so it must conservatively schedule recomposition even when collection contents are unchanged.

## What to Check

### MUST BLOCK

- There are no blocking violations for this rule. Violations are `⚠️ Change requested` (see Severity section).

### MUST FLAG

- A `@Composable` function that has a parameter typed as `List<T>`, `Set<T>`, or `Map<K, V>` (or their mutable variants)
  - **Fix**: Change the parameter type to `ImmutableList<T>`, `ImmutableSet<T>`, or `ImmutableMap<K, V>` from `kotlinx.collections.immutable`
- A `UiState` data class (models passed directly to a `@Composable` root screen or content function) that holds a property typed as `List<T>`, `Set<T>`, or `Map<K, V>`
  - **Fix**: Change the property type to the corresponding immutable collection type; update ViewModel mapping code to call `.toPersistentList()`, `.toPersistentSet()`, or `.toPersistentMap()` when converting from domain collections
- A `UiState` data class that uses `emptyList()`, `emptySet()`, or `emptyMap()` as a default value for a collection property
  - **Fix**: Use `persistentListOf()`, `persistentSetOf()`, or `persistentMapOf()` as the default value so the field type and default are consistent
- A `@Composable` function that converts a standard collection to an immutable collection inline at the call site (e.g., `list.toPersistentList()` passed as an argument)
  - **Fix**: The conversion must happen upstream in the ViewModel when building `UiState`, not inside the Composable. Inline conversion inside a Composable defeats the purpose — it allocates a new object on every recomposition

## Scope

This rule applies exclusively to `features/*/ui/` source sets and any shared UI code in `core-ui/`. It must NOT propagate to domain or data layers:

- **Domain layer** (`features/*/domain/`): Must never import `kotlinx.collections.immutable`. Domain models use standard Kotlin collection interfaces — they carry no Compose dependency.
- **Data layer** (`features/*/data/`): Must never import `kotlinx.collections.immutable`. DTOs, entities, and repository implementations use standard collections.
- **ViewModel** (`features/*/ui/`): ViewModels are in the `ui/` sub-module and are responsible for converting domain collections to immutable types when building `UiState`. The conversion boundary is the ViewModel — domain `List<T>` in, `ImmutableList<T>` out in `UiState`.

## Common Mistakes

- Using `List<T>` in a `UiState` data class because it "looks like" a pure data model. `UiState` is a UI concern — it is held in a `StateFlow` and passed directly to Composables. It must satisfy Compose stability requirements.
- Forgetting to update the `UiState` default value after changing a property type — `emptyList()` returns `List<T>`, which mismatches an `ImmutableList<T>` property type and requires an explicit cast that silences the compiler warning without fixing the problem.
- Converting in the Composable rather than in the ViewModel — this is a common reflex when a `@Composable` receives a `List` from a non-`UiState` source. The conversion must happen before the value is emitted from the `StateFlow`.
- Applying immutable collections to domain or data model classes. Domain models are passed across module boundaries as standard Kotlin types. Adding a Compose-specific library dependency to `domain/` violates domain purity (see `code-quality-checklist.md`).
- Using `toImmutableList()` (from older versions of the library) instead of `toPersistentList()`. The `Persistent*` variants are the recommended API in `kotlinx.collections.immutable` — they implement the `Immutable*` interfaces and are the correct conversion functions.

## Examples

### Bad

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsUiState.kt
// BAD: List<TransactionUi> is not @Stable — Compose cannot guarantee it won't mutate.
// Every StateFlow emission will trigger recomposition of all Composables that read `transactions`,
// even if the list contents are identical to the previous emission.
data class TransactionsUiState(
    val transactions: List<TransactionUi> = emptyList(), // violation: standard List with emptyList() default
    val filters: Set<FilterUi> = emptySet(),             // violation: standard Set with emptySet() default
)
```

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsScreen.kt
// BAD: @Composable parameter typed as List<T> — Compose treats it as unstable.
// Even if the list contents did not change, Compose will recompose this function
// whenever the parent recomposes, because it cannot verify the list is unmodified.
@Composable
fun TransactionList(
    transactions: List<TransactionUi>, // violation: standard List
    onTransactionClick: (String) -> Unit,
) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionRow(transaction, onTransactionClick)
        }
    }
}
```

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsViewModel.kt
// BAD: ViewModel converts to immutable inside the Composable call site, not here.
// The UiState property is still List<T>, so the Composable still sees an unstable type.
class TransactionsViewModel(...) : ViewModel() {
    val uiState: StateFlow<TransactionsUiState> = transactionsFlow
        .map { transactions ->
            TransactionsUiState(
                transactions = transactions.map { it.toUi() } // returns List<T>, not ImmutableList<T>
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())
}
```

### Good

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsUiState.kt
// GOOD: ImmutableList and ImmutableSet are @Stable — Compose can skip recomposition
// when the StateFlow emits a new UiState with structurally equal collections.
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class TransactionsUiState(
    val transactions: ImmutableList<TransactionUi> = persistentListOf(),
    val filters: ImmutableSet<FilterUi> = persistentSetOf(),
)
```

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsScreen.kt
// GOOD: @Composable parameter typed as ImmutableList<T> — Compose treats it as stable.
// Skippable recomposition is now possible for this function.
import kotlinx.collections.immutable.ImmutableList

@Composable
fun TransactionList(
    transactions: ImmutableList<TransactionUi>,
    onTransactionClick: (String) -> Unit,
) {
    LazyColumn {
        items(transactions) { transaction ->
            TransactionRow(transaction, onTransactionClick)
        }
    }
}
```

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/.../TransactionsViewModel.kt
// GOOD: ViewModel owns the conversion boundary. Domain List<Transaction> is mapped
// to ImmutableList<TransactionUi> before being emitted into UiState.
// The Composable always receives a stable, immutable type.
import kotlinx.collections.immutable.toPersistentList

internal class TransactionsViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> = getTransactionsUseCase()
        .map { transactions ->
            TransactionsUiState(
                transactions = transactions.map { it.toUi() }.toPersistentList(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())
}
```

## Severity

- `⚠️ Change requested` — `@Composable` function parameter typed as `List<T>`, `Set<T>`, or `Map<K, V>`
- `⚠️ Change requested` — `UiState` data class property typed as a standard Kotlin collection interface
- `⚠️ Change requested` — `UiState` default value using `emptyList()`, `emptySet()`, or `emptyMap()` instead of `persistentListOf()`, `persistentSetOf()`, or `persistentMapOf()`
- `⚠️ Change requested` — Inline collection conversion (`.toPersistentList()`) inside a `@Composable` body or argument list instead of in the ViewModel
- `💡 Suggestion` — `kotlinx.collections.immutable` import present in `domain/` or `data/` source sets — this library must remain a `ui/`-only dependency
