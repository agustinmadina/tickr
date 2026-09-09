---
description: Enum entries must be referenced using unqualified names via static imports rather than fully qualified names
paths:
  - "features/**/*.kt"
  - "core/**/*.kt"
  - "shared/**/*.kt"
  - "sdks/**/*.kt"
---

# Enum Import Style

## Rule

Enum entries must be referenced using unqualified names (via a static import) rather than fully qualified names whenever the meaning is unambiguous from context. Fully qualified references — `CardRequirementResult.NeedsKycUpgrade`, `LoadState.Loading`, etc. — add noise that makes `when` expressions and function call sites harder to read without adding clarity.

## What to Check

### MUST BLOCK

None — enforcement is at review time.

### MUST FLAG

- A `when` expression whose branches are all written as `EnumClass.Value` — this is the highest-signal location for the violation because the enum type is already encoded in the subject expression
  - **Fix**: Add a static import (`import dev.madina.tickr.feature.cards.domain.CardRequirementResult.NeedsKycUpgrade`) for each branch value and drop the qualifier at the call site
- A function parameter or return expression that uses a fully qualified enum entry when only one enum of that type exists in scope
  - **Fix**: Add a static import and use the unqualified name
- A sealed-class subtype reference in a `when` branch written as `SealedParent.SubType` when all branches in the expression are from the same sealed hierarchy
  - **Fix**: Static-import each subtype that appears more than once and use the unqualified name
- A file that mixes qualified and unqualified references to the same enum class — inconsistency within a file is harder to read than either style applied uniformly
  - **Fix**: Pick one style for the file; prefer the unqualified form with static imports

## Common Mistakes

- Using the qualified form `CardRequirementResult.NeedsKycUpgrade` in every `when` branch because the IDE auto-completes it that way — the result compiles but clutters the expression with six extra tokens per branch
- Conflating the "no wildcard imports" rule with static member imports — `import dev.madina.tickr.SomeEnum.*` is a wildcard and is banned; `import dev.madina.tickr.SomeEnum.SpecificValue` is a named static import and is required by this rule
- Leaving qualified names after a refactor moves the enum to a new package — the file still compiles but the import is stale; resolve by removing the old qualifier and adding the correct static import
- Applying unqualified names when two different enum classes in the same file both have a variant with the same name (e.g., `Status.Active` from module A and `Status.Active` from module B) — this is the one case where the qualifier is necessary for disambiguation

## Examples

### Good

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/.../CardRequirementScreen.kt
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.Approved
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.NeedsKycUpgrade
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.Pending

fun mapToUiMessage(result: CardRequirementResult): String = when (result) {
    Approved -> "Your card is ready."
    NeedsKycUpgrade -> "Please complete identity verification."
    Pending -> "Review in progress."
}
```

```kotlin
// GOOD: single-value reference in a function body
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.NeedsKycUpgrade

fun shouldShowKycBanner(result: CardRequirementResult) =
    result == NeedsKycUpgrade
```

### Bad

```kotlin
// BAD: qualifier repeated on every branch — the type is already known from the subject
fun mapToUiMessage(result: CardRequirementResult): String = when (result) {
    CardRequirementResult.Approved -> "Your card is ready."
    CardRequirementResult.NeedsKycUpgrade -> "Please complete identity verification."
    CardRequirementResult.Pending -> "Review in progress."
}
```

```kotlin
// BAD: wildcard import — banned by the no-wildcard-imports rule in code-quality-checklist.md
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.*

fun mapToUiMessage(result: CardRequirementResult): String = when (result) {
    Approved -> "Your card is ready."
    NeedsKycUpgrade -> "Please complete identity verification."
    Pending -> "Review in progress."
}
```

```kotlin
// BAD: qualified and unqualified mixed in the same file
import dev.madina.tickr.feature.cards.domain.CardRequirementResult.Approved

fun mapToUiMessage(result: CardRequirementResult): String = when (result) {
    Approved -> "Your card is ready."                            // unqualified
    CardRequirementResult.NeedsKycUpgrade -> "Please verify."   // qualified — inconsistent
    CardRequirementResult.Pending -> "Review in progress."       // qualified — inconsistent
}
```

## Exceptions

- Two or more enum or sealed classes in the same file share a variant name — qualify only the ambiguous names; import the unambiguous ones
- Auto-generated or scaffolded code that has not yet been reviewed — fix during the first substantive edit to the file

## Severity

- `⚠️ Change requested` — `when` expression with three or more branches all using the qualified `EnumClass.Value` form
- `⚠️ Change requested` — Mixed qualified and unqualified references to the same enum class within a single file
- `💡 Suggestion` — One or two standalone fully qualified enum references in a function body (still preferred to fix, but not blocking)
