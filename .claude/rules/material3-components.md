---
description: Production Composable code must use Material3 APIs instead of Material2, and prefer YES design system components over raw Material3 widgets when an equivalent exists
paths:
  - "shared/*/ui/**/*.kt"
  - "sharedLib/src/**/*.kt"
  - "features/*/ui/src/*Main/**/*.kt"
  - "core/core-ui/src/*Main/**/*.kt"
---

# Material3 and YES Design System Components

## Rule

This project uses Material Design 3 exclusively. Importing or using any component from `androidx.compose.material` (Material 2) in production source sets is banned. Mixing Material 2 and Material 3 in the same composable tree causes theming inconsistencies — `MaterialTheme.colors` (M2) and `MaterialTheme.colorScheme` (M3) are not compatible, and M2 components do not receive M3 theme tokens.

Material3 is the baseline API. When the YES design system ships a `Yes*` equivalent, production UI should render the design system component instead of the raw Material3 widget.

This rule applies to all production source sets (`commonMain`, `androidMain`, `desktopMain`, `iosMain`). It does NOT apply to test source sets or `@Preview` composables in androidMain.

**Note**: `androidx.compose.material.icons` imports are a shared package used by both Material 2 and Material 3. Icon imports from `androidx.compose.material.icons.*` are explicitly permitted and must NOT be flagged as violations.

## What to Check

### MUST BLOCK

- Any import from `androidx.compose.material` (non-icon sub-packages) in a production source set
  - **Fix**: Replace with the `androidx.compose.material3` equivalent; see the substitution table below
- `MaterialTheme.colors` accessed in any production composable
  - **Fix**: Replace with `MaterialTheme.colorScheme` (Material3 API)
- `MaterialTheme.typography` using M2 typography types (`h1`, `body1`, `subtitle1`, etc.)
  - **Fix**: Use M3 typography tokens (`displayLarge`, `bodyMedium`, `titleSmall`, etc.)

### MUST FLAG

- A file that imports from both `androidx.compose.material` and `androidx.compose.material3`
  - **Fix**: Remove all M2 imports and replace with M3 equivalents; files must not mix both systems
- `MaterialTheme.shapes` used with M2 shape properties (`small`, `medium`, `large` directly on `Shapes`)
  - **Fix**: Use M3 shape tokens (`MaterialTheme.shapes.small` is valid in M3 as well, but verify the import is from M3)

### Material3 that stays correct

These have no design system equivalent, so the M3 component is the right choice and must NOT be flagged:

- **Layout / structure**: `Scaffold`, `Surface` (via `AppScreen`), `Box`/`Column`/`Row`, `LazyColumn`, `LazyRow`
- **`Text` and `Icon`** — the DS styles text through `DesignSystemTheme.typography`, not a wrapper component
- **`Card`** — there is no general-purpose `YesCard`. `YesListItem(contentType = ListItemContentType.Card)` and `YesTransactionCard` are specific components, not substitutes
- **`AlertDialog` / `BasicAlertDialog` / `ModalBottomSheet`** — `YesAlert` is an **inline banner**, not a modal. Material3 remains the dialog/sheet container; the *actions inside* it should still be `YesButton`
- **`SnackbarHost` / `SnackbarHostState`** — no DS equivalent
- **`MaterialTheme.typography.*`** — `DesignSystemTheme` maps every M3 type slot onto a YES text style (`headlineMedium` → YES heading 2, `bodyMedium` → `text_body_base_regular`, …), so reading M3 typography slots inside `DesignSystemTheme` already yields DS styles. Both forms are acceptable; prefer `DesignSystemTheme.typography.*` in new code because the token name states the intent

### Spacing tokens are not dp-named

`Spacing.space_N` is a **scale index, not a dp value**: `Spacing.space_6` is 24dp and `Spacing.space_8` is 32dp — `Spacing.space_24` is **96dp**, not 24dp. Converting a raw `24.dp` literal to `Spacing.space_24` is a 4× size regression that compiles and passes lint. Always check the token's dp comment in `Spacing.kt` when replacing a literal.

## Material 2 → Material 3 Substitution Table

| Material 2 (banned) | Material 3 replacement |
|---|---|
| `Button` | `Button` (from `material3`) |
| `OutlinedButton` | `OutlinedButton` (from `material3`) |
| `TextButton` | `TextButton` (from `material3`) |
| `Text` | `Text` (from `material3`) |
| `TextField` | `TextField` (from `material3`) |
| `OutlinedTextField` | `OutlinedTextField` (from `material3`) |
| `Scaffold` | `Scaffold` (from `material3`) |
| `TopAppBar` | `TopAppBar` / `CenterAlignedTopAppBar` (from `material3`) |
| `BottomNavigation` / `BottomNavigationItem` | `NavigationBar` / `NavigationBarItem` (from `material3`) |
| `Divider` | `HorizontalDivider` / `VerticalDivider` (from `material3`), then prefer `YesDivider` in production UI |
| `Card` | `Card` (from `material3`) |
| `AlertDialog` | `AlertDialog` / `BasicAlertDialog` (from `material3`) |
| `Checkbox` | `Checkbox` (from `material3`) |
| `RadioButton` | `RadioButton` (from `material3`) |
| `Switch` | `Switch` (from `material3`) |
| `Slider` | `Slider` (from `material3`) |
| `CircularProgressIndicator` | `CircularProgressIndicator` (from `material3`) |
| `LinearProgressIndicator` | `LinearProgressIndicator` (from `material3`) |
| `FloatingActionButton` | `FloatingActionButton` (from `material3`) |
| `DropdownMenu` / `DropdownMenuItem` | `DropdownMenu` / `DropdownMenuItem` (from `material3`) |
| `MaterialTheme.colors` | `MaterialTheme.colorScheme` |
| `MaterialTheme.typography.h1` | `MaterialTheme.typography.displayLarge` |
| `MaterialTheme.typography.body1` | `MaterialTheme.typography.bodyLarge` |
| `MaterialTheme.typography.caption` | `MaterialTheme.typography.labelSmall` |

