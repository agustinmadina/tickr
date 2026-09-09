---
description: String values computed from multiple inputs inside a @Composable must be wrapped in remember with appropriate keys to avoid unnecessary recomposition
paths:
  - "sharedLib/src/**/*.kt"
  - "features/**/ui/**/*.kt"
  - "core/core-ui/**/*.kt"
  - "sdks/**/*.kt"
---

# Compose String Computation & remember

## Rule

String values computed from multiple inputs or via non-trivial operations inside a `@Composable` function must be wrapped in `remember` with the appropriate keys, so the computation runs only when its inputs change — not on every recomposition. Simple string literals and strings from stable `StateFlow` state do not require `remember`.

## What to Check

### MUST BLOCK

- String concatenation or interpolation that combines **two or more unstable or parameter-sourced values** inside a `@Composable` without `remember`
  - **Fix**: `val fullName = remember(firstName, lastName) { "$firstName $lastName" }`
- A `@Composable` that calls a non-trivial string-producing function (e.g., `format()`, `joinToString()`, custom formatters) on every recomposition without `remember`
  - **Fix**: Wrap in `remember(key1, key2, ...) { expensiveFormat(key1, key2) }`; if the formatter is deterministic and the inputs are stable, a single `remember` call is sufficient
- `buildString { }` or `StringBuilder` usage inside a `@Composable` body without `remember`
  - **Fix**: `val s = remember(inputs) { buildString { ... } }`

### MUST FLAG

- String interpolation of a single parameter (not from ViewModel state) inside a deeply nested composable that is itself unconditionally recomposed
  - **Acceptable**: If the composable is a leaf node with no children, `"Hello $name"` where `name` is a single `String` parameter rarely causes a measurable problem — flag as a suggestion rather than blocking
  - **Fix**: `val label = remember(name) { "Hello $name" }` if the composable recomposes frequently
- A computed string passed as a key argument to child composables where instability would force those children to recompose unnecessarily
  - **Fix**: Stabilise with `remember` at the point of computation so the reference is stable across recompositions where inputs are unchanged

## What Does NOT Require remember

The following patterns are explicitly exempt and must NOT be flagged:

- **Compile-time string literals**: `Text("Hello")`, `Text("Submit")` — these are constants baked into the bytecode; they carry no recomposition cost.
- **`stringResource(R.string.foo)`**: The Compose runtime caches resource lookups; wrapping in `remember` provides no benefit.
- **Strings from ViewModel `StateFlow` / `StateFlow`-backed `collectAsStateWithLifecycle()`**: The ViewModel owns the computation; the string is already stable by the time it reaches the composable.
- **Single-variable interpolation where the variable is already a `State<String>`**: The composable will only recompose when the `State` value changes; the interpolation itself is not the trigger.
- **Strings that are used only once inside the composable and not passed to children**: If a formatted label is used in a single `Text` call and the composable is a small leaf, the overhead is negligible — flag as a suggestion only, not blocking.

## Common Mistakes

- Treating every string as requiring `remember` — this is wrong and clutters composables with unnecessary remembered values; only computed strings with multiple unstable inputs need it
- Forgetting to list ALL inputs as `remember` keys — `remember(firstName) { "$firstName $lastName" }` will not recompute when `lastName` changes
- Using `remember` without keys (`remember { ... }`) for a string that depends on parameters — the block runs only on the first composition and never updates
- Computing a formatted string in the composable instead of in the ViewModel when the formatting logic belongs to the presentation model, not the UI layer — prefer moving non-trivial formatting to the ViewModel's `UiState` mapping

## Examples

### Good

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileHeader.kt

// GOOD: Two parameter-sourced values combined — remember with both keys.
@Composable
internal fun ProfileHeader(firstName: String, lastName: String, modifier: Modifier = Modifier) {
    val fullName = remember(firstName, lastName) { "$firstName $lastName" }
    Text(text = fullName, modifier = modifier)
}

// GOOD: Expensive formatting wrapped in remember.
@Composable
internal fun OrderSummary(subtotal: Double, tax: Double, modifier: Modifier = Modifier) {
    val summaryLine = remember(subtotal, tax) {
        "Subtotal: %.2f  Tax: %.2f  Total: %.2f".format(subtotal, tax, subtotal + tax)
    }
    Text(text = summaryLine, modifier = modifier)
}

// GOOD: String literal — no remember needed.
@Composable
internal fun SubmitButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Submit") // compile-time constant, no recomposition concern
    }
}

// GOOD: String from StateFlow state — ViewModel already owns the computation.
@Composable
internal fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Text(uiState.displayName) // stable state value, no additional remember needed
}

// GOOD: Single-variable interpolation — borderline but acceptable at a leaf composable.
@Composable
internal fun Greeting(name: String) {
    Text("Hello, $name") // single input, leaf composable — not worth remember here
}
```

### Bad

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileHeader.kt

// BAD: Two parameters concatenated without remember — string is recreated on every recomposition,
// forcing the Text composable to re-draw even when firstName and lastName have not changed.
@Composable
internal fun ProfileHeader(firstName: String, lastName: String, modifier: Modifier = Modifier) {
    val fullName = "$firstName $lastName" // recomputed unconditionally on every recomposition
    Text(text = fullName, modifier = modifier)
}

// BAD: Expensive format call without remember.
@Composable
internal fun OrderSummary(subtotal: Double, tax: Double, modifier: Modifier = Modifier) {
    val summaryLine = "Subtotal: %.2f  Tax: %.2f  Total: %.2f".format(subtotal, tax, subtotal + tax)
    Text(text = summaryLine, modifier = modifier)
}

// BAD: remember without keys — the block runs once and never updates when inputs change.
@Composable
internal fun ProfileHeader(firstName: String, lastName: String, modifier: Modifier = Modifier) {
    val fullName = remember { "$firstName $lastName" } // captures initial values only
    Text(text = fullName, modifier = modifier)
}

// BAD: Missing one key — lastName changes will not trigger recomputation.
@Composable
internal fun ProfileHeader(firstName: String, lastName: String, modifier: Modifier = Modifier) {
    val fullName = remember(firstName) { "$firstName $lastName" } // lastName not listed as key
    Text(text = fullName, modifier = modifier)
}
```

## Severity

- `🚫 Blocking` — String concatenation or interpolation from two or more non-`State` parameter inputs inside a `@Composable` without `remember`, or non-trivial formatting calls (`.format()`, `joinToString()`, `buildString`) without `remember`
- `⚠️ Change requested` — `remember` used without keys for a string that depends on composable parameters; missing keys in a `remember` call that cause stale computed strings
- `💡 Suggestion` — Single-variable string interpolation from a parameter (not `State`) in a composable that recomposes frequently; computed string passed to child composables without stabilisation
