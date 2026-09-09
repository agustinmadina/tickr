# CLAUDE.md

## Project Overview

Tickr is a crypto portfolio tracker with live prices, written as a Kotlin Multiplatform app whose
**entire UI is shared**: Android, iOS and web (wasmJs) run the same Compose Multiplatform code, with
no per-platform screens. Modular clean architecture, Gradle with a version catalog. The versions
that constrain everything else are Kotlin 2.4.10, AGP 9.2.1, Compose Multiplatform 1.11.1 and
Gradle 9.6.1; AGP and Kotlin are the two that break the rest when bumped.

This is a personal portfolio project, so the web target is not a nice-to-have: it is the reason the
project exists. A change that builds on Android but breaks wasmJs is a broken change.

## Module Structure

```
core/
  core-common/              DispatcherProvider. No Dispatchers.IO: JVM only, breaks iOS and wasm.
  core-domain/              UseCase and FlowUseCase base classes.
  core-network/             Ktor client + WebSocket plumbing, engine per platform. No business concepts.
  core-storage/             key-value storage behind expect/actual. No business concepts.
  core-ui/                  theme, design tokens, BaseViewModel, shared composables.
features/
  feature-portfolio/
    domain/                 models, repository interfaces, use cases. Pure Kotlin.
    data/                   repository impls, DTOs, mappers, data sources.
    ui/                     Compose screens, ViewModels, UI models.
    di/                     Koin module wiring the three together.
shared/                     aggregator: declares the three targets, hosts App() and the DI root.
                            Produces the iOS framework and the wasm bundle.
androidApp/                 Android entry point (com.android.application).
iosApp/                     Xcode project consuming shared's framework.
build-logic/                convention plugins. Every KMP module applies tickr.kmp.library
                            rather than repeating the target list.
```

### Dependency Rules

Enforced **at the Gradle level**, not by convention: `ui` does not declare a dependency on `data`,
so a violation fails to compile rather than being caught in review.

```
  ui ──→ domain ←── data
   \        ↑        /
    └──→   di   ←──┘
```

- **domain**: no framework dependencies. No Koin, no Ktor, no storage, no Compose, no `android.*`
  or `platform.*`, no `@Serializable`. Kotlin stdlib and coroutines only.
- **data**: depends on domain plus `core-network` and `core-storage`. No Compose.
- **ui**: depends on domain plus `core-ui`. Never on data.
- **di**: depends on all three and binds implementations to interfaces.
- `core/` stays domain-agnostic: a business type (`Holding`, `Money`, `PriceTick`) appearing there
  is a bug, not a shortcut.

## Key Technologies

| Concern | Choice |
|---|---|
| UI | Compose Multiplatform, Material3 only |
| Async | Coroutines and Flow. `Dispatchers.Default` or injected, never `Dispatchers.IO` in commonMain |
| Networking | Ktor client; WebSocket for the live price feed |
| Persistence | multiplatform-settings behind one `expect`/`actual`: SharedPreferences, NSUserDefaults, localStorage. Not SQLDelight, which has no wasm driver |
| DI | Koin, constructor injection |
| Tests | Kotest BehaviorSpec |

## Common Commands

```shell
# Build
./gradlew build                                 # everything
./gradlew :androidApp:assembleDebug             # Android APK
./gradlew :shared:wasmJsBrowserDistribution     # web bundle -> shared/build/dist/wasmJs/productionExecutable
./gradlew :shared:wasmJsBrowserDevelopmentRun   # web, live on localhost

# Test
./gradlew allTests

# iOS
open iosApp/iosApp.xcodeproj                    # then run from Xcode
```

## Code Rules (MANDATORY)

**You MUST comply with ALL rules in `.claude/rules/` that apply to the files you touch.** When
making changes, check every applicable rule against your code before considering the task complete.

Rules load in two tiers:

- **Always-on** (no `paths:` frontmatter): loaded into every session. Keep this set small; it is
  for rules with no path affinity.
- **Path-scoped** (`paths:` frontmatter): loaded automatically the first time a file matching their
  globs is read. **A rule not being in context does NOT mean it does not apply**, it means no
  matching file has been read yet. Reviewers read the full rule set regardless of paths.

**When creating a new file, first read a sibling file in the target directory.** Path-scoped rules
trigger on read, so this loads the layer's rules before anything is written. For a brand-new module
with no siblings, read the corresponding file in the nearest analogous module.

New rules must declare `paths:` frontmatter scoping them to the files they govern
(see `.claude/templates/rules-template.md`).

The rule set was pruned from the 41 it came with. Deliberately **not** carried over, because they
describe infrastructure this project does not have: `pr-uses-ticket-data` and
`pr-what-to-test-section` (no issue tracker, no tester distribution), `screen-app-screen-wrapper`
(no `AppScreen` composable), `todo-comment-format` (requires a ticket link), and
`breaking-change-detection` (no published SDK). Reintroduce one only along with the thing it
describes.

## Conventions

- **Commit format**: `type(scope): description`, e.g. `feat(portfolio): stream live prices`. No
  ticket prefix, since there is no tracker.
- **Branches**: `type/short-slug`. `main` is what deploys, so it stays green.
- **Verification is visual.** A screen that compiles is not a screen that renders. Check it with
  `xcrun simctl io <udid> screenshot /tmp/x.png` and read the PNG; that needs no permission, whereas
  `screencapture` fails because the terminal lacks macOS Screen Recording. Two layout bugs on day
  one were invisible in the code and obvious in the capture.

## Non-obvious things worth knowing

- **Every push to `main` publishes.** `.github/workflows/deploy-web.yml` builds the wasm bundle and
  deploys it to https://agustinmadina.github.io/tickr/. The demo is public, so a broken `main` is a
  broken portfolio piece.
- **The `compose.*` dependency accessors are deprecated but staying.** Each Compose Multiplatform
  artifact is on its own version train (material3 ships `1.12.0-alphaNN` while runtime ships
  `1.12.0`), so pinning explicit coordinates means maintaining several release calendars. They are
  wrapped in `@Suppress("DEPRECATION")` in `shared/build.gradle.kts` on purpose.
- **The web bundle is 12 MB on disk but ~4 MB over the wire**, since Pages gzips it; 8.25 MB of the
  raw size is Skia and cannot be trimmed. Quote the compressed figure.
- **`compileSdk` is 37**, which needs the `android-37.0` platform installed locally.
- **Price APIs are keyless and CORS-open by design.** Coinbase Exchange and Binance public endpoints
  both return `access-control-allow-origin: *` and need no key, which is what lets the browser build
  call them directly. Do not introduce an API that needs a secret without solving the web story
  first, because there is no server to hide a key behind.
- **Test isolation**: never use `IsolationMode` in `commonTest`. It is silently ignored on
  Kotlin/Native, so state leaks between `When` blocks and fails on iOS only. Create mutable state
  inside `When` blocks.

## Environment Variables

None required to build or run.
