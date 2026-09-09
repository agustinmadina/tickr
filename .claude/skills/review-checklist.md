# Shared Review Checklist

This checklist is used by both `/review-local` and `/review-pr` skills. Each item maps to a rule file in `.claude/rules/`. Read the referenced rule for full details, severity levels, and examples.

When reviewing, only flag genuine issues — skip categories not relevant to the changes.

## Architecture & Module Boundaries

| Check | Rule file | Severity |
|---|---|---|
| Clean architecture layers respected (UI -> Domain <- Data) | `code-quality-checklist.md` | Blocking |
| Infrastructure modules (`core-network`, `core-database`, `core-realtime`) only in `data/` layer | `code-quality-checklist.md` | Blocking |
| No circular dependencies between modules | `repository-dependency-boundaries.md` | Blocking |
| Feature modules follow 4-layer structure (data, domain, ui, di) | `code-quality-checklist.md` | Blocking |
| Domain layer has zero framework imports (no Koin, Ktor, SQLDelight, Android, Compose) | `code-quality-checklist.md` | Blocking |
| Repositories never depend on other repositories | `repository-dependency-boundaries.md` | Blocking |
| Data-layer classes never depend on use cases | `data-layer-use-case-prohibition.md` | Blocking |

## Visibility Modifiers

| Check | Rule file | Severity |
|---|---|---|
| Feature module classes default to `internal` | `visibility-modifiers.md` | Change requested |
| Data-layer data classes (`*Json`, `*Entity`) are `internal` | `visibility-modifiers.md` | Blocking |
| Ktor API clients (`*ApiClient`, `*ApiService`) are `internal` | `visibility-modifiers.md` | Blocking |
| Mappers in data layer are `internal` | `visibility-modifiers.md` | Blocking |
| Repository implementations are `internal` | `visibility-modifiers.md` | Blocking |
| ViewModels are `internal` | `viewmodel-visibility.md` | Change requested |
| `@Preview` composables are `private` | `visibility-modifiers.md` | Change requested |
| `PreviewParameterProvider` is `private` or `internal` | `compose-preview-parameter-provider.md` | Blocking |
| Test classes are `internal` | `test-class-visibility.md` | Change requested |
| Koin module vals in `data/` and `ui/` are `public` (for `di/` includes) | `visibility-modifiers.md` | Note |

## Model Layering & Naming

| Check | Rule file | Severity |
|---|---|---|
| Domain models have no `@Serializable` or framework annotations | `model-layering-naming-conventions.md` | Blocking |
| Network DTOs use `*Json` suffix, live in `data/` | `model-layering-naming-conventions.md` | Change requested |
| Database entities use `*Entity` suffix, live in `data/` | `model-layering-naming-conventions.md` | Change requested |
| UI display models use `*Ui` suffix, live in `ui/` | `model-layering-naming-conventions.md` | Change requested |
| Domain models have no suffix | `model-layering-naming-conventions.md` | Change requested |
| No `*Json` or `*Entity` types in domain interfaces or ViewModel state | `model-layering-naming-conventions.md` | Blocking |
| Mappers live in `data/` (Json/Entity->domain) or `ui/` (domain->Ui), never in `domain/` | `model-layering-naming-conventions.md` | Change requested |

## Dependency Injection

| Check | Rule file | Severity |
|---|---|---|
| Mappers NOT registered in Koin (`single{}` or `factory{}`) | `di-mapper-prohibition.md` | Blocking |
| No mapper types as constructor parameters | `di-mapper-prohibition.md` | Change requested |
| No repository-to-repository injection | `repository-dependency-boundaries.md` | Blocking |
| No use case injection into data-layer classes | `data-layer-use-case-prohibition.md` | Blocking |
| Constructor injection preferred over service locator | `code-quality-checklist.md` | Change requested |

## Use Cases

| Check | Rule file | Severity |
|---|---|---|
| All use cases extend `UseCase<PARAMS, RESULT>` | `use-case-pattern.md` | Blocking |
| Use cases live in `domain/` sub-module only | `use-case-pattern.md` | Blocking |
| `execute()` returns `RESULT` directly (not `Result<RESULT>`) | `use-case-pattern.md` | Blocking |
| Called with direct call syntax `useCase(params)`, not `.invoke()` | `use-case-invocation-syntax.md` | Suggestion |
| No-param use cases use `Unit` as PARAMS, called as `useCase()` | `use-case-pattern.md` | Suggestion |
| Params defined as nested `data class` inside use case | `use-case-pattern.md` | Suggestion |

## ViewModel & State

| Check | Rule file | Severity |
|---|---|---|
| All state mutations via `updateState { it.copy(...) }` | `viewmodel-state-source-of-truth.md` | Blocking |
| No secondary `MutableStateFlow` alongside `BaseViewModel.state` | `viewmodel-state-source-of-truth.md` | Change requested |
| No side effects inside `updateState {}` lambda (no `emitEffect`, `launch`, logging) | `update-state-purity.md` | Blocking |
| `emitEffect()` called AFTER `updateState`, not inside | `update-state-purity.md` | Blocking |
| Flow collection via `.onEach{}.catch{}.launchIn()`, not `launch{collect{}}` | `viewmodel-flow-collection.md` | Change requested |
| `getOrThrow()` only inside `UseCase.execute()`, not in ViewModels | `result-method-restrictions.md` | Blocking |

