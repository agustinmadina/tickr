---
description: Name meaningful literals, and declare compile-time constants as top-level private const rather than wrapping them in a companion object
paths:
  - "**/*.kt"
---

# Named Constants

## Rule

Two related things, both about constants:

1. **A literal that carries meaning gets a name.** A bare `3` or `'.'` tells the reader what the value is but not what it means. A named constant states the intent once, at the declaration, so every call site reads as the thing rather than the number.
2. **A compile-time constant is declared as a top-level `private const val`, not inside a `companion object`.** A companion object exists to give a type members — factories, `operator` functions, an interface implementation. Using one purely to hold `const val`s adds a nested declaration for something that has no need to be attached to the type, and it makes the constants read as part of the type's surface when they are file-local details.

This rule is about naming and declaring constants in Kotlin generally. It deliberately says nothing about **which** value to use in Compose — reaching for a design-system token instead of an ad-hoc dimension is [`composable-no-raw-dimensions.md`](composable-no-raw-dimensions.md)'s subject, and that rule owns it. A `16.dp` literal is that rule's business; a `private companion object` holding `const val SEGMENT_COUNT = 3` is this one's.

## What to Check

### MUST BLOCK

None. Both clauses are advisory — see Severity.

### MUST FLAG

- A literal with domain meaning used inline where a name would state the intent — a segment count, a retry limit, a magic string key, a separator character, a threshold
  - **Fix**: Declare a `private const val` with a name that says what the value means, and use it at the call site
- The same non-trivial literal repeated three or more times in one file
  - **Fix**: Declare it once as a named constant; three occurrences is the point at which a reader has to check whether they are the same value on purpose
- A `companion object` whose body contains **nothing but** compile-time constants
  - **Fix**: Delete the companion object and move each constant to a top-level `private const val` in the same file. Nothing else has to change: a top-level private declaration is visible to every declaration in the file, including nested classes

## Carve-outs for the companion object clause

A `companion object` is the right home for constants when:

- **The constants are part of the type's public surface** and callers reference them as `Type.CONSTANT`. Moving those to top-level would either break call sites or force a wider visibility than the file.
- **The companion holds anything besides constants** — a factory function, an `operator fun invoke`, a `Companion` that implements an interface. Then the constants are already in a home that exists for other reasons and splitting them out is churn.
- **The type is an `expect`/`actual` declaration** whose companion must match across platforms.

The clause targets exactly one shape: a companion object that exists *only* as a container for file-local constants.

## Exemptions

These are not violations and must not be flagged:

- `0`, `1`, `-1`, `2` — their meaning is carried by the literal
- Indices where a name adds nothing over the expression (`segments[0]`, `first()`)
- Test data, fixtures and `@Preview` sample values — a literal in a test *is* the specification, and naming it usually hides what the case is testing
- Values already named by a library — `HttpStatusCode.OK`, `Duration.ZERO`, enum entries
- Dimension and typography literals in Compose — owned by `composable-no-raw-dimensions.md`

## Common Mistakes

- Reaching for `private companion object` out of habit from Java's `static final`, where there was no top-level alternative. Kotlin has one, and it is the simpler declaration
- Naming a constant after its value (`const val THREE = 3`) rather than its meaning (`JWT_SEGMENT_COUNT`) — a value-named constant is the literal with extra steps
- Naming a constant that is already obvious and thereby making the call site harder to read: `if (list.size > MINIMUM_SIZE_ONE)` is worse than `if (list.size > 1)`
- Moving public constants out of a companion object to satisfy the second clause, breaking `Type.CONSTANT` call sites — check the carve-outs first
- Assuming a nested class cannot see a top-level private constant. It can; file-private means visible throughout the file

## Examples

### Good

```kotlin
// shared/identity/data/.../session/AccessTokenClaims.kt
// Constants are top-level and private: file-local details, declared as such.
private const val JWT_SEGMENT_SEPARATOR = '.'
private const val JWT_SEGMENT_COUNT = 3
private const val JWT_PAYLOAD_INDEX = 1

internal fun readIdentityIdClaim(accessToken: String): String? {
    val segments = accessToken.split(JWT_SEGMENT_SEPARATOR)
    if (segments.size != JWT_SEGMENT_COUNT) return null
    // ...
}
```

```kotlin
// A companion object that earns its place: it holds a factory, not just constants.
internal class WidgetSession private constructor(val id: String) {
    companion object {
        fun forWallet() = WidgetSession(id = "wallet")
    }
}
```

### Bad

```kotlin
// BAD: companion object used purely as a constant container. The constant is correctly
// named, but the wrapper adds a nested declaration for a file-local detail and reads as
// though SEGMENT_COUNT were part of Issued's surface.
data class Issued(val accessToken: String) : WidgetTokenEvent {

    private val isJwtShaped: Boolean
        get() = accessToken.split('.').let { segments ->   // also unnamed: what is '.' here?
            segments.size == JWT_SEGMENT_COUNT && segments.none { it.isBlank() }
        }

    private companion object {
        const val JWT_SEGMENT_COUNT = 3
    }
}
```

```kotlin
// BAD: meaningful literals inline. "session_access_token" is a storage key and 30_000 is a
// retry interval; neither says so at the call site, and the key is easy to mistype into a
// silent miss on read.
suspend fun restore() {
    val token = secretStore.get("session_access_token")
    delay(30_000)
}
```

```kotlin
// BAD: named after the value rather than the meaning — the literal with extra steps.
private const val THREE = 3
private const val DOT = '.'
```

## Severity

- `💡 Suggestion` — A meaningful literal used inline where a named constant would state the intent, or the same non-trivial literal repeated three or more times in a file
- `💡 Suggestion` — A `companion object` whose body contains nothing but compile-time constants, where a top-level `private const val` would do
