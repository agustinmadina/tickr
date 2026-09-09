---
description: Network-layer tests in commonTest must use shared core-testing helpers (buildMockHttpClient, jsonMockHttpClient) — hand-rolled MockEngine setups and MockWebServer in commonTest are banned
paths:
  - "**/commonTest/**/*.kt"
  - "**/*Test.kt"
  - "**/build.gradle.kts"
---

# Mock HTTP Client Test Helpers

## Rule

Network-layer tests that stub HTTP responses — repository implementations, data sources, and API clients in `commonTest` — must use the shared helpers from `core/core-testing`: `buildMockHttpClient`, `respondJson`, and `jsonMockHttpClient` (all in `dev.madina.tickr.core.testing.network`). Hand-rolling `HttpClient(MockEngine { … }) { install(ContentNegotiation) { json(…) } }` inline is banned: it duplicates ~20 lines per file and, more critically, silently drifts from the production JSON configuration (`ignoreUnknownKeys = true`, `isLenient = true`) — causing tests to pass or fail on inputs that production handles differently.

## What to Check

### MUST BLOCK

- `MockEngine` used in `commonTest` without going through the shared helpers — i.e. a test file that constructs `HttpClient(MockEngine { … })` inline and manually installs `ContentNegotiation` and `json { … }`
  - **Fix**: Replace the inline stanza with `buildMockHttpClient { request -> … }` (when request assertions are needed) or `jsonMockHttpClient(body, status)` (when only response mapping is tested). Both are in `dev.madina.tickr.core.testing.network`
- `okhttp3.mockwebserver.MockWebServer` used inside a `commonTest` source set
  - **Fix**: `MockWebServer` is JVM-only — it will not compile on iOS. Use `buildMockHttpClient` instead. `MockWebServer` is only acceptable in `desktopTest` or `androidUnitTest` for real-socket integration tests that must run on a JVM; it must never be the sole HTTP stubbing mechanism for a test that is also expected to pass on iOS

### MUST FLAG

- A test file in `commonTest` that declares a private helper which itself builds a `MockEngine`-backed `HttpClient` (the pre-refactor pattern — e.g. a private `buildTestClient()` that replicates the production JSON config)
  - **Fix**: Delete the private helper and call `buildMockHttpClient` / `jsonMockHttpClient` directly. The shared helper is already a private-builder equivalent, available to the whole project at zero duplication cost
- A feature or shared module whose `commonTest` dependencies include a direct `implementation(libs.ktor.client.mock)` entry when the module already depends on `project(":core:core-testing")` in `commonTest`
  - **Fix**: Remove the direct `ktor.client.mock` entry. `core-testing` exposes `ktor-client-mock`, `ktor-client-core`, `ktor-client-content-negotiation`, and `ktor-serialization-kotlinx-json` as `api` dependencies — the mock stack is already on the test classpath transitively
- Request body assertions made by reading `response.bodyAsText()` or re-parsing the response, when the test actually needs to inspect the _outbound_ request
  - **Fix**: Use `buildMockHttpClient { request -> val body = request.body.toByteArray().decodeToString(); … }` to capture the outbound body before responding

## Exceptions

A test that must exercise a **non-standard client configuration** (e.g. testing `ContentNegotiation` edge cases themselves, verifying behaviour when `ignoreUnknownKeys = false`, or testing `core-network` internals that underpin the shared helpers) may build its own `HttpClient` inline. The test file must include a short comment on the custom client explaining why the shared helper does not fit:

```kotlin
// Custom client: testing strict JSON parsing (ignoreUnknownKeys = false) — shared
// buildMockHttpClient uses lenient config by design, which would hide this failure.
val client = HttpClient(MockEngine { … }) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = false }) }
}
```

`core-network`'s own unit tests (`NetworkModuleTest`, `HttpClientFactoryTest`, etc.) are also exempt — they predate the helpers and test the production client configuration itself.

## Common Mistakes

- Copying the `HttpClient(MockEngine { … }) { install(ContentNegotiation) { json { … } } }` boilerplate from an existing test file that predates the helpers — delete the boilerplate and use `buildMockHttpClient` instead
- Omitting `ignoreUnknownKeys = true` from a hand-rolled JSON config, then writing test JSON that matches the API exactly — the test passes but production code sees extra fields from the real API and throws `SerializationException`. The shared helpers prevent this class of divergence
- Using `jsonMockHttpClient` when the test also needs to assert on the outbound request body — `jsonMockHttpClient` discards the request; use `buildMockHttpClient { request -> … }` to capture it
- Adding `implementation(libs.ktor.client.mock)` to `commonTest.dependencies` in a module that already depends on `project(":core:core-testing")` — `core-testing` already exposes the full mock stack as `api`; the explicit dependency is redundant and flags drift from the shared convention
- Using `MockWebServer` in `commonTest` "just for one integration test" — the file will compile on JVM but fail on iOS CI. Keep `MockWebServer` confined to `desktopTest` / `androidUnitTest` source sets

## Examples

### Good

