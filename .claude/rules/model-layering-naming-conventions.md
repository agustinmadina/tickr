---
description: Every model class must belong to exactly one layer with the correct suffix — Entity (DB), Json (API), no suffix (domain), Ui (UI)
paths:
  - "features/**/*.kt"
  - "shared/**/*.kt"
  - "sdks/**/*.kt"
---

# Model Layering & Naming Conventions

## Rule

Every model class must belong to exactly one layer and carry a suffix that makes that membership unambiguous. Sharing a model across layer boundaries — whether by reuse, extension, or annotation leakage — breaks the independence between layers and turns implementation details into public contracts.

## Naming Conventions by Layer

| Layer | Suffix | Example | Lives in |
|---|---|---|---|
| Persisted record (what is written to the platform store) | `Entity` | `HoldingEntity` | `features/*/data/` |
| API / network (Ktor response/request body) | `Json` | `UserJson` | `features/*/data/` |
| Domain (business model, source of truth) | _(none)_ | `User` | `features/*/domain/` |
| UI (display model for Compose screens) | `Ui` | `UserUi` | `features/*/ui/` |

These suffixes are mandatory for all new code and for any file modified in a PR.

## What to Check

### MUST BLOCK

- A domain model (`features/*/domain/`) carries `@Serializable`, `@SerialName`, `@Json`, or any kotlinx.serialization annotation
  - **Fix**: Move the serialization annotation to a `*Json` DTO in the `data/` sub-module and write a mapper from `*Json` to the domain model
- A domain model carries a persistence type or any `com.russhwolf.settings.*` import
  - **Fix**: Define a separate `*Entity` in `data/` and map it to the domain model in a mapper class
- A `*Json` or `*Entity` type appears as a parameter or return type in a domain repository interface or use case
  - **Fix**: The domain interface must use the plain domain model; the `data/` layer's implementation is responsible for mapping before returning to the caller
- A Compose screen, ViewModel state class, or UI event type in `features/*/ui/` directly imports or references a `*Json` or `*Entity` type
  - **Fix**: Map to a `*Ui` model in the ViewModel before exposing it via `StateFlow`; screens must only see `*Ui` types
- A model class whose name carries no suffix and is defined inside `features/*/data/` or `features/*/ui/` (suffix-less names are reserved for domain models)
  - **Fix**: Rename to the appropriate suffixed form (`*Json`, `*Entity`, or `*Ui`) and update all usages

### MUST FLAG

- A `*Json` DTO is persisted directly, without an intermediary `*Entity`, so the wire format becomes the storage format
  - **Fix**: Define a dedicated `*Entity` and map `*Json` to it; API contracts change independently of DB schema
- A `*Ui` model exposes nullable fields that the domain model has already guaranteed non-null — the ViewModel is likely skipping the mapping step
  - **Fix**: The ViewModel mapper should resolve optionality; UI state should reflect what is actually displayable, not what the network might return
- A domain enum is annotated with `@Serializable` "for convenience" when the API returns the same string values
  - **Fix**: Define a `*Json` enum in `data/` with `@Serializable` and map it to the domain enum in the mapper; API enum values change independently of business meaning
- A mapper function is defined in `domain/` (e.g., as an extension on the domain model that references a `*Json` type)
  - **Fix**: All mapper functions must live in `data/` (for `*Json`/`*Entity` to domain and back) or in `ui/` (for domain to `*Ui`)

## Enum Handling

Domain enums and data-layer enums must be kept separate unless the following conditions are ALL true:

1. The API or DB string/int values are permanently identical to the domain enum constant names
2. The enum has no associated data or business behavior
3. The team has explicitly discussed and documented the reuse decision in a code comment

When in doubt, define a `*Json` enum in `data/` and map it to the domain enum. Enum mapping is cheap; silent behavioral coupling is expensive.

## Mapper Placement

- `*Json` → domain: mapper lives in `features/*/data/src/commonMain/`, typically as an extension function on the `*Json` type or a dedicated `*Mapper` class
- `*Entity` → domain: mapper lives in `features/*/data/src/commonMain/`, alongside the data source that reads from the store
- domain → `*Ui`: mapper lives in `features/*/ui/src/commonMain/`, typically as a private extension or a dedicated `*UiMapper`; never in `domain/`
- domain → `*Json` (outbound request body): mapper lives in `features/*/data/src/commonMain/`

Mappers are intentional boilerplate. They are the seam that protects each layer from changes in adjacent layers. Do not eliminate them by sharing model types.

## Common Mistakes

