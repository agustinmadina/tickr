---
description: Modules depend on other modules' :domain contracts, never their :data implementations — only sharedLib/androidApp composition roots may depend on :data
paths:
  - "**/*.gradle.kts"
  - "**/data/**/*.kt"
  - "**/di/**/*.kt"
---

# Consume Domain, Not Data — Cross-Module Dependency Direction

## Rule

A module may depend on another module's `:domain` (the contract: interfaces + models), but **never** on another module's `:data` (the implementation). The only place allowed to depend on a `:data` module is the **composition root** — `sharedLib` and `androidApp` — where Koin binds implementations to interfaces.

This applies to shared libraries (`:shared:<lib>:data`) and to feature data layers (`:features:feature-<name>:data`) alike. When module A needs behaviour implemented in module B, A depends on `B:domain` and codes against B's interface; the concrete impl from `B:data` is injected by Koin at runtime, registered once at the composition root.

`:domain` modules have zero framework dependencies, so depending on a contract leaks nothing. `:data` modules carry impls, infrastructure, DTOs, and DI internals — pulling them across a module boundary couples implementations and inverts the dependency-on-abstraction principle.

## What to Check

### MUST BLOCK

- A feature `data/` sub-module that declares `implementation(project(":features:feature-<other>:data"))` or `implementation(project(":shared:<lib>:data"))`
  - **Fix**: Depend on the corresponding `:domain` module instead. Inject the needed type as its domain **interface** via the constructor; register the impl in Koin at the composition root
- A feature `domain/`, `ui/`, or `di/` sub-module that depends on **any** `:data` module (its own or another's)
  - **Fix**: `domain`/`ui` depend on `:domain` contracts only. The feature's own `di/` wires its own `:data` via Koin includes — it does not need to depend on a *sibling feature's* or *shared lib's* `:data`
- A constructor (repository impl, data source, manager) in module A that accepts a **concrete implementation type** from module B's data layer (e.g. `DefaultSessionManager`, `AuthRepositoryImpl`, `*ApiClient` from another module)
  - **Fix**: Accept the domain interface (`SessionManager`, `AuthRepository`) instead; Koin provides the concrete impl

### MUST FLAG

- A `:data` module referenced by any module that is not `sharedLib` or `androidApp`
  - **Acceptable**: A feature's own `di/` sub-module depending on its own sibling `:data` to aggregate Koin modules — that is the intended 4-sub-module wiring, internal to the feature
  - **Fix** (otherwise): Re-point the dependency to the matching `:domain` module and move impl binding to the composition root
- A type needed across modules that exists only in `:data` (no interface in `:domain`)
  - **Fix**: Promote the contract out of `:data`. If it is framework-free, put the interface in `:domain`. If it is **framework-coupled** (e.g. exposes a Koin `Scope`, a Ktor type) it cannot go in `:domain` (domain purity forbids framework imports) — extract it into a dedicated framework-legal contract module instead. Reference precedent: identity's Koin-coupled DI-scope contracts (`IdentityScope`, `IdentityScopeProvider`) live in `:shared:identity:contracts`, consumed by other data modules without depending on `:shared:identity:data`. The impl (`IdentityScopeManager`) stays in `:data`.

## Common Mistakes

- Depending on `:shared:identity:data` from a feature's `data/` to reuse `DefaultSessionManager` — depend on `:shared:identity:domain` and inject `SessionManager` (the interface); Koin binds `DefaultSessionManager` at the composition root
- Thinking "domain is just interfaces, so data is where the real dependency is" — the runtime impl is bound by DI; compile-time you only ever need the contract. Reaching for `:data` means you are about to hold a concrete type you should be injecting
- Adding a sibling-feature `:data` dependency to get a model — models live in `:domain`; depend on that
- Believing a module must depend on `:data` to "make the impl available" — availability is a runtime concern solved by Koin registration at the composition root, not a compile-time dependency in the consumer

## Examples

### Good

```kotlin
// features/feature-onboarding/data/build.gradle.kts
// Depends on the shared library's CONTRACT only.
commonMain.dependencies {
    implementation(project(":shared:identity:domain"))   // interface + models
    // NOT :shared:identity:data
}

// features/feature-onboarding/data/.../OnboardingRepositoryImpl.kt
// Codes against the interface; Koin injects the concrete impl at runtime.
internal class OnboardingRepositoryImpl(
    private val sessionManager: SessionManager,   // domain interface from :shared:identity:domain
    private val settings: CoreSettings,
) : OnboardingRepository { /* ... */ }
```

```kotlin
// sharedLib/build.gradle.kts — the composition root is the ONE place that pulls :data
commonMain.dependencies {
    implementation(project(":shared:identity:domain"))
    implementation(project(":shared:identity:data"))   // binds DefaultSessionManager → SessionManager via Koin
}
```

### Bad

```kotlin
// features/feature-onboarding/data/build.gradle.kts
// BAD: feature data depending on another module's data layer (impls).
commonMain.dependencies {
    implementation(project(":shared:identity:data"))   // violation — reach into impls
}

// features/feature-onboarding/data/.../OnboardingRepositoryImpl.kt
// BAD: holding the concrete impl type instead of the interface.
internal class OnboardingRepositoryImpl(
    private val sessionManager: DefaultSessionManager,   // violation — concrete impl from another module
) : OnboardingRepository { /* ... */ }
```

## Severity

- `🚫 Blocking` — A `:data` module (shared lib or sibling feature) referenced by anything other than `sharedLib`/`androidApp` or the owning feature's own `di/`
- `🚫 Blocking` — A constructor accepting a concrete implementation type from another module's data layer instead of the domain interface
- `⚠️ Change requested` — A cross-module type that exists only in `:data` with no `:domain` interface to depend on