## Common Mistakes

- Importing `androidx.compose.material.Text` instead of `androidx.compose.material3.Text` because the IDE auto-completes the shorter import — always verify the import package after auto-completion
- Using `Divider` from Material 2 because `HorizontalDivider` is unfamiliar — Material 3 renamed `Divider` to `HorizontalDivider`; the M2 `Divider` must not be used
- Stopping at raw Material3 after replacing M2 — if the design system table lists a `Yes*` equivalent, migrate to that component for production UI
- Mixing M2 `MaterialTheme.colors.primary` with M3 components — the M2 color system is not available when the M3 `MaterialTheme` is applied; the call will crash or return incorrect values at runtime
- Importing `androidx.compose.material.icons.Icons` thinking it is an M2 import — icon imports from `material.icons` are shared and are explicitly permitted

## Examples

### Good

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeScreen.kt
// GOOD: All imports are from androidx.compose.material3.
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

@Composable
internal fun HomeScreen(state: HomeState) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.home_title)) })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodyMedium, // M3 typography token
                color = MaterialTheme.colorScheme.onSurface, // M3 color token
            )
            HorizontalDivider()
        }
    }
}
```

```kotlin
// GOOD: Prefer the YES design system component when one exists.
import example.xyz.mobiledesignsystem.designsystem.components.button.ButtonRole
import example.xyz.mobiledesignsystem.designsystem.components.button.YesButton
import example.xyz.mobiledesignsystem.designsystem.components.loader.YesLoader

@Composable
private fun LoadingError(onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column {
        YesLoader()
        YesButton(
            label = stringResource(Res.string.retry),
            onClick = onRetry,
            role = ButtonRole.Primary,
        )
        YesButton(
            label = stringResource(Res.string.dismiss),
            onClick = onDismiss,
            role = ButtonRole.Plain,
        )
    }
}
```

```kotlin
// GOOD: Icon import from material.icons is explicitly permitted — not a violation.
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.back_content_description),
        )
    }
}
```

### Bad

```kotlin
// features/feature-home/ui/src/commonMain/kotlin/.../HomeScreen.kt
// BAD: Imports from androidx.compose.material (M2) — theming will not work correctly
// with the M3 MaterialTheme applied at the app root.
import androidx.compose.material.Button       // violation — use material3
import androidx.compose.material.Divider       // violation — use material3 HorizontalDivider
import androidx.compose.material.MaterialTheme // violation — use material3
import androidx.compose.material.Scaffold      // violation — use material3
import androidx.compose.material.Text          // violation — use material3

@Composable
internal fun HomeScreen(state: HomeState) {
    Scaffold {
        Column {
            Text(
                text = state.subtitle,
                color = MaterialTheme.colors.primary, // violation — MaterialTheme.colors is M2
            )
            Divider() // violation — use HorizontalDivider from material3
            Button(onClick = {}) { Text("Action") }
        }
    }
}
```

```kotlin
// BAD: Raw Material3 widgets where YES design system equivalents exist.
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

@Composable
private fun LoadingError(onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column {
        CircularProgressIndicator() // violation — use YesLoader
        Button(onClick = onRetry) { Text(stringResource(Res.string.retry)) } // violation — use YesButton
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dismiss)) } // violation — use YesButton(role = ButtonRole.Plain)
    }
}
```

```kotlin
// BAD: Mixed M2 and M3 imports in the same file.
import androidx.compose.material.Text          // violation — M2
import androidx.compose.material3.Button       // M3 — correct
import androidx.compose.material3.MaterialTheme

@Composable
internal fun MixedScreen() {
    Button(onClick = {}) {
        Text("Label") // uses M2 Text while Button is M3 — inconsistent theming
    }
}
```

## Severity

- `🚫 Blocking` — Any import from `androidx.compose.material` (non-icon sub-packages) in a production source set
- `⚠️ Change requested` — Mixed M2 and M3 imports in the same file
- `⚠️ Change requested` — `MaterialTheme.colors` used instead of `MaterialTheme.colorScheme`
- `⚠️ Change requested` — A Material3 component used where the design system ships a `Yes*` equivalent (see the table above)
- `⚠️ Change requested` — A raw `.dp` literal replaced with a same-numbered `Spacing.space_N` token (the scale is not dp-named — `space_24` is 96dp)
