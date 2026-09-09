---
description: KDoc only on public cross-module API; no KDoc on internal/private/protected; // comments capped at one line (multi-line only by explicit developer decision); change-history narration banned everywhere
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Comment and KDoc Scope

## Rule

Comments and KDoc are not free — every one is a maintenance liability that can drift from the code it describes. Default to writing no comments. A plain `//` comment is warranted only when it captures a non-obvious **WHY** — a hidden constraint, a subtle invariant, a workaround for a specific bug, surprising behavior — that the code's own names cannot express. It must never reference the current task, fix, ticket, or a specific caller.

A `//` comment is capped at **one line**. A multi-line `//` block (two or more consecutive comment lines above or beside the same code) is flagged by default: explaining at length in comments is a deliberate, human decision, never something an agent or a default writing habit introduces. The fix is to condense to a single line or delete; if one line cannot carry the WHY, that is usually the signal to rename or restructure the code instead. A multi-line block survives review only when the developer explicitly decides to keep it (their call, stated on the PR) — reviewers flag it, they do not silently accept it.

KDoc (`/** ... */`) is reserved **exclusively** for genuinely public, cross-module API surfaces — the things `visibility-modifiers.md` and `viewmodel-visibility.md` establish as `public` because other modules consume them: domain repository interfaces, public domain models, use cases, and public `core-ui` components. On an `internal`, `private`, or `protected` declaration — ViewModels (`internal` per `viewmodel-visibility.md`), data-layer `internal` classes (`*Json`, `*Entity`, `*ApiClient`, mappers per `visibility-modifiers.md`), private functions, or any other implementation detail with no consumer outside its own module — KDoc is **not allowed**. If the content is a genuine non-obvious WHY, convert it to a single-line `//` comment; otherwise delete it. The only exceptions are the two cases where another checked-in convention explicitly requires KDoc regardless of visibility — `expect` declarations (`platform-parity.md`) and the `suspend`-returning-`Flow` explanation (`coroutine-thread-safety.md`) — listed in Scope below.

