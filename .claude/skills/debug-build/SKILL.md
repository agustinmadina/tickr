---
name: debug-build
description: Diagnose and fix Gradle, KMP, or Compose build failures. Parses error output, identifies root causes, and applies fixes.
argument-hint: "[optional module-path or error description]"
disable-model-invocation: true
allowed-tools: Bash(./gradlew:*), Bash(gradle:*), Bash(java:*), Read, Grep, Glob
---

# Debug Build Failure

Diagnose and fix the build failure for `$ARGUMENTS`.

## Step 1: Reproduce the failure

If `$ARGUMENTS` is a module path (e.g., `:features:feature-auth`), run:
```bash
./gradlew $ARGUMENTS:build 2>&1
```

If `$ARGUMENTS` is a description or empty, run the most likely failing command:
```bash
./gradlew build 2>&1
```

Capture the FULL output. If it's truncated, run with `--stacktrace`:
```bash
./gradlew build --stacktrace 2>&1
```

## Step 2: Parse the error

Read the error output carefully. Classify it into one of these categories:

### Category A: Dependency Resolution
**Symptoms:** `Could not resolve`, `Module was compiled with an incompatible version`, `Cannot find`
**Check:**
- `gradle/libs.versions.toml` — version mismatches
- `build.gradle.kts` — incorrect project/library references
- `settings.gradle.kts` — missing module includes
- Run `./gradlew dependencies` on the failing module

### Category B: AGP 9.0 / KMP Plugin
**Symptoms:** `Unresolved reference`, `androidLibrary`, `com.android.kotlin.multiplatform.library` errors
**Check:**
- Plugin declaration uses `alias(libs.plugins.androidKotlinMultiplatformLibrary)` not `com.android.library`
- Android config uses `kotlin { androidLibrary { ... } }` DSL, not `android { ... }`
- `gradle.properties` has `android.disallowKotlinSourceSets=false`
- No `kotlin.android` plugin (built-in with AGP 9.0)

### Category C: Compose Compiler
**Symptoms:** `Compose compiler`, `@Composable`, stability issues, `Unresolved reference: compose`
**Check:**
- Both `composeMultiplatform` and `composeCompiler` plugins are applied
- Compose dependencies use `compose.runtime`, `compose.material3` etc. (not Maven coordinates)
- `core-ui` module is in the dependency chain for UI modules
- Kotlin version is compatible with Compose compiler version

### Category D: Kotlin Multiplatform Source Sets
**Symptoms:** `Unresolved reference` in platform-specific code, `expect`/`actual` mismatches
**Check:**
- `expect` declarations in `commonMain` have matching `actual` in all platform source sets
- Platform-specific imports are only in the correct source set (`androidMain`, `iosMain`, `desktopMain`)
- `val desktopMain by getting` is present when using `jvm("desktop")`
- No Android imports in `commonMain`

### Category E: Kotlin Serialization
**Symptoms:** `Serializer not found`, `@Serializable`, `kotlinx.serialization`
**Check:**
- `kotlinSerialization` plugin is applied in `build.gradle.kts`
- `kotlinx-serialization-json` is in dependencies
- `@Serializable` annotated classes are in the correct source set

### Category F: Koin / DI
**Symptoms:** `No definition found for class`, `KoinApplication has not been started`
**Check:**
- Module is registered in the Koin application setup
- `koin-core` is in `commonMain` dependencies
- `koin-android` is in `androidMain` dependencies
- Correct scope used (single, factory, etc.)

### Category G: iOS / Kotlin Native
**Symptoms:** `Kotlin/Native`, `cinterop`, `framework`, `iosMain`
**Check:**
- iOS targets are declared: `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`
- Framework configuration in `binaries.framework { ... }`
- No JVM-only dependencies leaked into `commonMain`

### Category H: Resource / Configuration
**Symptoms:** `namespace not specified`, `minSdk`, `compileSdk`, `duplicate class`
**Check:**
- `namespace` is set in `kotlin { androidLibrary { namespace = "..." } }`
- `minSdk` and `compileSdk` use `libs.versions.android.minSdk.get().toInt()`
- No duplicate module includes in `settings.gradle.kts`

## Step 3: Investigate root cause

Based on the category, read the relevant files:

1. The failing module's `build.gradle.kts`
2. `gradle/libs.versions.toml` for version info
3. `settings.gradle.kts` for module registration
4. `gradle.properties` for flags
5. The specific source file mentioned in the error

Compare against a working module (e.g., `core/core-domain/build.gradle.kts` or `core/core-network/build.gradle.kts`) to spot differences.

## Step 4: Apply the fix

Fix the root cause. Common fixes:

| Problem | Fix |
|---|---|
| Missing dependency | Add to `build.gradle.kts` dependencies block |
| Version mismatch | Update `libs.versions.toml` |
| Missing module | Add `include()` to `settings.gradle.kts` |
| Wrong plugin | Switch to `androidKotlinMultiplatformLibrary` |
| Missing `actual` | Create platform implementation |
| Namespace missing | Add `namespace = "com.example.app.module.name"` |
| Compose not found | Add `composeMultiplatform` + `composeCompiler` plugins |

## Step 5: Verify the fix

Re-run the build:
```bash
./gradlew $MODULE:build 2>&1
```

If it still fails, go back to Step 2 with the new error. Build errors often cascade — fix them one at a time.

## Step 6: Report

Tell the user:
- **Root cause**: one sentence explaining why it failed
- **Fix applied**: what was changed and where
- **Verification**: confirm the build passes now

## Common AGP 9.0 Gotchas

These trip up frequently and are worth checking proactively:

1. `com.android.library` does NOT work for KMP — must use `com.android.kotlin.multiplatform.library`
2. `android { }` block does NOT exist — use `kotlin { androidLibrary { } }`
3. KSP requires `android.disallowKotlinSourceSets=false` in `gradle.properties`
4. `kotlin("android")` plugin must NOT be applied — AGP 9.0 bundles Kotlin support
5. `jvmTarget` is set via `compilerOptions` inside `androidLibrary`, not a top-level `jvmToolchain`
