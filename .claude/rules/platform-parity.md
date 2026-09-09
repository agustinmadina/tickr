---
description: Ensure expect/actual declarations have consistent implementations across all KMP targets (Android, iOS, wasmJs)
paths:
  - "**/commonMain/**/*.kt"
  - "**/androidMain/**/*.kt"
  - "**/iosMain/**/*.kt"
  - "**/wasmJsMain/**/*.kt"
---

# Platform Parity Check

## Rule

Every `expect` declaration in `commonMain` must have a corresponding `actual` implementation for ALL supported targets: Android, iOS (including simulator variants), and wasmJs. Missing `actual` implementations will cause build failures on the affected platform.

This project declares exactly those three in `build-logic`. There is no desktop source set: Compose Multiplatform targets JVM desktop and the architecture would need no change for it, but it is not built here, so a rule demanding a `wasmJsMain` actual would block every `expect` in the repo.

## What to Check

### Completeness
- Every `expect class`, `expect fun`, `expect val`, `expect object` in `commonMain` must have matching `actual` declarations in:
  - `androidMain/`
  - `iosMain/` (or platform-specific `iosArm64Main/`, `iosSimulatorArm64Main/` if not using intermediate source set)
  - `wasmJsMain/`
- If a new `expect` is added, verify all three `actual` source sets are present in the PR

### Behavioral consistency
- `actual` implementations should provide semantically equivalent behavior across platforms
- Platform-specific limitations should be documented with KDoc on the `expect` declaration
- Error handling should be consistent — if the Android `actual` throws on invalid input, iOS and wasmJs should too

### Type mapping consistency
- Kotlin types used in `expect` signatures should map naturally on all platforms
- Avoid platform-specific types in `expect` signatures (e.g., don't use `android.content.Context` in common code)
- Use `expect`/`actual` for the platform-specific wrapper, not the business logic itself

### Test parity
- If platform-specific behavior differs, tests should exist in each platform's test source set (`androidHostTest`, `iosTest`, `wasmJsTest`)
- Common tests in `commonTest` should cover the shared contract

## Common Mistakes

- Adding `expect`/`actual` for Android and iOS but forgetting wasmJs, which is the one that only breaks the web demo
- Using `iosMain` but not handling both `iosArm64` and `iosSimulatorArm64` (when intermediate source set isn't configured)
- Implementing an `actual` that silently no-ops on one platform while working on others

## Severity

- `🚫 Blocking` — Missing `actual` implementation for any target platform
- `⚠️ Change requested` — Behavioral inconsistency between `actual` implementations without documentation
- `💡 Suggestion` — Missing tests for platform-specific behavior
