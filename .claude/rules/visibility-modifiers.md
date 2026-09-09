---
description: Enforce minimal visibility for classes, functions, and properties — especially in SDK and shared modules
paths:
  - "sharedLib/src/**/*.kt"
  - "sdks/**/*.kt"
  - "shared/**/*.kt"
  - "core/**/*.kt"
  - "features/**/*.kt"
---

# Visibility Modifier Review

## Rule

All declarations should use the most restrictive visibility modifier possible. In Kotlin, `public` is the default and the most common source of accidental API surface leakage. Every public declaration in SDK and shared modules becomes a contract that must be maintained.

## Module-Specific Rules

### SDK modules (`sdks/`) — STRICTEST
- Every `public` class, interface, function, and property is an intentional API surface
- Implementation details MUST be `internal` or `private`
- Helper/utility functions MUST be `internal` unless they are part of the documented API
- Data classes used only for internal serialization (DTOs) MUST be `internal`
- Companion object functions that are not part of the public API MUST be `private` or `internal`
- `internal` classes should NOT appear in public function signatures or return types

### Shared modules (`shared-domain/`, `shared-data/`)
- Domain models that are consumed across features should be `public`
- Implementation helpers and mappers should be `internal`
- Repository implementations should be `internal` (only the interface is public, from domain)

### Core modules (`core/`)
- Primary abstractions and interfaces: `public` (exposed as `api` dependency)
- Configuration builders and setup utilities: `public`
- Implementation details, internal helpers: `internal` or `private`

### Feature modules (`features/`)
- Everything should be `internal` by default — feature internals should not leak
- Only the DI module entry point and navigation route need to be accessible
- ViewModels, use cases, repositories: all `internal` (for ViewModel-specific Koin wiring details and examples, see `viewmodel-visibility.md`)
- **Screen `@Composable` functions**: `internal` is preferred but `public` is acceptable. Screens may need to be referenced from navigation graphs or `sharedLib` modules outside the `ui/` sub-module. Do not flag a public screen composable as a violation.
- **Ktor API client classes** (`*ApiClient`, `*ApiService`, any class that wraps Ktor `HttpClient` calls) MUST be `internal` — they are data-layer implementation details consumed only by repository implementations in the same `data/` sub-module
- **Data-layer data classes** — every `data class` defined inside a `features/*/data/` sub-module MUST be `internal`, without exception:
  - Network DTOs / response bodies (`*Json`, e.g., `LoginResponseJson`, `UserJson`)
  - Network request bodies (`*Request`, `*Body`, e.g., `LoginRequestJson`)
  - Database entities (`*Entity`, e.g., `UserEntity`, `SessionEntity`)
  - Intermediate or adapter data classes used only within the data layer (e.g., `PaginatedResultJson`, `ErrorBodyJson`)
  - All mapper classes and extension functions that convert between these types and domain models
  - The only cross-module contract from a `data/` sub-module is the domain repository **interface** (which lives in `domain/` and is `public`). No data-layer `data class` should ever be importable by `ui/`, another feature's `data/`, or any other module.
- **Koin module vals**: Layer Koin module vals in separate Gradle sub-modules (`data/`, `ui/`) MUST be `public` so the `di/` aggregator can import them via `includes()`. Only Koin vals defined within the `di/` sub-module itself can be `internal`.
- **`@Preview` functions**: Every `@Composable` function annotated with `@Preview` MUST be `private`. They are compile-time-only tooling artifacts with no runtime callers — `internal` widens visibility beyond what the tooling requires and leaks preview scaffolding into the module's API surface.
- **`PreviewParameterProvider` implementations**: MUST be `private` (preferred, when used in one file) or `internal` (when shared across files in the same module). They are preview-only sample-data classes with no framework requirement to be `public`. For the full rule, see `compose-preview-parameter-provider.md`.
- **Test classes**: Test classes in all test source sets (`commonTest/`, `androidHostTest/`, `iosTest/`, `wasmJsTest/`) MUST be `internal`. For the full rule, see `test-class-visibility.md`.

## Common Violations

- Data class with default `public` visibility that's only used within the module
- Utility/extension functions at file-level that default to `public` but are module-internal
- `object` declarations (singletons) that should be `internal`
- Constructor parameters that expose internal types through `public` data classes
- Sealed class variants that are `public` but only matched internally
- Ktor API client class (`*ApiClient`, `*ApiService`) without an explicit `internal` modifier — omitting it exposes an HTTP implementation detail as a public type that any other module can import and depend on directly, bypassing the repository interface
- `*Json` or `*Entity` `data class` in `features/*/data/` without `internal` — the DTO then becomes part of the module's public API surface, allowing `ui/` layers or other feature modules to import and depend on a network or database contract instead of a domain model
- Mapper extension function in `features/*/data/` without `internal` — a `public` mapper leaks both its receiver type (the DTO) and its return type (the domain model) as an accessible cross-module conversion utility, which implies consumers can reach the DTO directly
- `@Preview` `@Composable` function without an explicit `private` modifier — Kotlin defaults to `public`, making the preview function part of the module's API surface even though it has no runtime callers outside the IDE/tooling

## How to Check

- `public` is Kotlin's default — it does NOT need to be written explicitly. Never flag a missing `public` modifier as a violation.
- Look for classes/functions that are implicitly `public` but should be restricted — those need an explicit `internal` or `private`
- Check if implicitly-public declarations are used outside their module — if not, they should be `internal`
- Review `import` statements in other modules to verify cross-module dependencies are intentional

## Examples

