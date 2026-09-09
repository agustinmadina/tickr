---
description: PreviewParameterProvider implementations must be private or internal — they are preview-only infrastructure and must never leak into a module's public API surface
paths:
  - "sharedLib/src/**/*.kt"
  - "features/**/*.kt"
  - "core/**/*.kt"
  - "shared/**/*.kt"
  - "sdks/**/*.kt"
---

# Compose PreviewParameterProvider Visibility

## Rule

All `PreviewParameterProvider` implementations must be declared `private` or `internal`. They exist solely to supply sample data to `@Preview` composables and have no runtime use. Making them `public` leaks preview-only infrastructure as part of the module's API surface, allowing external modules to import and depend on classes that carry no production contract.

This rule is distinct from the general Compose preview exception in `visibility-modifiers.md`. That rule covers `@Preview` functions themselves (which must be `private` per the project standard — see `visibility-modifiers.md`). It does NOT apply to `PreviewParameterProvider` implementations — those have no framework requirement to be `public`.

## What to Check

### MUST BLOCK

- A `PreviewParameterProvider` subclass declared without an explicit visibility modifier — Kotlin defaults to `public`, which leaks the provider into the module's public API
  - **Fix**: Add `private` if the provider is used only in one file (preferred); add `internal` if it is shared across multiple files within the same module
- A `PreviewParameterProvider` subclass explicitly declared `public`
  - **Fix**: Change to `private` or `internal` as above

### MUST FLAG

- A `PreviewParameterProvider` declared `internal` when it is only used in a single file
  - **Acceptable**: `internal` is not wrong, but `private` is the stricter and preferred choice when the provider is co-located with the `@Preview` function that uses it
- A `PreviewParameterProvider` placed in a `commonMain` source set
  - **Fix**: Move to `androidMain` — `@Preview` is Android-only (Compose tooling does not render previews on iOS or Desktop). Placing preview infrastructure in `commonMain` pulls an Android-only concern into the shared source set

## Common Mistakes

- Omitting the visibility modifier entirely because the IDE auto-completes `class MyProvider : PreviewParameterProvider<T>` without a modifier — the result is silently `public`
- Marking a provider `internal` thinking that matches the general feature-module default, without considering that `private` is tighter and more appropriate when the provider is in the same file as its `@Preview` function
- Confusing the `@Preview` function visibility rule with `PreviewParameterProvider` visibility — `@Preview` functions must be `private` per the project standard (see `visibility-modifiers.md`). `PreviewParameterProvider` implementations have an identical requirement: `private` or `internal`, never `public`. Neither has a framework requirement to be non-private.

## Examples

### Good

```kotlin
// features/feature-home/ui/src/androidMain/kotlin/.../HomeScreenPreview.kt
// Provider is private — used only in this file. No external module can import it.
private class HomeScreenUiStateProvider : PreviewParameterProvider<HomeScreenUiState> {
    override val values = sequenceOf(
        HomeScreenUiState.Loading,
        HomeScreenUiState.Content(items = listOf("A", "B", "C")),
        HomeScreenUiState.Error(message = "Something went wrong"),
    )
}

@Preview
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeScreenUiStateProvider::class) uiState: HomeScreenUiState,
) {
    HomeScreen(uiState = uiState)
}
```

```kotlin
// features/feature-home/ui/src/androidMain/kotlin/.../preview/SharedPreviewProviders.kt
// Provider is internal — shared across multiple preview files in the same ui/ sub-module.
// Still not accessible outside the module.
internal class UserCardUiStateProvider : PreviewParameterProvider<UserCardUiState> {
    override val values = sequenceOf(
        UserCardUiState(name = "Alice", avatarUrl = null),
        UserCardUiState(name = "Bob", avatarUrl = "https://example.com/bob.png"),
    )
}
```

### Bad

```kotlin
// BAD: No visibility modifier — defaults to public.
// Any other module can now import HomeScreenUiStateProvider and depend on preview-only
// sample data as if it were a stable production type.
class HomeScreenUiStateProvider : PreviewParameterProvider<HomeScreenUiState> {
    override val values = sequenceOf(
        HomeScreenUiState.Loading,
        HomeScreenUiState.Content(items = listOf("A", "B", "C")),
    )
}

// BAD: Explicitly public — same problem, now intentional.
public class UserCardUiStateProvider : PreviewParameterProvider<UserCardUiState> {
    override val values = sequenceOf(
        UserCardUiState(name = "Alice", avatarUrl = null),
    )
}

// BAD: Placed in commonMain — @Preview is Android-only. This pulls Android preview
// infrastructure into the shared source set where it serves no purpose for iOS or Desktop.
// File: features/feature-home/ui/src/commonMain/kotlin/.../HomeScreenUiStateProvider.kt
internal class HomeScreenUiStateProvider : PreviewParameterProvider<HomeScreenUiState> {
    override val values = sequenceOf(HomeScreenUiState.Loading)
}
```

## Severity

- `🚫 Blocking` — `PreviewParameterProvider` subclass is `public` (explicit or default) in any module
- `⚠️ Change requested` — `PreviewParameterProvider` placed in a `commonMain` source set instead of `androidMain`
- `💡 Suggestion` — `PreviewParameterProvider` declared `internal` when it is only used in a single file; prefer `private`
