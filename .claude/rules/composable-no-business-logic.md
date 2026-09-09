---
description: Composable functions must render already-resolved UiState only — business-rule derivation, domain branching, and collection processing belong in the ViewModel or a use case, not the UI layer
paths:
  - "features/*/ui/**/*.kt"
  - "shared/*/ui/**/*.kt"
  - "core/core-ui/**/*.kt"
  - "sharedLib/src/**/*.kt"
---

# Composable No Business Logic

## Rule

A `@Composable` function's job is to render a `UiState` it is handed — nothing more. Every value a Composable displays, every branch it takes based on domain meaning, and every collection it iterates must already be resolved by the time it reaches the Composable. If a Composable has to *derive* business meaning from raw or partially-processed data — evaluate an eligibility rule, branch on a domain enum to decide behavior, filter/sort/aggregate a list, or reach into the domain/data layer itself — that logic is misplaced. It belongs in the ViewModel's state-mapping code, or one layer further down in a use case, per `use-case-pattern.md`.

This rule was raised in PR #239 review, where business logic had been added directly to a Compose screen instead of the ViewModel. It codifies "no business logic in the UI" as a checkable standard, distinct from the several existing Compose rules that govern *how* a Composable renders (strings, dimensions, collections, remember) — this rule governs *what a Composable is allowed to decide*.

## What to Check

### MUST BLOCK

- A `@Composable` function (or a private helper called only from one) that injects or directly calls a `*UseCase` or `*Repository` — via `koinInject<T>()`, `koinViewModel<T>()` on the wrong type, direct construction, or a function parameter of that type — bypassing the screen's own ViewModel entirely
  - **Fix**: Route the call through the ViewModel. Add a use case call to `handleAction`/`init` in the ViewModel, expose the result via `UiState`, and pass only the resolved value or a callback (`onClick: () -> Unit`) into the Composable
- A `@Composable` function parameter typed as a domain repository or use case interface (e.g. `fun CardsScreen(cardsRepository: CardsRepository)`)
  - **Fix**: The Composable's parameters must be `UiState` (or its fields) and callback lambdas only. Remove the domain-typed parameter and thread the needed value through `UiState` instead

### MUST FLAG

- A `@Composable` function that computes a business-meaningful derived value inline — an eligibility check, a threshold comparison, a multi-field condition that encodes a business rule (e.g. `user.tier == Tier.PREMIUM && user.kycStatus == KycStatus.VERIFIED`) — instead of reading a single pre-computed field from `UiState`
  - **Fix**: Move the condition into the ViewModel's state-mapping code (or a use case if it requires a repository call) and expose a single resolved field, e.g. `UiState.isEligibleForUpgrade: Boolean`
- A `@Composable` function that branches (`if`/`when`) on a domain model or domain enum to decide *business* behavior — which flow to launch, whether an action is permitted, what data to request next — as opposed to selecting a *visual* variant that the ViewModel has already decided on
  - **Fix**: Have the ViewModel branch on the domain type once, and expose the outcome as a UI-level enum/boolean/sealed type in `UiState` that the Composable can safely branch on for rendering only
- A `@Composable` function that filters, sorts, groups, deduplicates, or aggregates a collection (`.filter { }`, `.sortedBy { }`, `.groupBy { }`, `.distinctBy { }`, `.sumOf { }`, etc.) before rendering it
  - **Fix**: Perform the operation in the ViewModel's mapping step (or a use case) and expose the already-processed `ImmutableList`/`ImmutableMap` per `compose-immutable-collections.md`
- A `@Composable` function that calls a domain→`*Ui` (or domain→anything) mapper function itself, rather than receiving an already-mapped `*Ui` model from `UiState`
  - **Fix**: The mapping call itself is placed correctly per `model-layering-naming-conventions.md` (mappers live in `ui/`, not `domain/`) — but it must be *invoked* by the ViewModel's state-mapping code, not by the Composable. Move the call site into the ViewModel and pass the mapped `*Ui` model down
- A private helper function declared in the same file as a screen Composable whose body performs any of the above (business derivation, domain branching, collection processing) and is called from the Composable's render path
  - **Fix**: Same as the inline case — a private helper does not change where the logic executes; move it to the ViewModel or a use case

## What Does NOT Count as Business Logic

These are pure rendering concerns and must NOT be flagged:

