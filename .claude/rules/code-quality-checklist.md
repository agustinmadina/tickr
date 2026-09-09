# Code Quality Checklist

Actionable rules that all code-writing agents must follow. These are enforced by the PR review agent — violations will block merge.

## Visibility Modifiers

- **Feature modules**: All classes, functions, and properties default to `internal`. Only DI entry points, navigation routes, and Koin module vals in separate Gradle sub-modules (needed by the `di/` aggregator) may be `public`.
- **Data layer**: Repository implementations, data sources, mappers, and DTOs are `internal`.
- **Domain layer**: Interfaces, models, and use cases are `public` (consumed cross-module). Implementation helpers are `internal`.
- **SDK modules**: Only intentional API surface is `public`. Everything else is `internal`.
- **Rule of thumb**: `public` is Kotlin's default and does NOT need to be written explicitly. Only `internal` and `private` must be written explicitly, since omitting them would silently make a declaration public when it should be restricted.

## Domain Purity

The domain layer (`*/domain/`) must have **zero** framework imports:

- **No DI**: No `org.koin.*`, no `dagger.*`, no `javax.inject.*`
- **No networking**: No `io.ktor.*`
- **No persistence**: No `com.russhwolf.settings.*`
- **No platform types**: No `android.*`, no `platform.Foundation.*`
- **No serialization annotations**: No `@Serializable` on domain models (that belongs on DTOs in data layer)
- Domain layer depends only on: Kotlin stdlib, kotlinx.coroutines, and other domain modules

## Thread Safety (commonMain)

These are **blocking** violations in `commonMain`:

- **No `Dispatchers.IO`** — JVM-only, won't compile on iOS. Use `Dispatchers.Default` or inject a dispatcher.
- **No `runBlocking`** — Blocks the thread, can deadlock. Use `suspend` functions or `coroutineScope { }`.
- **No `GlobalScope`** — Leaks coroutines. Use scoped `CoroutineScope` or `viewModelScope`.
- **No `Thread.sleep()`** — JVM-only. Use `delay()`.
- **No `synchronized`** — JVM-only. Use `Mutex` from `kotlinx.coroutines.sync`.
- **No `Dispatchers.Unconfined`** — Unless in tests or with explicit justification.

## Null Safety

- **No `!!` operator** — Use `?.let { }`, `checkNotNull()`, `requireNotNull()`, or `?: default` instead.
- Exception: Test code where `!!` is acceptable for concise assertions.

## Koin Dependency Injection

- **Use constructor injection** — Classes receive dependencies via constructor parameters.
- **No service locator** — Never use `KoinPlatform.getKoin().get<T>()` in production code.
- **Compose**: Use `koinInject<T>()` or `koinViewModel<T>()`, not manual `getKoin()` calls.
- **Core library modules**: Use `get()` in Koin module definitions, never `androidContext()` (requires `koin-android`).

## Platform Parity

- Every `expect` declaration must have `actual` implementations for **all three targets**: Android, iOS, Desktop.
- `actual` implementations should be semantically equivalent across platforms.
- If platform behavior differs, add tests in each platform's test source set.

## Flow & Coroutines

- Use `flowOn()` for upstream dispatcher changes, not `withContext` wrapping `flow { }`.
- Use `SharingStarted.WhileSubscribed(5000)` for UI-bound `StateFlow` (allows resubscription without re-fetch).
- Use `SharingStarted.Lazily` for data that should survive configuration changes.
- `StateFlow` and `SharedFlow` for state management, not `MutableLiveData`.

## Import Ordering (ktlint)

- Imports must follow ASCII sort order (enforced by ktlint).
- No wildcard imports (`*`).
- Run `./gradlew ktlintFormat` before committing to auto-fix.

## Spelling & Typos

- Typos in code, comments, strings, and configuration files are **blocking** (`⚠️ Change requested`).
- This includes variable names, function names, log messages, error messages, YAML comments, and documentation within code files.
- The PR review agent must flag every typo and request a fix before approving.

## Test Isolation (KMP / Kotlin Native)

- **Never use `IsolationMode`** (`InstancePerTest`, `InstancePerLeaf`) in `commonTest`. These modes are silently ignored on Kotlin/Native — tests always run as `SingleInstance` on iOS, causing state pollution between `When` blocks and iOS-only failures.
- **Create mutable state inside `When` blocks**, not at `Given` or spec level. This guarantees each scenario starts clean on all platforms:
  ```kotlin
  // WRONG — shared across When blocks on native
  Given("a store") {
      val store = InMemoryStore()
      When("...") { store.put("k", "v") }
      When("...") { store.get("k") shouldBe null }  // fails on iOS!
  }

  // CORRECT — each When gets a fresh instance
  Given("a store") {
      When("...") { val store = InMemoryStore(); store.put("k", "v") }
      When("...") { val store = InMemoryStore(); store.get("k") shouldBe null }
  }
  ```
- Immutable/read-only values (enums, configs, routers with no state) may be declared at `Given` level.

## General

- Prefer editing existing files over creating new ones.
- Don't add features, refactoring, or improvements beyond what was asked.
- Keep solutions minimal — the right amount of complexity is the minimum needed for the current task.
