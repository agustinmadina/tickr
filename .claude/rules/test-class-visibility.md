---
description: Non-abstract test classes must be declared internal to prevent test-only code from leaking into the module's public API surface
paths:
  - "**/*Test.kt"
  - "**/*Tests.kt"
  - "**/*Spec.kt"
  - "**/test/**/*.kt"
  - "**/commonTest/**/*.kt"
  - "**/androidHostTest/**/*.kt"
  - "**/iosTest/**/*.kt"
  - "**/wasmJsTest/**/*.kt"
---

# Test Class Visibility

## Rule

Non-abstract test classes must be `internal`. Kotlin defaults to `public`, so the modifier must be written explicitly. Test-only code must never be importable by production modules or by test code in other Gradle modules that have no declared dependency on this module's test sources.

## What to Check

### MUST BLOCK

_(nothing in tests rises to build-breaking at the toolchain level — all violations are at least `⚠️ Change requested`. See Severity.)_

### MUST FLAG

- A non-abstract test class in any test source set (`commonTest/`, `androidHostTest/`, `iosTest/`, `wasmJsTest/`) that has no explicit visibility modifier (defaults to `public`)
  - **Fix**: Add `internal` before `class`
- A non-abstract test class explicitly declared `public`
  - **Fix**: Change to `internal`. There is no scenario where a concrete test class is a valid cross-module dependency
- A test helper class, fake, stub, or mock defined in a feature module's test source set without `internal`
  - **Fix**: Add `internal`. This project has no shared test-fixture module: every fake is built inside the `When` block that uses it, which is also what keeps Kotlin/Native from leaking state between scenarios
- A `companion object` inside a test class that exposes factory methods or constants without `internal` on the containing class
  - **Fix**: Make the containing class `internal`; the companion object inherits the visibility of its owner

## Common Mistakes

- Forgetting that Kotlin's default visibility is `public` — in test source sets this is just as true as in `main` source sets; the compiler does not apply any implicit narrowing
- Defining reusable fakes or test data builders directly inside a feature module's test sources and leaving them `public`
- Applying `internal` to an `object` test helper but omitting it on a nearby test `class` in the same file — check all declarations in the file, not just the first one

## Examples

### Good

```kotlin
// features/feature-auth/domain/src/commonTest/kotlin/.../LoginUseCaseTest.kt
// Non-abstract test class is internal — no other module needs to import or extend it.
internal class LoginUseCaseTest {

    private val fakeRepository = FakeAuthRepository()
    private val useCase = LoginUseCase(fakeRepository)

    @Test
    fun `returns failure when credentials are invalid`() { ... }
}

// features/feature-auth/domain/src/commonTest/kotlin/.../FakeAuthRepository.kt
// Test fake is internal — it lives in the feature module's own test sources and is only
// needed by tests within this module.
internal class FakeAuthRepository : AuthRepository {
    var shouldFail = false
    override suspend fun login(email: String, password: String): Result<AuthToken> =
        if (shouldFail) Result.failure(Exception("invalid")) else Result.success(AuthToken.fixture())
}

// features/feature-portfolio/data/src/commonTest/kotlin/.../Fakes.kt
// Even a shared base class stays internal: this project has no cross-module test-fixture
// module, so nothing outside this Gradle module ever needs to subclass it.
internal abstract class BaseRepositoryTest {
    protected val testDispatcher = StandardTestDispatcher()
    protected val testScope = TestScope(testDispatcher)

    @AfterTest
    fun tearDown() { testScope.cancel() }
}
```

### Bad

```kotlin
// BAD: No explicit visibility modifier — defaults to public.
// Any module that has a test compile dependency on this module can import LoginUseCaseTest,
// which is never intentional and pollutes the dependency graph.
class LoginUseCaseTest {
    @Test
    fun `returns failure when credentials are invalid`() { ... }
}

// BAD: Explicitly public — there is no valid reason for a concrete test class to be
// importable outside its own Gradle module.
public class LoginUseCaseTest {
    @Test
    fun `returns failure when credentials are invalid`() { ... }
}

// BAD: Test fake is public inside a feature module's test sources.
// If SessionRepositoryTest in another feature module imports this fake, it creates a hidden
// cross-module test dependency on a feature-internal type.
class FakeAuthRepository : AuthRepository {
    var shouldFail = false
    override suspend fun login(email: String, password: String): Result<AuthToken> = ...
}

// BAD: a fixture left public so another module could reach it.
// There is no shared test-fixture module here, so this only widens the surface for nothing.
abstract class BaseRepositoryTest {
    protected val testDispatcher = StandardTestDispatcher()
}
```

## Severity

- `⚠️ Change requested` — Non-abstract test class in any test source set without an explicit `internal` modifier (whether it defaulted to `public` or is explicitly `public`)
- `⚠️ Change requested` — Test helper class, fake, or stub in a feature module's test sources that is `public` and should be `internal`