- Choosing a color, icon, text style, or content-description based on a **UI-level enum or boolean that the ViewModel already computed** (e.g. `when (state.badgeVariant) { BadgeVariant.Success -> ... }`)
- Simple null/empty checks that decide whether to show or hide a piece of UI (`if (state.errorMessage != null) { ... }`, `if (items.isEmpty()) EmptyState() else ...`)
- Conditional rendering that branches on already-resolved `UiState` (`if (state.isLoading) LoadingIndicator() else Content()`) — this is presentation branching on a value the ViewModel already decided, not business-meaning derivation
- Animation, scroll, drag, or focus state held in local `remember { mutableStateOf(...) }` — ephemeral render-only state is explicitly allowed by the MVI convention in `CLAUDE.md`
- Pure display formatting that requires no domain knowledge — capitalization, pluralization of an already-resolved count, locale-aware number/date formatting of an already-resolved value (subject to `compose-string-remember.md`'s memoization requirements)

## Relationship to Other Rules

This rule governs **what** a Composable is allowed to decide. It does not duplicate:

- `viewmodel-state-source-of-truth.md` / `update-state-purity.md` — govern how the ViewModel itself mutates `UiState`, once the logic is correctly placed there
- `model-layering-naming-conventions.md` — governs **where mapper functions live as files** (domain→Ui mappers belong in `ui/`); this rule governs **who calls them** (the ViewModel's mapping step, not the Composable body)
- `compose-immutable-collections.md` — governs the **type** a `UiState` collection must use; this rule governs **who is allowed to transform** that collection before it becomes `UiState`
- `compose-string-remember.md` — governs memoization of string computation that is legitimately in the Composable (pure display formatting); this rule governs whether the computation is legitimate there at all
- `data-layer-use-case-prohibition.md` / `repository-dependency-boundaries.md` — govern inverted dependencies **within the data layer**; this rule governs a Composable **skipping the ViewModel layer entirely** to reach domain/data, a different failure mode with the same root cause (business logic outside its layer)
- The MVI & Layering Conventions in `CLAUDE.md` ("Presentation state lives in the ViewModel... Local `remember` is only for ephemeral render-only concerns") — this rule is the enforceable, example-backed elaboration of that same principle as it applies specifically to business/domain logic (the conventions doc also covers navigation-step state and sheet visibility, which are a `viewmodel-state-source-of-truth.md` concern, not this one)

## Common Mistakes

- Believing a derived value is "just a boolean" so it is harmless to compute in the Composable — the derivation still encodes a business rule (e.g. eligibility, permission, tier logic) that must change in lockstep with the same rule wherever else it is evaluated; duplicating it in the UI layer creates a second place that can drift out of sync
- Confusing safe presentation branching with business branching — `if (state.isLoading)` is safe (branching on a UI-level flag the ViewModel already resolved); `if (user.role == Role.ADMIN && user.kycStatus == VERIFIED)` is not (branching on raw domain fields to decide business-meaningful behavior)
- Writing `items(list.sortedBy { it.timestamp })` directly inside `LazyColumn` — sort order is a business/product decision (e.g. "most recent first") that belongs in the ViewModel, not a one-line convenience in the render call
- Calling `koinInject<SomeUseCase>()` inside a Composable "just to fetch one extra piece of data quickly" instead of adding it to the screen's own `UiState` — this creates a second, ViewModel-less data path that the `BaseViewModel` state/effect/logging machinery never sees
- Adding a `private fun computeX(...)` helper in the screen's `.kt` file that mirrors what should be a use case, on the reasoning that "it's not enough logic to justify a use case" — size is not the test; whether the computation encodes a business rule is

## Examples

### Good

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/com/example/app/feature/cards/ui/CardsUiState.kt
// GOOD: The eligibility rule is already resolved into a single UI-level field.
data class CardsUiState(
    val isEligibleForUpgrade: Boolean = false,
    val transactions: ImmutableList<TransactionUi> = persistentListOf(),
)
```

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/com/example/app/feature/cards/ui/CardsViewModel.kt
// GOOD: The business rule (tier + KYC status → eligibility) is evaluated once, here,
// where it can be tested and changed alongside any other business logic for this screen.
// Sorting also happens here, not in the Composable.
internal class CardsViewModel(
    private val getCardRequirementUseCase: GetCardRequirementUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
) : BaseViewModel<CardsAction, CardsEffect, CardsUiState>(CardsUiState()) {

    private suspend fun loadCards() {
        getCardRequirementUseCase()
            .onSuccess { result ->
                updateState {
                    it.copy(isEligibleForUpgrade = result == CardRequirementResult.Approved)
                }
            }
            .onFailure { error -> log.e(error) { "Failed to load card requirement" } }

        getTransactionsUseCase()
            .onSuccess { transactions ->
                val sorted = transactions.sortedByDescending { it.timestamp }.map { it.toUi() }
                updateState { it.copy(transactions = sorted.toPersistentList()) }
            }
            .onFailure { error -> log.e(error) { "Failed to load transactions" } }
    }
}
```

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/com/example/app/feature/cards/ui/CardsScreen.kt
// GOOD: The Composable only renders what it is given. isEligibleForUpgrade and
// transactions are already resolved; the only branching here is presentation branching
// on a UI-level boolean, which is explicitly allowed.
@Composable
internal fun CardsScreen(state: CardsUiState, onUpgradeClicked: () -> Unit) {
    AppScreen {
        Column {
            if (state.isEligibleForUpgrade) {
                UpgradeBanner(onClick = onUpgradeClicked)
            }
            LazyColumn {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionRow(transaction)
                }
            }
        }
    }
}
```

### Bad

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/com/example/app/feature/cards/ui/CardsScreen.kt
// BAD: The Composable evaluates a business rule inline (tier + KYC status) and calls
// a use case directly, bypassing CardsViewModel entirely. BaseViewModel's state/effect/
// logging pipeline never sees this data path, and the eligibility rule now lives in two
// places if any other screen needs it.
@Composable
internal fun CardsScreen(user: User) {
    val getCardRequirementUseCase = koinInject<GetCardRequirementUseCase>() // violation — bypasses ViewModel

    AppScreen {
        Column {
            // violation — business rule (tier + KYC) evaluated inline in the Composable
            val isEligibleForUpgrade = user.tier == Tier.PREMIUM && user.kycStatus == KycStatus.VERIFIED
            if (isEligibleForUpgrade) {
                UpgradeBanner(onClick = { /* ... */ })
            }

            var requirementResult by remember { mutableStateOf<CardRequirementResult?>(null) }
            LaunchedEffect(Unit) {
                requirementResult = getCardRequirementUseCase().getOrNull() // violation — domain call from Composable
            }
        }
    }
}
```

```kotlin
// features/feature-transactions/ui/src/commonMain/kotlin/com/example/app/feature/transactions/ui/TransactionsScreen.kt
// BAD: Sorting and filtering are business/product decisions performed in the render path.
// If the "most recent first, exclude pending" rule changes, this file (and every other
// place that duplicates it) must be found and updated individually.
@Composable
internal fun TransactionsScreen(state: TransactionsUiState) {
    AppScreen {
        LazyColumn {
            val visible = state.transactions
                .filter { it.status != TransactionStatus.Pending } // violation — filtering in Composable
                .sortedByDescending { it.timestamp }                // violation — sorting in Composable
            items(visible, key = { it.id }) { transaction ->
                TransactionRow(transaction)
            }
        }
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/com/example/app/feature/profile/ui/ProfileScreen.kt
// BAD: A private helper in the same file still executes the business logic in the UI
// layer — moving it out of the composable body and into a nearby function changes
// nothing about where the rule lives.
@Composable
internal fun ProfileScreen(state: ProfileUiState) {
    AppScreen {
        Text(text = describeAccountStanding(state.user)) // violation — see helper below
    }
}

// violation — this is domain-mapping logic (User -> display string) that belongs in the
// ViewModel's UiState mapping, not as a file-local helper in ui/.
private fun describeAccountStanding(user: User): String = when {
    user.kycStatus == KycStatus.VERIFIED && user.tier == Tier.PREMIUM -> "Premium verified"
    user.kycStatus == KycStatus.PENDING -> "Verification pending"
    else -> "Standard"
}
```

## Severity

- `🚫 Blocking` — `@Composable` (or a helper it calls) injects or directly calls a `*UseCase` or `*Repository`, bypassing the screen's ViewModel
- `🚫 Blocking` — `@Composable` function parameter typed as a domain repository or use case interface
- `⚠️ Change requested` — `@Composable` computes a business-meaningful derived value inline instead of reading a pre-computed `UiState` field
- `⚠️ Change requested` — `@Composable` branches on a domain model/enum to decide business behavior rather than selecting a visual variant already resolved by the ViewModel
- `⚠️ Change requested` — `@Composable` filters, sorts, groups, or aggregates a collection before rendering it
- `⚠️ Change requested` — `@Composable` calls a domain→`*Ui` mapper function itself instead of receiving an already-mapped model from `UiState`
- `⚠️ Change requested` — Private helper co-located with a screen Composable performs business derivation, domain branching, or collection processing on the Composable's behalf