### Good
```kotlin
// features/feature-auth/data/src/commonMain/kotlin/.../dto/LoginResponseJson.kt
// Network DTO is internal — ui/ and other features never import this type.
// The repository maps it to the domain model (AuthToken) before returning it to callers.
@Serializable
internal data class LoginResponseJson(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: String,
)

// features/feature-auth/data/src/commonMain/kotlin/.../dto/LoginRequestJson.kt
// Request body is also internal — the API contract is the data layer's concern, not the domain's.
@Serializable
internal data class LoginRequestJson(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

// features/feature-auth/data/src/commonMain/kotlin/.../local/SessionEntity.kt
// Database entity is internal — callers receive domain model AuthToken, never the raw DB row.
internal data class SessionEntity(
    val userId: String,
    val accessToken: String,
    val createdAt: Long,
)

// features/feature-auth/data/src/commonMain/kotlin/.../mapper/LoginMapper.kt
// Mapper is internal — only AuthRepositoryImpl calls it. No outside module needs to convert
// LoginResponseJson to AuthToken; they receive AuthToken directly from the repository.
internal fun LoginResponseJson.toDomain() = AuthToken(
    userId = userId,
    token = accessToken,
)

// features/feature-auth/data/src/commonMain/kotlin/.../AuthApiClient.kt
// Ktor HTTP wrapper is internal — only AuthRepositoryImpl in the same data/ sub-module calls it.
internal class AuthApiClient(private val httpClient: HttpClient) {
    suspend fun login(request: LoginRequestJson): LoginResponseJson { ... }
}

// features/feature-auth/data/src/commonMain/kotlin/.../AuthRepositoryImpl.kt
// The concrete implementation is internal — no consumer outside this module needs to reference it directly.
// The di/ sub-module binds it to the public AuthRepository interface via Koin.
internal class AuthRepositoryImpl(
    private val apiClient: AuthApiClient,
    private val db: AuthDatabase,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthToken> {
        val response = apiClient.login(LoginRequestJson(email, password))
        return Result.success(response.toDomain())
    }
}

// features/feature-auth/data/src/commonMain/kotlin/.../authDataModule.kt
// The Koin module val must be public so the di/ aggregator can import it via includes().
val authDataModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
}
```

### Bad
```kotlin
// BAD: LoginResponseJson is public — any module can now import this DTO and depend on
// the network contract directly instead of the domain model AuthToken.
// If the API renames "access_token" to "token", every module that imported LoginResponseJson breaks.
@Serializable
data class LoginResponseJson(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: String,
)

// BAD: SessionEntity is public — a Compose screen or another feature's data layer can now
// import the raw database row type, bypassing the domain repository entirely.
data class SessionEntity(
    val userId: String,
    val accessToken: String,
    val createdAt: Long,
)

// BAD: Mapper is public — this signals that the conversion from LoginResponseJson to AuthToken
// is an intentional cross-module utility, implying LoginResponseJson is also a stable type.
// Both are data-layer implementation details and must stay internal.
fun LoginResponseJson.toDomain() = AuthToken(userId = userId, token = accessToken)

// BAD: Missing internal on the API client — defaults to public.
// Another feature module can now import AuthApiClient directly and bypass the
// AuthRepository interface entirely, creating an uncontrolled dependency on an HTTP implementation detail.
class AuthApiClient(private val httpClient: HttpClient) {
    suspend fun login(request: LoginRequestJson): LoginResponseJson { ... }
}

// BAD: Missing internal — the repository implementation leaks as a public type.
class AuthRepositoryImpl(
    private val apiClient: AuthApiClient,
    private val db: AuthDatabase,
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthToken> { ... }
}

// BAD: @Preview function is public (Kotlin default) — the preview becomes part of the
// module's API surface even though it has no runtime callers. Any module that depends
// on this one can import and reference LoginScreenPreview, which is never intentional.
@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen(state = LoginUiState.Empty)
}

// BAD: @Preview function is internal — still wider than necessary. Preview functions
// are compile-time tooling artifacts; internal gives them cross-file visibility within
// the module for no benefit, and can mislead reviewers into thinking other files use them.
@Preview
@Composable
internal fun LoginScreenPreview() {
    LoginScreen(state = LoginUiState.Empty)
}
```

### Good (previews)
```kotlin
// GOOD: @Preview function is private — only the file that declares the Composable under
// preview needs access to the preview function. private is the most restrictive scope
// that still allows the IDE tooling to render it.
@Preview
@Composable
private fun LoginScreenPreview() {
    LoginScreen(state = LoginUiState.Empty)
}

// GOOD: Multiple previews for different states, all private.
@Preview(name = "Empty state")
@Composable
private fun LoginScreenEmptyPreview() {
    LoginScreen(state = LoginUiState.Empty)
}

@Preview(name = "Loading state")
@Composable
private fun LoginScreenLoadingPreview() {
    LoginScreen(state = LoginUiState.Loading)
}
```

## Severity

- `🚫 Blocking` — Implementation detail leaked as public in SDK modules
- `🚫 Blocking` — Repository implementation, data source, Ktor API client class (`*ApiClient`, `*ApiService`), or mapper in a feature `data/` sub-module or `shared-data/` is `public`
- `🚫 Blocking` — Any `data class` in a `features/*/data/` sub-module (`*Json`, `*Entity`, request body, response body, or any intermediate DTO) is `public` — data-layer `data class` types have no valid consumer outside the module; the domain model is the only cross-module contract
- `⚠️ Change requested` — Feature module internals (ViewModels, use cases) defaulting to public without justification
- `⚠️ Change requested` — `@Preview` `@Composable` function that is not explicitly `private` (whether it defaulted to `public` or was explicitly marked `internal`)
- `💡 Suggestion` — Shared/core declarations that could be narrowed to `internal`