```kotlin
// shared/identity/data/src/commonTest/kotlin/com/example/app/identity/data/repository/IdpUserRepositoryImplTest.kt
// GOOD: private helpers delegate directly to the shared core-testing helpers.
// jsonMockHttpClient is used when only response mapping is tested;
// buildMockHttpClient is used when the outbound request body must be captured.
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import dev.madina.tickr.core.testing.network.buildMockHttpClient
import dev.madina.tickr.core.testing.network.jsonMockHttpClient
import dev.madina.tickr.core.testing.network.respondJson

internal class IdpUserRepositoryImplTest {

    // One-liner repo builder for response-mapping tests — no request assertions needed.
    private fun buildRepo(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): IdpUserRepositoryImpl =
        IdpUserRepositoryImpl(IdentityUserApiClient(jsonMockHttpClient(responseBody, status)))

    // Repo builder that captures the outbound request body for request-shape tests.
    private fun buildRepoCapturingBody(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onBody: suspend (String) -> Unit,
    ): IdpUserRepositoryImpl {
        val client = buildMockHttpClient { request ->
            onBody(request.body.toByteArray().decodeToString())
            respondJson(responseBody, status)
        }
        return IdpUserRepositoryImpl(IdentityUserApiClient(client))
    }

    @Test
    fun getUser_returns_mapped_user_on_200() = runTest {
        val repo = buildRepo("""{"id":"user-001","client_id":"client-abc","username":"johndoe"}""")
        val result = repo.getUser()
        assertTrue(result.isSuccess)
        assertEquals("user-001", result.getOrThrow().id)
    }

    @Test
    fun upsertIdentifier_includes_identifier_type_in_body() = runTest {
        var capturedBody = ""
        val repo = buildRepoCapturingBody("", HttpStatusCode.Created) { body ->
            capturedBody = body
        }
        repo.upsertIdentifier(identifierType = EMAIL, value = "new@example.com")
        assertTrue(capturedBody.contains("\"identifier_type\""))
    }
}
```

```kotlin
// shared/identity/data/build.gradle.kts — commonTest block
// GOOD: No direct ktor-client-mock dep — core-testing exposes it transitively via api().
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.kotlinx.coroutinesTest)
    implementation(project(":core:core-testing")) // brings ktor-client-mock, content-negotiation, etc.
}
```

### Bad

```kotlin
// features/feature-payments/data/src/commonTest/.../PaymentsRepositoryImplTest.kt
// BAD: Hand-rolled MockEngine + ContentNegotiation stanza inline.
// (1) Duplicates ~20 lines per file.
// (2) Json config omits ignoreUnknownKeys = true — tests pass on trimmed JSON but production
//     API response with extra fields would throw SerializationException at runtime.
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal class PaymentsRepositoryImplTest {

    // BAD: private builder that replicates what buildMockHttpClient already does.
    private fun buildTestClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
        HttpClient(MockEngine {
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) {
                // BAD: missing ignoreUnknownKeys = true — silent divergence from production
                json(Json { isLenient = true })
            }
        }

    @Test
    fun getWalletBalance_returns_mapped_balance() = runTest {
        val client = buildTestClient("""{"balance":"100.00","currency":"USD"}""")
        val repo = PaymentsRepositoryImpl(PaymentsApiClient(client))
        val result = repo.getWalletBalance()
        assertTrue(result.isSuccess)
    }
}
```

```kotlin
// features/feature-payments/data/build.gradle.kts
// BAD: direct ktor-client-mock dep alongside core-testing — redundant and signals
// that the test file is hand-rolling instead of using the shared helpers.
commonTest.dependencies {
    implementation(libs.kotlin.test)
    implementation(project(":core:core-testing"))
    implementation(libs.ktor.client.mock) // violation — already transitive from core-testing
}
```

```kotlin
// features/feature-activity/data/src/commonTest/.../ActivityRepositoryImplTest.kt
// BAD: MockWebServer in commonTest — JVM-only, will not compile for iOS targets.
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

internal class ActivityRepositoryImplTest {

    private val server = MockWebServer() // violation — use buildMockHttpClient in commonTest

    @Test
    fun fetchActivity_returns_mapped_list() = runTest {
        server.enqueue(MockResponse().setBody("""[{"id":"act-001"}]"""))
        server.start()
        val repo = ActivityRepositoryImpl(ActivityApiClient(server.url("/").toString()))
        // ...
        server.shutdown()
    }
}
```

## Severity

- `🚫 Blocking` — `MockWebServer` used in a `commonTest` source set (JVM-only; will not compile on iOS)
- `⚠️ Change requested` — Hand-rolled `HttpClient(MockEngine { … }) { install(ContentNegotiation) { json(…) } }` inline in `commonTest` where `buildMockHttpClient` or `jsonMockHttpClient` would suffice
- `⚠️ Change requested` — Private test-file helper in `commonTest` that replicates the `MockEngine` + `ContentNegotiation` setup instead of delegating to the shared helpers
- `💡 Suggestion` — Direct `implementation(libs.ktor.client.mock)` in a module's `commonTest` dependencies when the module already depends on `project(":core:core-testing")`
