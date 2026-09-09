# Module Placement — core/ vs shared/ vs features/

## Rule

Every new module must be placed in the top-level group that matches its nature, decided on a single axis: **technical infrastructure vs business logic vs user-facing feature**. Misplacing a module leaks domain knowledge into infrastructure, couples features to each other, or buries a reusable concern inside one feature where no sibling can reach it.

- **`core/`** — domain-agnostic technical infrastructure. Knows *how* (Ktor setup, key-value storage setup, Compose theme, design tokens, base `UseCase`/repository contracts). Knows nothing about `Holding`, `Portfolio`, `PriceTick`. Could ship unchanged in an unrelated app. Exposes its primary deps as `api`.
- **`shared/`** — cross-feature business logic. Knows the domain (`User`, `Auth`, `Session`, `Identity`) and is consumed by **two or more** features. Product-specific.
- **`features/`** — one user-facing slice (its screens + the domain/data behind them), consumed by no other feature.

### The decision test

1. "Could this ship in an unrelated app, unchanged?" → **`core/`**.
2. "Does it name a business concept AND is it used by 2+ features (or by features + app wiring)?" → **`shared/`**.
3. "Is it used by exactly one feature?" → that **feature's own** sub-module.

### Shared library nesting convention

A shared library that has multiple clean-architecture layers is grouped under its own directory and nested with colon-separated Gradle paths, mirroring the feature convention (`:features:feature-<name>:<layer>`):

```
shared/identity/
├── domain/   # :shared:identity:domain — models, repository interfaces, session contracts (pure Kotlin)
└── data/     # :shared:identity:data   — impls, session manager, token provider
```

Flat single-purpose shared modules (`shared/shared-domain`, `shared/shared-data`) keep the flat `:shared:<name>` form. Use the nested `shared/<group>/{domain,data,...}` form only when a shared concern is a bounded library with its own layers (e.g. `identity`).

Reference decision: **identity** (auth/session/identity models + `SessionManager` + `SessionTokenProvider`, consumed by `feature-onboarding` and the app shell) is **shared business logic, not a feature** — it has no `ui/` and is depended on by other modules. It lives at `:shared:identity:domain` / `:shared:identity:data`, **not** under `features/` and **not** in a `libs/` folder (no such group exists in this project).

## What to Check

### MUST BLOCK

- A new module placed in `core/` that imports or references a business-domain type (`User`, `Auth`, `Endorsement`, `Identity`, etc.) or a `*Json`/`*Entity` DTO
  - **Fix**: Move it to `shared/` (if used by 2+ features) or into the owning feature's sub-module
- A feature module (`features/feature-*`) that declares a dependency on **another** feature module
  - **Fix**: Extract the shared type/logic into `shared/` and have both features depend on that instead — features never depend on siblings
- A new top-level module group that is not one of `core/`, `features/`, `shared/`, `sdks/`, `sharedLib/`, `androidApp/`, `iosApp/` (e.g. inventing `libs/`, `lib/`, `modules/`)
  - **Fix**: Use the existing group that fits — domain-agnostic infra → `core/`, cross-feature business logic → `shared/`. Adding a group requires updating this rule, the CLAUDE.md Module Structure section, and any agent/skill that assumes the standard groups

### MUST FLAG

- A module under `features/` that has no `ui/` sub-module and is consumed by other modules — it is a library, not a feature
  - **Fix**: Move it to `shared/`. A feature is user-facing (has screens); a consumed-by-others, UI-less module is shared business logic
- A reusable business type defined inside one feature's `data/` or `domain/` that a second feature now needs
  - **Fix**: Promote it to `shared/` rather than adding a feature→feature dependency
- A shared library with multiple layers placed as flat sibling modules (`shared/identity-domain`, `shared/identity-data`) instead of the nested group form (`shared/identity/{domain,data}`)
  - **Acceptable**: Flat form is fine for single-purpose shared modules (`shared-domain`, `shared-data`); prefer the nested group form when the concern is a bounded multi-layer library

## Common Mistakes

- Putting auth/session/identity under `features/` because "login is a feature" — the *login screen* is a feature; the *session/token/identity domain* that every authenticated call depends on is shared infrastructure-of-the-domain and belongs in `shared/`
- Inventing a `libs/` folder for shared libraries — this project's documented groups are `core/`/`features/`/`shared/`/`sdks/`; a shared library goes in `shared/`, optionally nested
- Adding a feature→feature `implementation(project(...))` dependency to reuse a model — this couples release cycles and breaks the dependency graph; lift the model to `shared/`
- Placing a business model in `core/` because it is "used everywhere" — ubiquity does not make it domain-agnostic; `core/` must remain free of business concepts
- Depending on a shared library's `data/` from a feature instead of its `domain/` — features depend on the shared library's **domain** (interfaces/models); the `data/` impls bind via Koin at the app/`sharedLib` level (same rule as features' own `ui/`-never-`data/` boundary)

## Examples

### Good

```kotlin
// settings.gradle.kts — identity is a nested shared library, not a feature
include(":shared:identity:domain")
include(":shared:identity:data")

// features/feature-onboarding/data/build.gradle.kts
// Feature depends on the shared library's DOMAIN only (interfaces + models).
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:identity:domain"))
        }
    }
}
```

```
shared/identity/
├── domain/   # :shared:identity:domain — AuthRepository, SessionManager, Identity, PersonaId
└── data/     # :shared:identity:data   — DefaultSessionManager, SessionTokenProvider, MockAuthRepository
```

### Bad

```kotlin
// BAD: identity placed under features/ — it has no ui/ and is consumed by other modules.
include(":features:feature-identity:domain")   // violation — it's a shared library, use :shared:identity:domain

// BAD: feature depending on another feature to reuse a model.
// features/feature-payments/data/build.gradle.kts
implementation(project(":features:feature-onboarding:domain")) // violation — lift the shared type to shared/

// BAD: inventing a new top-level group.
include(":libs:identity:domain") // violation — no libs/ group; use :shared:identity:domain

// BAD: business model parked in core/.
// core/core-network/.../model/User.kt  — violation: core/ must stay domain-agnostic
```

## Severity

- `🚫 Blocking` — Business-domain type or DTO placed in a `core/` module
- `🚫 Blocking` — Feature module depending on another feature module
- `🚫 Blocking` — New top-level module group outside the documented set (`core/`, `features/`, `shared/`, `sdks/`, `sharedLib/`, `androidApp/`, `iosApp/`)
- `⚠️ Change requested` — UI-less, consumed-by-others module placed under `features/` instead of `shared/`
- `⚠️ Change requested` — Feature depending on a shared library's `data/` instead of its `domain/`
- `💡 Suggestion` — Multi-layer shared library kept as flat siblings instead of the nested `shared/<group>/{domain,data}` form
