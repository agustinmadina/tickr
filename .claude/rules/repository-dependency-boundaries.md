---
description: Enforce that repository classes depend only on lower-level data-layer components, never on other repositories
paths:
  - "features/**/data/**/*.kt"
  - "shared/**/data/**/*.kt"
  - "sdks/**/data/**/*.kt"
---

# Repository Dependency Boundaries

## Rule

Repository implementations must never accept another repository as a constructor parameter. Repositories sit at the top of the data layer — they are the single integration point between the domain layer and data sources. Injecting one repository into another creates tight coupling, obscures data flow, and violates the single-responsibility principle.

## What to Check

### MUST BLOCK

- A repository constructor that takes any type whose name ends in `Repository` as a parameter — this is always a layering violation
  - **Fix**: Identify the shared data need and extract it into a common data source, API client, DAO, or manager that both repositories can depend on independently
- A repository that delegates core business logic to another repository rather than to a data source
  - **Fix**: Move the shared logic into a data source or manager class that both repositories depend on directly

### MUST FLAG

- A repository that calls `suspend` functions or collects `Flow` from a type that is itself a repository (even if injected via its interface)
  - **Fix**: Extract a shared `*DataSource`, `*ApiClient`, or `*Manager` and inject that into both repositories instead

> Use case types appearing as constructor parameters in any data-layer class are covered in full by `data-layer-use-case-prohibition.md`.

## Allowed Dependencies for Repository Implementations

Repository implementations in `features/*/data/`, `shared/shared-data/`, and `sdks/*/data/` may ONLY depend on:

| Dependency type | Examples |
|---|---|
| Remote data sources | `*RemoteDataSource`, `*ApiClient`, `*ApiService` |
| Local data sources | `*LocalDataSource`, `*Store` |
| Managers / coordinators | `*Manager`, `*Store`, `*Cache` |
| Mappers | `*Mapper`, `*DtoMapper` |
| Core infrastructure | `HttpClient` (Ktor), `Settings` (multiplatform-settings), `DispatcherProvider` |
| Domain interfaces (self) | The repository interface it implements — for type declaration only |
| Kotlin / coroutines | `CoroutineScope`, `CoroutineDispatcher` |

## Common Mistakes

- Injecting `UserRepository` into `ProfileRepository` because profile data requires a user ID — instead, inject the shared `UserLocalDataSource` or a `UserDao` so both repositories read user data independently
- Injecting `AuthRepository` into `SessionRepository` to check login state — instead, extract a `TokenStore` or `AuthLocalDataSource` that both can query
- Using a repository as a shortcut to avoid writing a new data source class — each distinct storage or network concern deserves its own data source

## Examples

### Bad

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../ProfileRepositoryImpl.kt
// BAD: UserRepository is another repository — this creates a hidden cross-repository dependency
// and makes ProfileRepositoryImpl impossible to test in isolation.
internal class ProfileRepositoryImpl(
    private val profileApiClient: ProfileApiClient,
    private val userRepository: UserRepository, // violation
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<Profile> {
        val user = userRepository.getUser(userId).getOrThrow() // pulling from another repo
        return profileApiClient.fetchProfile(user.id)
    }
}
```

```kotlin
// features/feature-session/data/src/commonMain/kotlin/.../SessionRepositoryImpl.kt
// BAD: AuthRepository is injected here — the session layer now depends on the auth layer
// at the data level, coupling two feature modules' internals together.
internal class SessionRepositoryImpl(
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val authRepository: AuthRepository, // violation
) : SessionRepository {

    override fun isSessionActive(): Flow<Boolean> =
        authRepository.observeAuthState().map { it.isLoggedIn }
}
```

### Good

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../ProfileRepositoryImpl.kt
// GOOD: Both the remote API client and a local user data source are lower-level components.
// ProfileRepositoryImpl has no knowledge of any other repository.
internal class ProfileRepositoryImpl(
    private val profileApiClient: ProfileApiClient,
    private val userLocalDataSource: UserLocalDataSource, // data source, not a repository
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<Profile> {
        val userId = userLocalDataSource.getCurrentUserId()
        return profileApiClient.fetchProfile(userId)
    }
}
```

```kotlin
// features/feature-session/data/src/commonMain/kotlin/.../SessionRepositoryImpl.kt
// GOOD: SessionRepositoryImpl depends on a TokenStore (a focused lower-level component),
// not on AuthRepository. AuthRepository can also depend on TokenStore independently.
internal class SessionRepositoryImpl(
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val tokenStore: TokenStore, // focused manager, not a repository
) : SessionRepository {

    override fun isSessionActive(): Flow<Boolean> =
        tokenStore.observeToken().map { it != null }
}
```

```kotlin
// features/feature-checkout/domain/src/commonMain/kotlin/.../PlaceOrderUseCase.kt
// GOOD: Cross-repository orchestration belongs in the use case (domain layer),
// not inside a repository. The use case calls both repositories; neither calls the other.
// NOTE: In production code, PlaceOrderUseCase must extend UseCase<PARAMS, RESULT> from
// dev.madina.tickr.core.domain.usecase — see use-case-pattern.md for the required structure.
class PlaceOrderUseCase(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    dispatcher: CoroutineDispatcher,
) : UseCase<PlaceOrderUseCase.Params, Order>(dispatcher) {

    data class Params(val userId: String)

    override suspend fun execute(parameters: Params): Order {
        val cart = cartRepository.getCart(parameters.userId).getOrThrow()
        return orderRepository.createOrder(cart).getOrThrow()
    }
}
```

## Severity

- `🚫 Blocking` — Repository constructor contains a parameter whose type is another repository (interface or implementation)
- `💡 Suggestion` — Repository with more than two data-source dependencies that could indicate it is doing too much and should be split
