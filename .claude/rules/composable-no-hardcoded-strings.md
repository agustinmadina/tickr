---
description: Composable functions must not contain hardcoded user-visible strings — use Compose Resources stringResource(Res.string.*) for all displayed text
paths:
  - "sharedLib/src/**/*.kt"
  - "features/*/ui/**/*.kt"
  - "shared/*/ui/**/*.kt"
  - "core/core-ui/**/*.kt"
---

# Composable No Hardcoded Strings

## Rule

User-visible strings must never be hardcoded as literals inside `@Composable` functions. All displayed text must be loaded from the Compose Resources system using `stringResource(Res.string.key_name)`. This ensures strings are translatable, centralised, and consistent with the design system's copy.

This applies to every location where a string is rendered or used as accessibility metadata: `Text()` content, button labels, `contentDescription`, `placeholder`, `label`, dialog titles and bodies, snackbar messages, and tooltip text.

## What to Check

### MUST BLOCK

- A string literal passed directly to `Text("...")`, `Button { Text("...") }`, or any composable that renders text to the screen
  - **Fix**: Add the string to the appropriate `composeResources/values/strings.xml` file and reference it via `stringResource(Res.string.your_key)`
- A hardcoded string literal assigned to `contentDescription = "..."` on any `Modifier` or composable parameter
  - **Fix**: Add to strings resources and reference with `stringResource(Res.string.your_key)`

### MUST FLAG

- A hardcoded error message string passed from ViewModel state that is rendered directly in the UI
  - **Fix**: Define the error string in resources; the ViewModel should emit a string resource ID or a sealed `UiMessage` type that maps to a resource, not a raw hardcoded string
- A hardcoded placeholder or `label` string inside `TextField`, `OutlinedTextField`, or any other input component
  - **Fix**: Use `stringResource(Res.string.your_key)` for placeholder and label text
- A hardcoded string in a dialog title, dialog body, or confirmation message
  - **Fix**: Use `stringResource(Res.string.your_key)` consistently for all dialog copy
- A hardcoded string in a `Snackbar` message or `snackbarHostState.showSnackbar("...")`
  - **Fix**: Use `stringResource` at the composable level or pass a string resource reference from the ViewModel via a `UiEffect`

## Exemptions

The following patterns are explicitly exempt and must NOT be flagged:

- **Log tags**: `Logger.withTag("ScreenName")` — these are developer-facing, not user-facing
- **Analytics event names**: `analytics.track("button_tapped")` — these are internal identifiers, not displayed text
- **Test data**: Strings in test files or `@Preview` composables — these are tooling artifacts
- **Navigation routes**: String route constants used for navigation, not displayed to users
- **`@Preview` composable data**: Fake data passed to previews for development purposes
- **Placeholder stand-ins for server-supplied content**: literals in a clearly-marked placeholder
  collection that stands in for data a backend will supply — a release-notes feed, a promotions
  carousel, a transaction list. See the qualifying conditions below.

### Placeholder stand-ins for server-supplied content

The rule exists to keep **translatable UI copy** out of source. A stub's stand-in for
backend-supplied content is not that copy: it is disposable scaffolding that the real feed deletes.
Routing it through `strings.xml` adds keys that must be removed again when the feed lands, and files
feed data alongside app copy as though the localisation pipeline owned it.

This says nothing about *how* the eventual feed is localised — that is the feed's contract to define,
and a locale-neutral payload the client formats is just as valid as a pre-localised one. Where the
client will do the formatting (dates and numbers especially), the deferred ticket should say so: a
hardcoded `"Mar 14 2026"` is en-US-shaped, and the real feed should carry ISO-8601 for the client to
render.

This exemption applies **only** when all of the following hold:

1. The content is a stand-in for data a backend will own, not copy the app owns. A screen title, a
   button label, or a section heading is app copy and is **never** exempt.
2. The stand-in lives in a single named collection or constant (e.g. a `private val` list), so
   replacing it with the real feed is one edit with no call-site changes.
3. The model carrying it is typed for runtime data (`String`), not for resources
   (`StringResource`) — so the stand-in cannot quietly become a permanent home for app copy, and the
   swap to the real feed needs no model change.
4. A single-line `//` comment on that collection states it is placeholder content for a backend
   feed and names the ticket or phase that will replace it. Not KDoc and not a multi-line block —
   the collection is non-public, and `comment-and-kdoc-scope.md` reserves KDoc for public
   cross-module API and caps `//` comments at one line.

