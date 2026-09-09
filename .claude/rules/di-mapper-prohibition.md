---
description: Mapper classes and mapper extension functions must never be registered in Koin — mappers are stateless pure functions called directly at the conversion boundary
paths:
  - "features/*/di/**/*.kt"
  - "shared/*/di/**/*.kt"
  - "sdks/*/di/**/*.kt"
---

# DI Mapper Prohibition

## Rule

Mappers are stateless pure functions — they take an input and return an output with no side effects and no dependencies on external services. Registering a mapper in Koin adds it to the dependency graph unnecessarily, forces injection boilerplate at every caller, and obscures the fact that the mapper has no real dependencies of its own.

Call mapper functions directly at the conversion boundary — either as extension functions (`userJson.toDomain()`) or as static-style functions on a companion object / standalone function. Do not register mapper classes via `single { }` or `factory { }` in any Koin module, and do not accept a `*Mapper` type as a constructor parameter.

## What to Check

### MUST BLOCK

- A `single { XMapper() }` or `factory { XMapper() }` registration in any Koin module
  - **Fix**: Remove the registration. Call the mapper's functions directly at the conversion boundary (e.g., `userJson.toDomain()` or `UserMapper.toDomain(userJson)`)
- A repository implementation, data source, or use case constructor that accepts a `*Mapper` type as a parameter
  - **Fix**: Remove the mapper parameter. Call the mapper function inline at the point where mapping occurs

### MUST FLAG

- An `object` that contains only mapping functions registered in Koin (e.g., `single { UserMapper }`)
  - **Fix**: Remove the Koin registration. `object` mappers are already singletons by Kotlin's `object` semantics — there is no need to register them in Koin
- A repository that stores a mapper as a `private val` injected from the constructor when the mapper function could be called inline
  - **Fix**: If the mapper has no constructor dependencies (i.e., it is a pure function), remove the `private val` and call the function directly

## Exceptions

A mapper class with a genuine constructor dependency (e.g., a `ClockProvider` for timestamp formatting, or a locale-aware formatter) may be registered in Koin as a `single { }` or `factory { }`. The dependency must be real — not a convenience injection. Document the reason with a comment on the Koin registration explaining why the mapper cannot be a pure function.

## Common Mistakes

- Registering a mapper "for testability" — pure functions with no external dependencies do not need to be mocked; test the mapper function directly with unit tests
- Creating a `class UserMapper(private val clockProvider: ClockProvider)` that takes a real dependency and registering it in Koin — this pattern is acceptable only when the mapper genuinely needs a collaborator (e.g., a formatter or clock); document the reason with a comment
- Accepting a mapper as a constructor parameter in a repository so that tests can inject a fake mapper — this is unnecessary; test the repository with the real mapper and test the mapper separately
- Registering an extension function file as an `object` in Koin to "namespace" the mappers — extension functions are already namespaced by the receiver type and require no Koin involvement

## Examples

### Good

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../mapper/UserMapper.kt
// GOOD: Mapper is a top-level extension function. Called directly at the conversion boundary.
// No Koin registration needed — stateless, no dependencies.
internal fun UserJson.toDomain() = User(
    id = userId,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role.toDomain(),
)

internal fun UserRoleJson.toDomain() = when (this) {
    UserRoleJson.ADMIN -> UserRole.ADMIN
    UserRoleJson.MEMBER -> UserRole.MEMBER
    UserRoleJson.GUEST -> UserRole.GUEST
}
```

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../ProfileRepositoryImpl.kt
// GOOD: Mapper called inline — no mapper constructor parameter, no Koin involvement.
internal class ProfileRepositoryImpl(
    private val profileApiClient: ProfileApiClient,
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<User> =
        runCatching { profileApiClient.fetchUser(userId).toDomain() }
}
```

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../profileDataModule.kt
// GOOD: Only the repository is registered. No mapper appears in the Koin module.
val profileDataModule = module {
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
}
```

### Bad

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../mapper/UserMapper.kt
// BAD: Mapper registered as a class so it can be injected via Koin.
// Mappers are stateless — they have no dependencies and do not need to be in the DI graph.
internal class UserMapper {
    fun toDomain(json: UserJson) = User(
        id = json.userId,
        displayName = json.displayName,
        avatarUrl = json.avatarUrl,
        role = json.role.toDomain(),
    )
}
```

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../profileDataModule.kt
// BAD: UserMapper registered in Koin — adds a stateless pure function to the DI graph
// for no reason. Every consumer must now declare a dependency on UserMapper.
val profileDataModule = module {
    factory { UserMapper() } // violation — mappers must not be registered in Koin
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
}
```

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/.../ProfileRepositoryImpl.kt
// BAD: Mapper injected as a constructor parameter — adds unnecessary DI ceremony.
// The mapper has no real dependencies; calling it inline is simpler and equally testable.
internal class ProfileRepositoryImpl(
    private val profileApiClient: ProfileApiClient,
    private val userMapper: UserMapper, // violation — stateless mapper as constructor parameter
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<User> =
        runCatching { userMapper.toDomain(profileApiClient.fetchUser(userId)) }
}
```

```kotlin
// features/feature-profile/di/src/commonMain/kotlin/.../ProfileModule.kt
// BAD: Object mapper registered in Koin — objects are already singletons; Koin registration
// is redundant and pollutes the DI module with a dependency that has no runtime value.
internal object UserMapper {
    fun toDomain(json: UserJson): User = User(id = json.userId, displayName = json.displayName)
}

val profileDomainModule = module {
    single { UserMapper } // violation — object mapper registered unnecessarily
}
```

## Severity

- `🚫 Blocking` — Mapper class or object registered via `single { }` or `factory { }` in any Koin module
- `⚠️ Change requested` — Repository or data source constructor accepts a `*Mapper` type as a parameter
- `💡 Suggestion` — Repository stores a mapper as a `private val` when the mapper could be called inline as a pure function
