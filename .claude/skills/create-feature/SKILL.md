---
name: create-feature
description: Scaffold a new KMP feature module with 4 Gradle sub-modules (domain, data, ui, di) following clean architecture.
argument-hint: "[feature-name]"
disable-model-invocation: true
---

# Create Feature Module

Scaffold a new feature module named `$ARGUMENTS` with 4 Gradle sub-modules following the project's clean architecture conventions.

## Variables

Derive these from the feature name argument:

- **FEATURE_NAME**: `$0` (e.g., `auth`, `home`, `settings`)
- **FEATURE_PACKAGE**: dots version of feature name (e.g., `auth`, `user.profile`)
- **MODULE_PATH**: `features/feature-$0`
- **GRADLE_PATH**: `:features:feature-$0`
- **PASCAL**: PascalCase of feature name (e.g., `auth` -> `Auth`, `user-profile` -> `UserProfile`)
- **CAMEL**: camelCase of feature name (e.g., `auth` -> `auth`, `user-profile` -> `userProfile`)
- **SNAKE**: snake_case of feature name (e.g., `auth` -> `auth`, `user-profile` -> `user_profile`)
- **PACKAGE_PATH_BASE**: `com/example/app/feature/$0`

## Templates

All templates are in `.claude/skills/create-feature/templates/`. Read each template file, replace placeholders (`{{FEATURE_NAME}}`, `{{FEATURE_PACKAGE}}`, `{{PASCAL}}`, `{{CAMEL}}`, `{{SNAKE}}`), and write to the correct destination.

## Step 1: Create directory structure

Create the 4 sub-module structure:

```
features/feature-$0/
+-- domain/
|   +-- build.gradle.kts
|   +-- src/commonMain/kotlin/com/example/app/feature/$0/domain/
|       +-- model/
|       +-- repository/
|       +-- usecase/
|
+-- data/
|   +-- build.gradle.kts
|   +-- src/
|       +-- commonMain/kotlin/com/example/app/feature/$0/data/
|       |   +-- di/
|       |   +-- mapper/
|       |   +-- repository/
|       +-- androidMain/kotlin/com/example/app/feature/$0/data/
|       +-- iosMain/kotlin/com/example/app/feature/$0/data/
|       +-- desktopMain/kotlin/com/example/app/feature/$0/data/
|
+-- ui/
|   +-- build.gradle.kts
|   +-- src/
|       +-- commonMain/
|       |   +-- kotlin/com/example/app/feature/$0/ui/
|       |   |   +-- di/
|       |   +-- composeResources/values/
|       +-- androidMain/kotlin/com/example/app/feature/$0/ui/
|       +-- iosMain/kotlin/com/example/app/feature/$0/ui/
|       +-- desktopMain/kotlin/com/example/app/feature/$0/ui/
|
+-- di/
    +-- build.gradle.kts
    +-- src/commonMain/kotlin/com/example/app/feature/$0/di/
```

Use `mkdir -p` to create all directories.

## Step 2: Create build.gradle.kts files

Read and apply each template with placeholder substitution:

| Template file | Destination |
|---|---|
| `templates/build.gradle.kts.domain` | `features/feature-$0/domain/build.gradle.kts` |
| `templates/build.gradle.kts.data` | `features/feature-$0/data/build.gradle.kts` |
| `templates/build.gradle.kts.ui` | `features/feature-$0/ui/build.gradle.kts` |
| `templates/build.gradle.kts.di` | `features/feature-$0/di/build.gradle.kts` |

NOTE: Add `core-network` or `core-storage` to data only if the feature needs them. Ask the user if unsure.

## Step 3: Create starter Kotlin files

Read and apply each template with placeholder substitution:

| Template file | Destination |
|---|---|
| `templates/Repository.kt` | `features/feature-$0/domain/src/commonMain/kotlin/.../domain/repository/${PASCAL}Repository.kt` |
| `templates/RepositoryImpl.kt` | `features/feature-$0/data/src/commonMain/kotlin/.../data/repository/${PASCAL}RepositoryImpl.kt` |
| `templates/DataModule.kt` | `features/feature-$0/data/src/commonMain/kotlin/.../data/di/${PASCAL}DataModule.kt` |
| `templates/UiState.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/${PASCAL}UiState.kt` |
| `templates/Action.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/${PASCAL}Action.kt` |
| `templates/Effect.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/${PASCAL}Effect.kt` |
| `templates/ViewModel.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/${PASCAL}ViewModel.kt` |
| `templates/Screen.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/${PASCAL}Screen.kt` |
| `templates/UiModule.kt` | `features/feature-$0/ui/src/commonMain/kotlin/.../ui/di/${PASCAL}UiModule.kt` |
| `templates/FeatureModule.kt` | `features/feature-$0/di/src/commonMain/kotlin/.../di/${PASCAL}Module.kt` |
| `templates/strings.xml` | `features/feature-$0/ui/src/commonMain/composeResources/values/strings.xml` |

Replace `...` with the full package path `com/example/app/feature/$0`.

### Template compliance with project rules

These templates are pre-validated against `.claude/rules/`:

- **visibility-modifiers**: RepositoryImpl, ViewModel, UiState, Action, Effect are `internal`. Repository interface and DI module vals are `public`.
- **viewmodel-visibility**: ViewModel is `internal`, uses BaseViewModel pattern.
- **viewmodel-state-source-of-truth**: State mutations via `updateState { it.copy(...) }`.
- **material3-components**: Screen uses only `material3` imports.
- **composable-no-hardcoded-strings**: strings.xml created with initial resource key.
- **model-layering-naming-conventions**: UiState suffix on UI model.
- **di-mapper-prohibition**: No mappers registered in Koin modules.
- **use-case-pattern**: No use case in starter (added when needed, must extend UseCase base).
- **error-logging-requirement**: BaseViewModel provides `log` via Kermit.

## Step 4: Register in settings.gradle.kts

Add all 4 sub-modules under the `// Feature modules` section:

```kotlin
include(":features:feature-$0:domain")
include(":features:feature-$0:data")
include(":features:feature-$0:ui")
include(":features:feature-$0:di")
```

## Step 5: Verify

Run these to confirm all sub-modules compile:

```shell
./gradlew :features:feature-$0:domain:assemble
./gradlew :features:feature-$0:data:assemble
./gradlew :features:feature-$0:ui:assemble
./gradlew :features:feature-$0:di:assemble
```

## Rules

- Use the EXACT build.gradle.kts patterns from the templates (AGP 9.0 `com.android.kotlin.multiplatform.library` plugin)
- Package names use dots: `com.example.app.feature.<name>.<layer>`
- Directory paths use slashes: `com/example/app/feature/<name>/<layer>`
- If the feature name contains hyphens (e.g., `user-profile`), convert to dots for package (`user.profile`) and camelCase/PascalCase for class names (`UserProfile`)
- Keep starter files minimal — just enough to compile
- Do NOT add the module as a dependency to `sharedLib` or `androidApp` unless the user asks
- The `ui` sub-module must NEVER depend on the `data` sub-module — enforced at Gradle level
- Only the `di` sub-module sees all three layers
