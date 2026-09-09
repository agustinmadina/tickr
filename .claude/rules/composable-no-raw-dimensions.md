---
description: Avoid magic literal .dp and .sp values in Composables — define named dimension tokens in the design system or use MaterialTheme spacing; small structural values are exempt
paths:
  - "sharedLib/src/**/*.kt"
  - "features/*/ui/**/*.kt"
  - "shared/*/ui/**/*.kt"
  - "core/core-ui/**/*.kt"
---

# Composable No Raw Dimensions

## Rule

Raw dimension literals scattered across Composables create an inconsistent design and make global spacing changes expensive. When a designer updates the 16dp padding token to 20dp, every hardcoded `16.dp` must be hunted down and changed manually — whereas a named token requires a single change.

Extract repeated or semantically meaningful dimension values to named constants in the design system (`core-ui`) or as local file-level constants. Use `MaterialTheme.typography` for text sizes instead of raw `.sp` literals.

This is a soft rule — the goal is consistency and maintainability, not zero tolerance. Small structural values (divider thickness, icon borders, corner radius for visual polish) are exempt because they carry their own semantic meaning from their literal value.

## What to Check

### MUST BLOCK

None — no dimension issue rises to a build-breaking violation. All enforcement is at suggestion level.

### MUST FLAG

- A `.dp` literal greater than 4dp in a `Modifier` chain (`padding()`, `size()`, `height()`, `width()`, `Spacer()`) that does not correspond to a named token
  - **Acceptable**: `0.dp`, `1.dp`, `2.dp` — these are structural (dividers, borders, stroke widths) and their meaning is clear from the literal
  - **Fix**: Define a named constant (e.g., `val SpacingMedium = 16.dp`) in `core-ui/` or in a local `Dimensions.kt` and use the name at the call site
- A `.sp` literal used directly as `fontSize` in a `TextStyle` or `Text()` modifier instead of referencing `MaterialTheme.typography`
  - **Fix**: Use `MaterialTheme.typography.bodyMedium` (or the appropriate style token) rather than `fontSize = 14.sp`
- The same `.dp` literal appearing three or more times within the same file — this is a strong signal that the value should be extracted to a named constant
  - **Fix**: Declare `private val ItemSpacing = 12.dp` at the file level and replace every occurrence

## Exemptions

The following are explicitly exempt and must NOT be flagged:

- `0.dp` and `1.dp` — zero and one-pixel structural values with no design-system equivalent
- `fillMaxSize()`, `fillMaxWidth()`, `fillMaxHeight()`, `wrapContentSize()` — no literal dimension involved
- `@Preview` composables — design exploration in previews may use raw literals
- Border stroke widths (e.g., `BorderStroke(1.dp, color)`) — structural, not spacing
- Explicit corner radius values that are part of a component's visual identity (e.g., `RoundedCornerShape(50)` for fully circular)

## Common Mistakes

- Naming a constant after its value (`val dp16 = 16.dp`) instead of its semantic role (`val SpacingMedium = 16.dp`) — value-named constants are meaningless when the design token changes
- Defining the same spacing value in three different feature modules instead of sharing it from `core-ui` — creates inconsistency when one module updates and another does not
- Using `MaterialTheme.typography.bodyMedium.fontSize` to extract the raw sp value and then adding to it (`MaterialTheme.typography.bodyMedium.fontSize + 2.sp`) — use a dedicated typography style variant instead
- Placing dimension constants inside a `companion object` on the screen file instead of a shared `Dimensions.kt` — limits reuse across the feature module

## Examples

### Good

```kotlin
// core/core-ui/src/commonMain/kotlin/com/example/app/core/ui/theme/Dimensions.kt
// GOOD: Named spacing tokens shared across the design system.
object Dimensions {
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingXLarge = 32.dp
}
```

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeScreen.kt
// GOOD: Named constants used; MaterialTheme.typography for text sizes.
import dev.madina.tickr.core.ui.theme.Dimensions.SpacingMedium
import dev.madina.tickr.core.ui.theme.Dimensions.SpacingSmall

@Composable
internal fun HomeScreen(state: HomeState) {
    Column(modifier = Modifier.padding(SpacingMedium)) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium, // no raw .sp literal
        )
        Spacer(modifier = Modifier.height(SpacingSmall))
        HomeItemList(items = state.items)
    }
}
```

```kotlin
// GOOD: 1.dp divider is exempt — structural value with obvious meaning from the literal.
HorizontalDivider(thickness = 1.dp)
```

### Bad

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeScreen.kt
// BAD: Raw .dp literals with no named constant — inconsistent with other screens,
// and a global spacing change requires finding every occurrence manually.
@Composable
internal fun HomeScreen(state: HomeState) {
    Column(modifier = Modifier.padding(16.dp)) { // violation — use SpacingMedium
        Text(
            text = state.title,
            fontSize = 22.sp, // violation — use MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp)) // violation — use SpacingSmall
        HomeItemList(items = state.items)
    }
}
```

```kotlin
// BAD: Same literal repeated three times in one file — should be a named constant.
@Composable
internal fun ProfileCard(profile: ProfileUi) {
    Card(modifier = Modifier.padding(12.dp)) { // violation — repeated
        Column(modifier = Modifier.padding(12.dp)) { // violation — repeated
            Row(modifier = Modifier.padding(12.dp)) { // violation — 3+ occurrences, extract it
                // ...
            }
        }
    }
}
```

```kotlin
// BAD: Value-named constant — meaningless if the design token changes from 16 to 20.
private val dp16 = 16.dp // violation — use SpacingMedium or a semantic name

@Composable
internal fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = dp16)) {
        Text(label)
    }
}
```

## Severity

- `💡 Suggestion` — `.dp` literal greater than 4dp in a `Modifier` chain without a named constant
- `💡 Suggestion` — `.sp` literal used as `fontSize` directly instead of `MaterialTheme.typography` style
- `💡 Suggestion` — Same `.dp` literal appearing three or more times in the same file