Where a screen mixes both, the split must be explicit: app-owned chrome via
`stringResource(Res.string.*)`, feed stand-ins as plain `String`s in the placeholder collection.

Worked example of the split — a screen whose title is app copy while its list content is a feed
stand-in:

```kotlin
// App-owned chrome: resource key, shared with the row label that opens this screen.
YesHeading(title = stringResource(Res.string.settings_release_notes_title), role = HeadingRole.H1)

// Feed stand-in: plain Strings in one named collection, one-line comment naming what replaces it:
// Placeholder stub (TICKET-631): replaced by the server-supplied release-notes feed in a later ticket.
private val releaseNoteEntries = persistentListOf(
    ReleaseNoteEntryUi(date = "Mar 14 2026", version = "5.16.0", description = "..."),
)
```

## Common Mistakes

- Using a hardcoded string "temporarily" while waiting for copy approval — the string ends up in production; use a placeholder resource key from the start. This is **not** the same as the
  server-supplied-content exemption above: copy the app owns and will ship needs a resource key even
  when the final wording is unsettled. The exemption covers data the app will never own.
- Passing a hardcoded English string from the ViewModel via `UiEffect` to `snackbarHostState.showSnackbar()` — the ViewModel must not own user-facing strings; emit a resource reference or a sealed effect type that the Composable maps to `stringResource`
- Writing `Text(text = if (isLoading) "Loading..." else "Submit")` — both branches contain hardcoded strings; each must be a `stringResource` call
- Adding `contentDescription = null` to avoid the rule — if the element is meaningful to accessibility, it requires a description; use `stringResource`

## Examples

### Good

```kotlin
// features/feature-auth/ui/src/commonMain/kotlin/.../LoginScreen.kt
// GOOD: All displayed text loaded from string resources.
@Composable
internal fun LoginScreen(
    state: LoginState,
    onLoginClicked: () -> Unit,
) {
    Column {
        Text(text = stringResource(Res.string.login_title))
        OutlinedTextField(
            value = state.email,
            onValueChange = { /* ... */ },
            label = { Text(stringResource(Res.string.login_email_label)) },
            placeholder = { Text(stringResource(Res.string.login_email_placeholder)) },
        )
        Button(onClick = onLoginClicked) {
            Text(stringResource(Res.string.login_button_label))
        }
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage, // errorMessage is a pre-resolved string from the domain/data layer
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../AvatarImage.kt
// GOOD: contentDescription loaded from resources for accessibility.
@Composable
internal fun AvatarImage(avatarUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = stringResource(Res.string.profile_avatar_content_description),
        modifier = modifier,
    )
}
```

### Bad

```kotlin
// features/feature-auth/ui/src/commonMain/kotlin/.../LoginScreen.kt
// BAD: Hardcoded string literals — not translatable, not centralised.
@Composable
internal fun LoginScreen(
    state: LoginState,
    onLoginClicked: () -> Unit,
) {
    Column {
        Text(text = "Sign In") // violation — hardcoded user-visible string
        OutlinedTextField(
            value = state.email,
            onValueChange = { /* ... */ },
            label = { Text("Email address") }, // violation
            placeholder = { Text("Enter your email") }, // violation
        )
        Button(onClick = onLoginClicked) {
            Text("Login") // violation
        }
    }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../AvatarImage.kt
// BAD: Hardcoded contentDescription — inaccessible to the localisation pipeline.
@Composable
internal fun AvatarImage(avatarUrl: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = avatarUrl,
        contentDescription = "User avatar", // violation — hardcoded accessibility string
        modifier = modifier,
    )
}
```

```kotlin
// BAD: Hardcoded dialog copy.
@Composable
internal fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text("Delete account?") }, // violation
        text = { Text("This action cannot be undone.") }, // violation
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } }, // violation
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }, // violation
        onDismissRequest = onDismiss,
    )
}
```

## Severity

- `⚠️ Change requested` — Hardcoded string literal in `Text()`, button content, `contentDescription`, `placeholder`, `label`, dialog title/body, or snackbar message
- `💡 Suggestion` — Hardcoded string in a non-rendering context (e.g., a `semantics { }` block or accessibility action label) that should still be externalised for consistency