- Reusing a `*Json` class directly as the ViewModel `UiState` to avoid writing a `*Ui` class — the UI then breaks whenever the API contract changes
- Placing `@Serializable` on a domain model because "the API and domain shapes are identical right now" — API shapes diverge; domain shapes must not be coupled to serialization formats
- Defining a shared `data class` with no suffix in `shared-domain/` that is also used as a network DTO — `shared-domain/` models follow the same no-suffix rule and must be free of framework annotations
- Writing a mapper in `domain/` as a convenience extension (`fun UserJson.toDomain()`) — this forces `domain/` to import `data/` types, inverting the dependency graph
- Returning a persisted `*Entity` from a repository instead of mapping it to a domain model first

## Examples

### Good

```kotlin
// features/feature-profile/data/src/commonMain/kotlin/com/example/app/feature/profile/data/dto/UserJson.kt
// Network DTO: carries serialization annotations, lives in data/, uses Json suffix.
@Serializable
internal data class UserJson(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("role") val role: UserRoleJson,
)

@Serializable
internal enum class UserRoleJson {
    ADMIN, MEMBER, GUEST
}

// features/feature-profile/data/src/commonMain/kotlin/com/example/app/feature/profile/data/local/UserEntity.kt
// Persisted record: mirrors what is written to the store, uses the Entity suffix.
internal data class UserEntity(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String, // stored as TEXT, mapped via adapter
)

// features/feature-profile/domain/src/commonMain/kotlin/com/example/app/feature/profile/domain/model/User.kt
// Domain model: no suffix, no framework annotations, pure Kotlin.
data class User(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: UserRole,
)

enum class UserRole { ADMIN, MEMBER, GUEST }

// features/feature-profile/data/src/commonMain/kotlin/com/example/app/feature/profile/data/mapper/UserMapper.kt
// Mapper lives in data/, maps Json → domain and Entity → domain.
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

// features/feature-profile/ui/src/commonMain/kotlin/com/example/app/feature/profile/ui/model/UserUi.kt
// UI model: carries display-ready strings, uses Ui suffix, lives in ui/.
internal data class UserUi(
    val displayName: String,
    val avatarInitials: String,
    val avatarUrl: String?,
    val roleLabel: String,
)

// features/feature-profile/ui/src/commonMain/kotlin/com/example/app/feature/profile/ui/mapper/UserUiMapper.kt
// Mapper lives in ui/, maps domain → Ui.
internal fun User.toUi() = UserUi(
    displayName = displayName,
    avatarInitials = displayName.take(2).uppercase(),
    avatarUrl = avatarUrl,
    roleLabel = role.name.lowercase().replaceFirstChar { it.uppercase() },
)
```

### Bad

```kotlin
// BAD: Domain model carries @Serializable — couples domain to kotlinx.serialization format.
// Any API contract change (field rename, added field) forces a domain model change.
@Serializable
data class User(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val role: UserRole,
)

// BAD: @Serializable on a domain enum — couples the domain enum constant names to
// the API's wire format. Renaming ADMIN to ADMINISTRATOR in the API silently breaks mapping.
@Serializable
enum class UserRole { ADMIN, MEMBER, GUEST }

// BAD: ViewModel exposes a *Json type directly as UI state — the UI now depends on
// the network contract. Any API change breaks the Compose screen.
class ProfileViewModel(private val getUser: GetUserUseCase) : ViewModel() {
    // UserJson is a data-layer DTO; it must never appear in ui/.
    private val _state = MutableStateFlow<UserJson?>(null)
    val state: StateFlow<UserJson?> = _state
}

// BAD: Mapper defined in domain/ — forces domain/ to import the *Json type,
// inverting the dependency so that domain depends on data.
// This file should not exist in features/feature-profile/domain/.
fun UserJson.toDomain() = User(id = userId, displayName = displayName)

// BAD: Repository interface in domain/ references the *Entity type —
// the domain contract now leaks a persistence implementation detail.
interface UserRepository {
    suspend fun getUser(id: String): UserEntity // must return User, not UserEntity
}
```

## Severity

- `🚫 Blocking` — Domain model carries `@Serializable` or any other framework annotation from `data/`
- `🚫 Blocking` — `*Json` or `*Entity` type appears as a parameter or return type in any domain interface or use case
- `🚫 Blocking` — Compose screen or ViewModel state directly references a `*Json` or `*Entity` type
- `⚠️ Change requested` — Model class defined without the correct layer suffix (e.g., a DTO in `data/` named `UserDto` instead of `UserJson`, or a UI display model named `UserModel` instead of `UserUi`)
- `⚠️ Change requested` — Mapper function placed in `domain/` instead of `data/` or `ui/`
- `⚠️ Change requested` — Domain enum reused as an API/DB serialization target without documented team decision
- `💡 Suggestion` — `*Json` DTO used directly as DB model without a dedicated `*Entity` (acceptable only when there is no local persistence layer for the feature)