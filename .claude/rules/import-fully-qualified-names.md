---
description: Class and function references must always use short names via import statements — inline fully qualified names are banned
paths:
  - "features/**/*.kt"
  - "core/**/*.kt"
  - "shared/**/*.kt"
  - "sdks/**/*.kt"
---

# Import Style — No Fully Qualified Names

## Rule

Class and function references in Kotlin source files must always use their short names via a proper `import` statement. Writing a fully qualified name inline (e.g., `kotlin.collections.listOf(...)`, `android.util.Log.d(...)`) is banned — it adds noise that obscures intent and makes code harder to scan.

Note: This rule covers general types, functions, and companion object constants. Enum and sealed-class entries are governed by the more specific rule in `enum-import-style.md`. Wildcard imports (`import pkg.*`) remain banned per `code-quality-checklist.md`.

## What to Check

### MUST BLOCK

- Any inline fully qualified class reference in a type annotation, variable declaration, or function signature where an `import` could resolve the short name
  - **Fix**: Add an `import` statement for the type and use its short name at the call site
- Any inline fully qualified function call (e.g., `kotlin.collections.listOf(...)`, `kotlinx.coroutines.flow.flowOf(...)`)
  - **Fix**: Add a top-level `import` for the function and use the short name
- Inline platform package references in `commonMain` source (e.g., `android.util.Log`, `platform.Foundation.NSLog`) — these are both a fully-qualified-name violation and a KMP platform-purity violation
  - **Fix**: Move platform-specific calls to `actual` implementations and import normally within each platform source set

### MUST FLAG

- A companion object constant or `object` declaration member accessed with the containing class qualifier inline (e.g., `HttpStatusCode.OK` repeated five or more times in the same file) — add a static import when the short name (`OK`) is unambiguous in context
  - **Acceptable**: Using the qualified form once or twice in a file is fine for clarity; static import is preferred when the same qualifier appears three or more times
  - **Fix**: Add `import io.ktor.http.HttpStatusCode.OK` and use the unqualified `OK` at each repeated call site
- A top-level extension function called with its package prefix (e.g., `dev.madina.tickr.core.common.extensions.toIsoString(date)`) — extension functions are designed to be called as member syntax, not as fully qualified free functions
  - **Fix**: Add the import and call as `date.toIsoString()`

## Common Mistakes

- Letting the IDE insert a fully qualified name when it cannot find an existing import (e.g., when two classes share a short name) — the correct fix is to import the one actually needed and rename or qualify only the other; do not leave both as inline fully qualified references
- Leaving a fully qualified name after moving a class to a new package — the file still compiles but now contains a stale qualified reference that skips the import block entirely, making the dependency invisible
- Using a fully qualified name to avoid an import conflict instead of using a type alias (`typealias NetworkResult = com.example.http.Result`) — the type alias approach is cleaner and keeps call sites readable
- Confusing this rule with the wildcard ban — `import dev.madina.tickr.core.common.extensions.*` is banned; `import dev.madina.tickr.core.common.extensions.toIsoString` is required

## Examples

### Good

```kotlin
// features/feature-auth/data/src/commonMain/kotlin/.../AuthRepositoryImpl.kt
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withContext
import dev.madina.tickr.core.common.DispatcherProvider

internal class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val dispatchers: DispatcherProvider,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        withContext(dispatchers.io) {
            val response = httpClient.post("auth/login") { ... }
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body<UserDto>().toDomain())
            } else {
                Result.failure(AuthException("Login failed"))
            }
        }
}
```

```kotlin
// core/core-common/src/commonMain/kotlin/.../DateExtensions.kt
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Instant.toDisplayString(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).toString()
```

```kotlin
// When two classes share a short name, import one and type-alias the other
import io.ktor.http.HttpStatusCode
import dev.madina.tickr.core.domain.Result as DomainResult

fun mapStatus(status: HttpStatusCode): DomainResult<Unit> = ...
```

### Bad

```kotlin
// BAD: fully qualified class reference in type annotation — add an import instead
internal class AuthRepositoryImpl(
    private val httpClient: io.ktor.client.HttpClient, // violation
    private val dispatchers: dev.madina.tickr.core.common.DispatcherProvider, // violation
) : AuthRepository { ... }
```

```kotlin
// BAD: fully qualified function call — import the function and use the short name
val items = kotlin.collections.listOf("a", "b", "c") // violation
val flow = kotlinx.coroutines.flow.flowOf(1, 2, 3)   // violation
```

```kotlin
// BAD: platform package reference inline in commonMain
// This is both a fully-qualified-name violation and a KMP platform-purity violation.
fun logDebug(tag: String, msg: String) {
    android.util.Log.d(tag, msg) // violation — platform type in commonMain
}
```

```kotlin
// BAD: fully qualified companion object constant repeated across the file
// Import HttpStatusCode.OK and HttpStatusCode.Unauthorized as static imports instead.
if (response.status == io.ktor.http.HttpStatusCode.OK) { ... }          // violation
if (response.status == io.ktor.http.HttpStatusCode.Unauthorized) { ... } // violation
```

## Exceptions

- Two types with identical short names are both needed in the same file and neither is clearly dominant — import the primary type normally and qualify the secondary type inline, or use `typealias` for the secondary type
- IDE-generated code or auto-scaffolded files that have not yet been reviewed — fix during the first substantive edit to the file
- KDoc `@see` and `@throws` tags may use fully qualified names when the referenced type is not imported in the source file (these are doc-only, not compiled references)

## Severity

- `🚫 Blocking` — Inline platform package reference (`android.*`, `platform.Foundation.*`) in `commonMain` source
- `⚠️ Change requested` — Any other inline fully qualified class or function reference that could be resolved with a proper `import` statement
- `💡 Suggestion` — Companion object constant or top-level extension function called with its qualifier at three or more call sites in the same file; prefer a static import
