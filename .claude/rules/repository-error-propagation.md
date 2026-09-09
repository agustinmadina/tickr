---
description: Result belongs at the UseCase level, not the repository level — repositories and data sources throw on failure; kotlin.Result enters the call chain exactly once, in UseCase.invoke()
paths:
  - "features/**/domain/**/*.kt"
  - "features/**/data/**/*.kt"
  - "shared/**/domain/**/*.kt"
  - "shared/**/data/**/*.kt"
  - "sdks/**/domain/**/*.kt"
  - "sdks/**/data/**/*.kt"
---

# Repository Error Propagation — Result Lives at the UseCase Level

## Rule

`kotlin.Result` is a `UseCase`-layer concept, not a repository-layer one. Repository implementations and data sources must **throw** on failure — they must never declare a `Result<T>` return type and must never convert a caught exception into `Result.failure`. `Result<T>` enters the call chain exactly once: inside `UseCase.invoke()` (`core/core-domain/.../usecase/UseCase.kt`), which already wraps `execute()` in a try/catch and returns `Result<RESULT>` to the caller. `execute()` calls repositories directly and lets exceptions propagate upward; the base class does the wrapping.

Repositories are **not required to catch anything**. The simplest compliant repository has no try/catch at all — it calls its data sources and lets exceptions fly; `UseCase.invoke()` is the error boundary. Catching inside a repository is optional and justified only for two reasons: (a) logging a failure with source context, or (b) constructing a domain-typed exception (e.g. wrapping a transport error into a feature-specific sealed error). In both cases the catch must end in `throw` — never in `return Result.failure(...)` — and `CancellationException` is rethrown first, unlogged.