## Compose UI

| Check | Rule file | Severity |
|---|---|---|
| Material3 only — no `androidx.compose.material` imports (icon imports exempt) | `material3-components.md` | Blocking |
| No hardcoded user-visible strings — use `stringResource(Res.string.*)` | `composable-no-hardcoded-strings.md` | Change requested |
| Collections in `UiState` use `ImmutableList`/`ImmutableSet` | `compose-immutable-collections.md` | Change requested |
| `UiState` defaults use `persistentListOf()` not `emptyList()` | `compose-immutable-collections.md` | Change requested |
| Multi-input string computations wrapped in `remember(keys)` | `compose-string-remember.md` | Blocking |
| Raw `.dp` > 4dp extracted to named constants | `composable-no-raw-dimensions.md` | Suggestion |
| Raw `.sp` replaced with `MaterialTheme.typography` | `composable-no-raw-dimensions.md` | Suggestion |

## Coroutines & Thread Safety

| Check | Rule file | Severity |
|---|---|---|
| No `Dispatchers.IO` in `commonMain` | `coroutine-thread-safety.md` | Blocking |
| No `runBlocking` in shared code | `coroutine-thread-safety.md` | Blocking |
| No `GlobalScope` usage | `coroutine-thread-safety.md` | Blocking |
| No `Thread.sleep()` in commonMain (use `delay()`) | `coroutine-thread-safety.md` | Blocking |
| No `synchronized` in commonMain (use `Mutex`) | `coroutine-thread-safety.md` | Blocking |
| Functions returning `Flow`/`StateFlow`/`SharedFlow` are NOT `suspend` | `coroutine-thread-safety.md` | Change requested |
| `SharingStarted.WhileSubscribed(5000)` for UI-bound flows | `coroutine-thread-safety.md` | Suggestion |

## Error Handling

| Check | Rule file | Severity |
|---|---|---|
| No empty `catch` blocks or empty `onFailure {}` | `error-logging-requirement.md` | Blocking |
| Every catch/onFailure logs with `log.e(throwable) { "description" }` | `error-logging-requirement.md` | Blocking |
| `Throwable` passed as first positional arg to `log.e()` | `error-logging-requirement.md` | Change requested |
| `CancellationException` always rethrown, never swallowed | `error-logging-requirement.md` | Blocking |
| No `getOrThrow()` in ViewModels, repositories, or Composables | `result-method-restrictions.md` | Blocking |
| No `getOrNull()`/`getOrDefault()` outside `UseCase.execute()` | `result-method-restrictions.md` | Change requested |

## Code Quality

| Check | Rule file | Severity |
|---|---|---|
| No `!!` operator outside tests | `code-quality-checklist.md` | Change requested |
| No wildcard imports | `code-quality-checklist.md` | Change requested |
| No fully qualified names inline (use imports) | `import-fully-qualified-names.md` | Change requested |
| Enum entries referenced via static imports (unqualified) in `when` expressions | `enum-import-style.md` | Change requested |
| Function bodies < 4 levels of nesting | `method-nesting-early-return.md` | Suggestion |
| TODO comments have ticket ID and description | `todo-comment-format.md` | Change requested |
| No spelling/typo errors in code, comments, or strings | `code-quality-checklist.md` | Change requested |

## KMP & Platform Code

| Check | Rule file | Severity |
|---|---|---|
| Every `expect` has `actual` for Android, iOS, and Desktop | `platform-parity.md` | Blocking |
| `actual` implementations are semantically equivalent | `platform-parity.md` | Change requested |
| No `android.*` or `platform.Foundation.*` imports in `commonMain` | `code-quality-checklist.md` | Blocking |
| Platform-specific code only in `androidMain/`, `iosMain/`, `desktopMain/` | `platform-parity.md` | Blocking |

## Testing

| Check | Rule file | Severity |
|---|---|---|
| Test classes are `internal` | `test-class-visibility.md` | Change requested |
| Mutable state created inside `When` blocks (not at `Given` level) | `code-quality-checklist.md` | Change requested |
| No `IsolationMode` usage in commonTest | `code-quality-checklist.md` | Blocking |

## Infrastructure & Config

| Check | Rule file | Severity |
|---|---|---|
| No absolute paths in `.claude/` or `docs/` files | `no-absolute-paths-in-config.md` | Blocking |
| SQLDelight migrations have `.sqm` files, are backwards-compatible | `sqldelight-migration-safety.md` | Blocking |
| Breaking changes in SDK/shared/core public APIs are documented | `breaking-change-detection.md` | Suggestion |
| PR is single-purpose and within size guidelines | `pr-size-scope.md` | Suggestion |
| PR title and body carry the Linear ticket ID and its acceptance criteria | `pr-uses-ticket-data.md` | Blocking |
| User-visible PR has a `## What to test` section, written for a tester | `pr-what-to-test-section.md` | Change requested |
