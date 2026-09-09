---
description: Every catch block and onFailure handler must log the exception using Kermit logger.e(throwable) { "description" } — silently swallowed exceptions are forbidden
paths:
  - "**/*.kt"
---

# Error Logging Requirement

## Rule

Every `catch` block and every `onFailure { }` handler that does not rethrow the exception must log it using Kermit at error level before taking any other action. Silently swallowing exceptions destroys the ability to diagnose production failures — a crash or a broken UI state with no log entry is impossible to root-cause.

The required logging pattern is:

```kotlin
logger.e(throwable) { "Human-readable description of what was being attempted" }
```

The `Throwable` must be passed as the **first positional argument** to preserve the stack trace in the log output. Logging only the message string loses the stack trace.

**Logger declaration pattern** (for ViewModels, use the one already in `BaseViewModel`):

```kotlin
private val log = Logger.withTag("ClassName")
```

For classes outside `BaseViewModel`, declare `log` as a `private val` with `Logger.withTag("ClassName")` from `co.touchlab.kermit.Logger`.

## What to Check

### MUST BLOCK

- An empty `catch` block — swallows the exception with zero signal
  - **Fix**: Add `log.e(e) { "Description of the operation that failed" }` as the first statement
- An empty `onFailure { }` lambda — same as an empty catch
  - **Fix**: Add `log.e(it) { "Description" }` before updating state or taking any other action
- A `catch` or `onFailure` that updates state with an error message but never logs the exception
  - **Fix**: Add `log.e(throwable) { "description" }` before the state update so the diagnostic information is available in crash reporting and logcat
- A `catch (e: CancellationException)` that does not rethrow
  - **Fix**: Always rethrow `CancellationException` — catching and swallowing it breaks structured concurrency and prevents parent coroutines from cancelling correctly: `catch (e: CancellationException) { throw e }`

### MUST FLAG

- `logger.e { e.message }` or `log.e { throwable.toString() }` — the message lambda variant without a `Throwable` first argument loses the stack trace
  - **Fix**: Change to `log.e(e) { "description" }` — the `Throwable` must be the first positional argument
- An exception logged at `d` (debug) or `i` (info) level instead of `e` (error) level when the catch block handles an error condition
  - **Fix**: Use `log.e(throwable) { "description" }` for error conditions; `d` and `i` are for expected, non-error events

## Exemptions

- `catch (e: CancellationException) { throw e }` — rethrow immediately, no logging needed
- Test source sets (`commonTest`, `androidHostTest`, `iosTest`, `wasmJsTest`) — test code may use `assertFailsWith` and other patterns without logging
- A `catch` block that converts the exception into a `Result.failure(e)` and returns it to the caller — the caller is responsible for handling and logging the failure; the conversion itself does not need to log. Note: new repositories and data sources do not use this pattern at all — they rethrow (see `repository-error-propagation.md`); this exemption applies to pre-existing call sites

## Common Mistakes

- Empty `catch (e: Exception) { }` blocks from auto-generated code left in place — these silently hide all exceptions
- `onFailure { updateState { it.copy(errorMessage = it.message.orEmpty()) } }` without a log — the user sees an error message but there is no log entry for the engineering team to diagnose
- `log.e { e.message }` — this logs only the message string, which omits the exception type, stack trace, and cause chain that are essential for diagnosis
- Catching `CancellationException` inside a coroutine and logging it as an error — `CancellationException` is a normal part of coroutine lifecycle; rethrowing without logging is correct
- Using `println(e)` or `System.err.println(e)` instead of Kermit — platform-specific output APIs are not routed through Kermit and are invisible in crash reporting

## Examples

### Good

```kotlin
// features/feature-auth/data/src/commonMain/kotlin/.../AuthRepositoryImpl.kt
// GOOD: Exception logged with Throwable, then rethrown — the repository throws
// (see repository-error-propagation.md); Result wrapping happens once, in UseCase.invoke().
internal class AuthRepositoryImpl(
    private val apiClient: AuthApiClient,
) : AuthRepository {

    private val log = Logger.withTag("AuthRepositoryImpl")

    override suspend fun login(email: String, password: String): AuthToken =
        try {
            apiClient.login(LoginRequestJson(email, password)).toDomain()
        } catch (e: CancellationException) {
            throw e // rethrow cancellation first, never logged
        } catch (e: Exception) {
            log.e(e) { "Login request failed for email=$email" } // Throwable as first arg
            throw e
        }
}
```

```kotlin
// features/feature-profile/ui/src/commonMain/kotlin/.../ProfileViewModel.kt
// GOOD: BaseViewModel provides `log` via Logger.withTag(this::class.simpleName).
// onFailure logs before updating state — stack trace preserved.
internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : BaseViewModel<ProfileAction, ProfileEffect, ProfileState>(ProfileState()) {

    override suspend fun handleAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Load -> {
                getUserProfileUseCase()
                    .onSuccess { profile ->
                        updateState { it.copy(profile = profile.toUi(), isLoading = false) }
                    }
                    .onFailure { error ->
                        log.e(error) { "Failed to load user profile" } // log before state update
                        updateState { it.copy(isLoading = false, errorMessage = error.message.orEmpty()) }
                    }
            }
        }
    }
}
```

```kotlin
// GOOD: CancellationException is rethrown immediately — no logging, no swallowing.
try {
    performLongOperation()
} catch (e: CancellationException) {
    throw e // correct — preserve structured concurrency
} catch (e: Exception) {
    log.e(e) { "Long operation failed" }
    updateState { it.copy(errorMessage = e.message.orEmpty()) }
}
```

### Bad

```kotlin
// BAD: Empty catch block — exception vanishes without a trace.
try {
    performOperation()
} catch (e: Exception) {
    // violation — silent swallow; add log.e(e) { "description" }
}
```

```kotlin
// BAD: onFailure updates state but never logs — user sees error, engineering team has no log.
getUserProfileUseCase()
    .onFailure { error ->
        // violation — no logging before state update
        updateState { it.copy(errorMessage = error.message.orEmpty()) }
    }
```

```kotlin
// BAD: Logging without Throwable argument — stack trace lost.
} catch (e: Exception) {
    log.e { e.message } // violation — Throwable must be first positional argument
    log.e { e.toString() } // violation — same problem; use log.e(e) { "description" }
}
```

```kotlin
// BAD: Exception logged at debug level — invisible in production log filters.
} catch (e: NetworkException) {
    log.d { "Network error: ${e.message}" } // violation — use log.e(e) { "..." }
}
```

```kotlin
// BAD: CancellationException caught and swallowed — breaks structured concurrency.
} catch (e: CancellationException) {
    log.e(e) { "Operation cancelled" } // violation — must rethrow, not log
}
```

## Severity

- `🚫 Blocking` — Empty `catch` block or empty `onFailure { }` lambda with no logging and no rethrow
- `🚫 Blocking` — `catch (e: CancellationException)` that does not rethrow
- `⚠️ Change requested` — `catch` or `onFailure` that updates state or takes action without logging the exception first
- `⚠️ Change requested` — `log.e { throwable.message }` or similar patterns that omit the `Throwable` first argument and lose the stack trace
- `💡 Suggestion` — Exception logged at `d` or `i` level when `e` (error) level is more appropriate for the error condition