Content that other conventions require on non-public declarations — a placeholder-content deferral naming its replacing ticket (`composable-no-hardcoded-strings.md`'s exemption) or an MVI presentation-enum's code↔error correspondence (CLAUDE.md's MVI conventions) — is still required, but it is written as a **single-line** `//` comment, never as KDoc and never as a multi-line block. Both the KDoc form and the multi-line form of that content are violations; the fix is format conversion and condensing, not deletion.

Single-line inline `//` comments — on local variables, private members, or code sections inside function bodies — are the author's judgment call. Reviewers do not flag them for restating WHAT the code does or for being unnecessary; over-commenting at the single-line scale is a style preference, not a review finding. The inline-comment violations are exactly two: change-history narration (task/fix/PR/caller references) and exceeding one line.

On public declarations, KDoc quality is still checked: a block that merely restates the signature with no invariant, constraint, or throwing contract is content-free and worse than no KDoc, because it looks authoritative while saying nothing. The one-line cap does not apply to KDoc on public API — detailed public documentation is KDoc's job.

TODO/FIXME comments are a distinct, already-covered concern with their own required format — see `todo-comment-format.md`. This rule does not apply to them.

## Scope

Out of scope — do not flag:

- File-level and module-level KDoc (`@file:` targets, package-level docs) — no declaration visibility to check against
- `expect` declarations — per `platform-parity.md`, platform-specific limitations are documented with KDoc on the `expect` declaration regardless of the containing module's default visibility
- The narrow `suspend`-returning-`Flow` exception in `coroutine-thread-safety.md`, which requires a KDoc explaining why suspension at the call site is needed — even on an `internal` repository implementation
- Test source sets (`commonTest`, `androidUnitTest`, `iosTest`, `desktopTest`) and `@Preview` composable files
- Generated or scaffolded code not yet substantively edited (e.g. a `/create-feature` scaffold) — fix on first substantive edit, consistent with `repository-error-propagation.md`'s Existing Code treatment
- Single-line inline `//` comments on local variables, private members, and code sections — never flagged for content or presence, only for change-history narration
- A multi-line `//` block the developer has explicitly decided to keep, with that decision on record (PR description or review reply) — the flag exists to force the decision, not to override it

## What to Check

### MUST BLOCK

None — comment and KDoc hygiene is a maintainability concern, not a build-breaking one. All enforcement is at review time (see Severity).

### MUST FLAG

- KDoc (`/** ... */`) on any `private`, `internal`, or `protected` declaration, outside the two Scope exceptions above — ViewModels, data-layer `internal` classes, private functions, or any implementation detail with no cross-module consumer. This includes convention-required content in KDoc form: a placeholder-content deferral or an MVI error-code correspondence written as `/** ... */` on a non-public declaration is a violation of this rule even though the content itself is mandatory
  - **Fix**: Delete the KDoc block. If the content is a genuine non-obvious WHY — or content another convention requires, like a placeholder deferral naming its replacing ticket or an MVI enum's code↔error mapping — convert it to a single-line `//` comment; the declaration's visibility disqualifies the KDoc form, never the content
- A multi-line `//` block: two or more consecutive `//` lines documenting the same declaration or code section, in any production source at any visibility. This includes multi-line versions of otherwise-legitimate WHY comments and of convention-required content (placeholder deferral, MVI error mapping)
  - **Fix**: Condense to one line or delete. If one line cannot carry the WHY, prefer renaming or restructuring the code. Keeping the multi-line block is a deliberate developer decision that must be stated explicitly (PR description or review reply) — absent that, the flag stands
- A comment or KDoc that narrates the current change's history by referencing a task, fix, PR, or specific caller by name (e.g. `// Fixed for TICKET-627`, `// added because ProfileViewModel needs this`)
  - **Fix**: Remove the reference. That context belongs in the commit message or PR description (see `pr-uses-ticket-data.md`), never in source — the comment should describe the code's own invariant, not the history of how it got there
  - **Acceptable**: A ticket referenced in a single-line `//` comment because a documented convention *requires* naming it, independent of which task currently touches the file — a placeholder-content deferral stating which ticket/phase replaces it (`composable-no-hardcoded-strings.md`'s exemption), an MVI presentation-enum's code↔error correspondence (CLAUDE.md's MVI conventions), or a `TODO`/`FIXME` in its required format (`todo-comment-format.md`). The ban targets a comment whose *only* purpose is narrating this edit's own history — a comment can name a ticket and still be banned if that's all it does; a comment can name a ticket and be fine if a listed convention is why it's there
- KDoc on a domain use case, repository interface, or public domain model that merely restates the method/property signature (e.g. `/** Gets the user profile. */` above `fun getUserProfile(): UserProfile`) with no invariant, constraint, or throwing contract documented
  - **Fix**: Either add substantive content (a documented throwing contract per `repository-error-propagation.md`, a non-obvious constraint) or remove the KDoc — content-free KDoc is worse than no KDoc because it looks authoritative while saying nothing

### DO NOT FLAG

- Single-line inline `//` comments on local variables, private functions, or code sections — regardless of content, unless they narrate change history. Reducing reviewer noise on these is deliberate: whether a single-line WHAT-restating comment stays is the author's call
- KDoc presence on a `public` cross-module declaration — only content-free KDoc there is a finding, not the block itself; multi-line KDoc on public API is fine
- Single-line `//` comments carrying convention-required content on non-public declarations — a placeholder deferral naming its replacing ticket, or an MVI enum's code↔error correspondence — including their ticket references
- A multi-line `//` block whose retention the developer has explicitly confirmed on the PR

## Common Mistakes

- Adding a KDoc block to a function because it "looks public" (top of file, no visible modifier at a glance) without actually checking whether the declaration is `internal`/`private` — visibility must be verified, not assumed from formatting or file location
- Writing convention-required documentation (placeholder deferral, MVI error mapping) as KDoc or as a multi-line block — the content is mandatory, the format is one `//` line
- Deleting a placeholder-deferral or MVI error-mapping comment entirely when converting away from KDoc or condensing — the fix is format conversion; removing the content violates the convention that requires it
- Wrapping one thought across several `//` lines because the sentence ran long — condense the sentence; the cap is a forcing function for precision, and keeping a genuinely long explanation is the developer's explicit call, not a formatting accident
- An agent expanding a one-line comment into a paragraph "to be thorough" — length is a human decision; agents write one line or nothing
- "Fixing" a non-public KDoc violation by making the declaration `public` so the KDoc can stay — visibility is governed by `visibility-modifiers.md`; the correct fix is deleting or converting the KDoc, never widening visibility
- Flagging a single-line `// what this does` comment on a local variable as a violation — this rule deliberately leaves single-line comment content to the author; only history narration and the one-line cap are enforced
- Narrating the current change instead of documenting an invariant (`// updated this to handle the new response shape`) — if it is follow-up work, it must use the `todo-comment-format.md` format; if it is just describing the diff, delete it

## Examples

### Good

```kotlin
// features/feature-cards/data/src/commonMain/kotlin/.../CardsRepositoryImpl.kt
// GOOD: internal, no KDoc; one-line WHY comment stating a hidden upstream quirk.
internal class CardsRepositoryImpl(
    private val cardsApiClient: CardsApiClient,
) : CardsRepository {

    override suspend fun getCards(): List<Card> {
        // Endpoint returns soft-deleted cards as status=null — filter or callers see phantom cards.
        return cardsApiClient.fetchCards().filter { it.status != null }.map { it.toDomain() }
    }
}
```

```kotlin
// features/feature-cards/domain/src/commonMain/kotlin/.../CardsRepository.kt
// GOOD: public, cross-module domain interface — KDoc warranted, documents the throwing contract.
interface CardsRepository {

    /** Returns the viewer's cards. Throws [CardsUnavailableException] if the issuer is down. */
    suspend fun getCards(): List<Card>
}
```

```kotlin
// GOOD: no comment at all — the default target.
private fun formatCardNumber(raw: String): String = raw.chunked(CARD_GROUP_SIZE).joinToString(" ")
```

```kotlin
// features/feature-settings/ui/src/commonMain/kotlin/.../ReleaseNotesScreen.kt
// GOOD: convention-required placeholder deferral as one // line, ticket named per
// composable-no-hardcoded-strings.md.
// Placeholder stub (TICKET-631): replaced by the server-supplied release-notes feed in a later ticket.
private val releaseNoteEntries = persistentListOf(
    ReleaseNoteEntryUi(date = "Mar 14 2026", version = "5.16.0", description = "..."),
)
```

```kotlin
// features/feature-addmoney/ui/src/commonMain/kotlin/.../AddMoneyErrorUi.kt
// GOOD: MVI code↔error correspondence as one // line per CLAUDE.md's MVI conventions.
// Backend mapping: 422/AMOUNT_TOO_LOW → BelowMinimum, 422/LIMIT_EXCEEDED → OverDailyLimit (until data layer owns it).
internal enum class AddMoneyErrorUi { BelowMinimum, OverDailyLimit, Generic }
```

### Bad

```kotlin
// features/feature-cards/ui/src/commonMain/kotlin/.../CardsViewModel.kt
// BAD: KDoc on an internal ViewModel — violation regardless of content; this one also
// narrates change history with a ticket reference.
/**
 * ViewModel for the cards screen.
 * Added for TICKET-742 to support the new cards list UI.
 */
internal class CardsViewModel(
    private val getCardsUseCase: GetCardsUseCase,
) : ViewModel() { /* ... */ }
```

```kotlin
// BAD: multi-line // block — even though the content is a legitimate WHY, two consecutive
// comment lines exceed the cap. Condense to one line, or the developer explicitly keeps it.
override suspend fun getCards(): List<Card> {
    // The cards endpoint returns soft-deleted cards with status=null rather than
    // omitting them entirely — filter here or callers see phantom cards.
    return cardsApiClient.fetchCards().filter { it.status != null }.map { it.toDomain() }
}
```

```kotlin
// BAD: convention-required placeholder content as KDoc on a private declaration —
// mandatory content, wrong format twice over (KDoc, and it spans multiple lines).
// Fix: one // line naming the ticket; never delete the content.
/**
 * Static release-notes stub (TICKET-631, Phase 11). Placeholder entries until a later,
 * data-driven ticket swaps [releaseNoteEntries] for the real server-supplied feed.
 */
private val releaseNoteEntries = persistentListOf(
    ReleaseNoteEntryUi(date = "Mar 14 2026", version = "5.16.0", description = "..."),
)
```

```kotlin
// BAD: history narration in an inline comment — describes this edit's story, not an invariant.
internal fun mapError(code: Int): CardsError {
    // changed for TICKET-810 because ProfileViewModel needs the new error shape
    return when (code) { /* ... */ }
}
```

```kotlin
// BAD: content-free KDoc on a public use case — restates the signature, documents nothing.
/**
 * Gets the user's cards.
 */
class GetCardsUseCase(
    private val cardsRepository: CardsRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<Unit, List<Card>>(coroutineDispatcher) {

    override suspend fun execute(parameters: Unit): List<Card> =
        cardsRepository.getCards()
}
```

```kotlin
// NOT FLAGGED (but discouraged): single-line WHAT-restating inline comments on private
// implementation details. Author's call — reviewers leave these alone.
internal class CardsRepositoryImpl(
    private val cardsApiClient: CardsApiClient, // the API client for cards
) : CardsRepository {

    override suspend fun getCards(): List<Card> {
        // Fetch the cards from the API
        val response = cardsApiClient.fetchCards()
        return response.map { it.toDomain() }
    }
}
```

## Severity

- `⚠️ Change requested` — KDoc on a `private`, `internal`, or `protected` declaration (ViewModel, data-layer internal class, private function, or any non-cross-module implementation detail), outside the two KDoc carve-outs (`expect` declarations, the `suspend`-returning-`Flow` explanation) — regardless of the block's content; convention-required content converts to a single-line `//` comment, it is not exempt in KDoc form
- `⚠️ Change requested` — Multi-line `//` block (two or more consecutive comment lines on the same declaration or code section) in production source, at any visibility, without an explicit on-record developer decision to keep it — including multi-line forms of WHY comments and convention-required content
- `⚠️ Change requested` — Comment or KDoc whose only purpose is narrating this edit's own history via a task, fix, PR, or specific-caller reference — not a ticket named in a single-line `//` comment because a separate checked-in convention requires it (placeholder deferral, an MVI presentation-enum's code↔error correspondence, `TODO`/`FIXME`)
- `⚠️ Change requested` — Content-free KDoc on a public domain interface, domain model, or use case that only restates the signature with no invariant, constraint, or throwing contract