**Why**:
- A single wrapping point removes the redundant `Result`-unwrap-then-rewrap dance that accumulates when both repositories and use cases handle failure independently.
- It removes the need for `getOrThrow()` chains inside `execute()` that exist only to unwrap a repository's `Result` before immediately risking it again — under this convention, `execute()` simply calls the repository and lets any exception surface, because the base class is already there to catch it.
- Throwing typed, domain-meaningful exceptions (e.g. the `core.domain.error` types produced by core-network's error mapper, or a feature-specific sealed exception hierarchy — e.g. an illustrative `ImageUploadError` with `ImageRejected`/`UploadFailed` variants) lets `execute()` and downstream consumers branch on failure type via the `Throwable` wrapped inside `Result.failure`, without inventing a second, parallel error channel.

**Known base-class caveat (not fixed by this rule)**: `UseCase.invoke()` currently catches `Throwable` broadly, which also catches `CancellationException` — this is a pre-existing defect in the base class (it should rethrow `CancellationException` before wrapping) and is tracked separately; fixing `UseCase.kt` is out of scope here and needs its own ticket/PR. Relatedly, `ObservableUseCase`'s KDoc still describes `invoke` as returning "a [Flow] of [Result]" even though its signature is `Flow<RESULT>` — stale copy from `UseCase`; correcting it belongs with that same base-class fix. Regardless of this base-class behavior, repositories and data sources must still rethrow `CancellationException` themselves (see `error-logging-requirement.md`) rather than relying on the base class to do the right thing.

## Scope

This rule's single-wrapping-point argument applies to **`suspend` repository methods reached via `UseCase`** — that is where `UseCase.invoke()` provides the `Result` boundary.

**`Flow`-returning repository methods** (the other major repository shape) follow the same "no `Result`" principle but have a different error boundary: `ObservableUseCase.invoke()` does no wrapping at all (only `flowOn`), so a failure travels *inside the stream*. The convention:

- Never return `Flow<Result<T>>` — emit values, and let failures surface as `Flow` errors (throw inside the `flow { }` builder / propagate from upstream).
- The error boundary for streams is the collector: per `viewmodel-flow-collection.md`, every production `Flow` observation ends in `.onEach { }.catch { }.launchIn(...)` — the `.catch { }` there is not optional polish, it is the stream counterpart of `UseCase.invoke()`'s try/catch.

## What to Check

### MUST BLOCK

- A repository **interface** method (in `domain/`) declared with a `Result<T>` return type, in **new code** — a brand-new repository interface, or a brand-new method added to an existing interface
  - **Fix**: Declare the method to return `T` directly and document (KDoc) that it throws on failure. The use case's `execute()` calls it directly with no unwrapping.
- A repository **implementation** method (in `data/`) that wraps its body in `runCatching { }` (or an equivalent try/catch) and returns the resulting `Result<T>`, in **new code**
  - **Fix**: Remove the `runCatching`/`Result` wrapping; call the data source directly and let exceptions propagate. If a log-and-rethrow is needed, catch, log, and `throw` — never `return Result.failure(...)`.
- A repository or data source method that catches an exception and returns `Result.failure(e)` instead of rethrowing it, in **new code**
  - **Fix**: Rethrow the exception (optionally after logging per `error-logging-requirement.md`); never convert it into a `Result`.

### MUST FLAG

- A **new** method added to an existing `Result`-returning repository interface that itself returns `Result<T>`, on the reasoning that it should "match the existing pattern in the file"
  - **Fix**: Add the new method as a throwing method even when older methods in the same interface return `Result<T>`. If a full interface migration is out of scope for the PR, flag it for a follow-up — see Existing Code below.
- A `UseCase.execute()` body that calls a **new-convention throwing repository** but still wraps the call in `runCatching { }` or a local try/catch that converts the result back into `Result`, duplicating what `UseCase.invoke()` already does
  - **Fix**: Call the repository directly inside `execute()`; let the exception propagate to the base class.
- A repository or data source that throws a bare `Exception`/`RuntimeException` when a typed/domain-meaningful exception is available (e.g. `core.domain.error` types from core-network's error mapper, or a feature-specific sealed exception hierarchy)
  - **Fix**: Throw the typed exception so `execute()` and downstream consumers can branch on failure type via `Result.failure`'s wrapped `Throwable`.

## Existing Code

Repositories that predate this rule return `Result<T>` and convert exceptions via `runCatching`. They are not violations while untouched — review evaluates changed code only. When substantively modifying such a file, migrate it to the throw convention as part of the change and drop the callers' now-unnecessary `getOrThrow()`. New code — new repositories and new methods on existing interfaces — always follows the throw convention; do not propagate the old shape into new surface area.

## Interplay with `error-logging-requirement.md`

Catching in a repository is optional (see the Rule section) — most repository methods need no try/catch at all. Where a catch IS used (for source-context logging or domain-error construction), `error-logging-requirement.md` applies unchanged, and the catch must always end in `throw`, never in `return Result.failure(...)`:

```kotlin
try {
    apiClient.fetchProfile(userId)
} catch (e: CancellationException) {
    throw e // always rethrow cancellation first, and never log it
} catch (e: Exception) {
    log.e(e) { "Failed to fetch profile for userId=$userId" }
    throw e // rethrow — never convert to Result.failure
}
```

## Common Mistakes

- Wrapping a repository call in `runCatching { }` "for safety" — the safety net is `UseCase.invoke()`; a second one downstream only produces confusing double-handling once composed with `getOrThrow()` at the use-case boundary
- Building a brand-new repository to return `Result<T>` just so `execute()` can call `.getOrThrow()` on it — this is unnecessary machinery under the new convention; make the repository throw and call it directly
- Migrating a repository interface method to throw but forgetting to update the Koin-registered implementation's `override suspend fun` signature to also drop `Result<T>` — the interface and implementation must be migrated together
- Adding a brand-new method that returns `Result<T>` "because the rest of the file already does" — new methods follow this convention regardless of the file's existing pattern
- Treating this rule as a mandate to migrate every existing repository immediately — it is not; migration happens when a file is next substantively touched (see Existing Code)
- Forgetting that `CancellationException` must be rethrown *before* any logging or generic `catch (e: Exception)` block — swallowing or logging it breaks structured concurrency (see `error-logging-requirement.md`)

## Examples

### Good

```kotlin
// shared/contacts/domain/src/commonMain/kotlin/com/example/app/contacts/domain/repository/ContactsRepository.kt
// GOOD (real code): the interface declares plain signatures; KDoc documents the throwing
// contract (failures are typically `dev.madina.tickr.core.domain.error` types produced by the
// network error mapper). Callers are UseCase.execute() implementations — the base class
// wraps thrown exceptions into Result.failure.
interface ContactsRepository {

    /** Adds the identity with [contactIdentityId] to the viewer's contacts. Throws on failure. */
    suspend fun addContact(contactIdentityId: String)

    /** Removes the identity with [contactIdentityId] from the viewer's contacts. Throws on failure. */
    suspend fun removeContact(contactIdentityId: String)
}
```

```kotlin
// GOOD (the default shape — illustrative): no try/catch at all. The API client already
// throws typed errors; UseCase.invoke() is the error boundary. This is all most methods need.
internal class ContactsRepositoryImpl(
    private val contactsApiClient: ContactsApiClient,
) : ContactsRepository {

    override suspend fun getContacts(): List<Contact> =
        contactsApiClient.fetchContacts().map { it.toDomain() }
}
```

```kotlin
// shared/contacts/data/src/commonMain/kotlin/com/example/app/contacts/data/repository/ContactsRepositoryImpl.kt
// GOOD (the optional variant — real code, trimmed): a catch is justified here because the
// method branches on a TYPED exception (409 = already a contact = success) and wants
// source-context logging. CancellationException is rethrown first, never logged; every
// other path ends in `throw` — never `return Result.failure(...)`.
override suspend fun addContact(contactIdentityId: String) {
    try {
        apiClient.addContact(AddContactRequestJson(identityId = contactIdentityId))
    } catch (e: CancellationException) {
        throw e
    } catch (e: ConflictException) {
        log.d { "Contact $contactIdentityId already exists (409) — treating as success" }
    } catch (e: Exception) {
        log.e(e) { "Failed to add contact $contactIdentityId" }
        throw e
    }
    // (cache bookkeeping elided)
}
```

```kotlin
// shared/contacts/domain/src/commonMain/kotlin/com/example/app/contacts/domain/usecase/AddContactUseCase.kt
// GOOD (real code): execute() calls the throwing repository directly. No getOrThrow(),
// no runCatching. UseCase.invoke() (base class) catches whatever the repository throws
// and wraps it into Result.failure before it reaches the ViewModel.
class AddContactUseCase(
    private val contactsRepository: ContactsRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<String, Unit>(coroutineDispatcher) {

    override suspend fun execute(parameters: String) =
        contactsRepository.addContact(parameters) // either returns or throws — nothing to unwrap
}
```

### Bad

```kotlin
// BAD — do not write this in new code. Pre-existing repositories with this shape
// migrate when next touched (see "Existing Code" above).

// features/feature-cards/domain/src/commonMain/kotlin/.../CardsRepository.kt
// BAD (new code): repository interface declares Result<T>.
interface CardsRepository {
    suspend fun getCards(): Result<List<Card>> // violation in new code
}
```

```kotlin
// features/feature-cards/data/src/commonMain/kotlin/.../CardsRepositoryImpl.kt
// BAD (new code): runCatching converts a thrown exception into Result.failure —
// exactly the pattern this rule retires for new repositories.
internal class CardsRepositoryImpl(
    private val cardsApiClient: CardsApiClient,
) : CardsRepository {

    override suspend fun getCards(): Result<List<Card>> =
        runCatching { cardsApiClient.fetchCards().map { it.toDomain() } } // violation — must throw instead

    // Also bad: explicitly catching and converting to Result.failure instead of rethrowing.
    override suspend fun refreshCards(): Result<Unit> =
        try {
            cardsApiClient.refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e) // violation — must rethrow, not wrap
        }
}
```

```kotlin
// features/feature-cards/domain/src/commonMain/kotlin/.../GetCardsUseCase.kt
// BAD: execute() unwraps a Result from a repository with getOrThrow() — this machinery
// only exists because the repository was wrongly built to return Result<T> in new code.
class GetCardsUseCase(
    private val cardsRepository: CardsRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<Card>>(coroutineDispatcher) {

    override suspend fun execute(parameters: Unit): List<Card> =
        cardsRepository.getCards().getOrThrow() // violation — cardsRepository should throw directly instead
}
```

## Severity

- `🚫 Blocking` — New repository interface or implementation method returns `Result<T>` instead of throwing
- `🚫 Blocking` — Repository or data source `catch` block converts an exception into `Result.failure(e)` instead of rethrowing (new code)
- `⚠️ Change requested` — New method added to an existing `Result`-returning repository interface that itself returns `Result<T>` instead of following the throw convention
- `⚠️ Change requested` — `execute()` wraps a call to a new-convention throwing repository in `runCatching { }` or a local try/catch that reconverts to `Result`, duplicating `UseCase.invoke()`'s job
- `💡 Suggestion` — Repository throws a generic `Exception`/`RuntimeException` instead of a typed/domain-meaningful exception when one is available
- `💡 Suggestion` — Existing `Result`-returning repository file substantively touched in a PR without opportunistically migrating it to the throw convention
