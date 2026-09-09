# Tickr

A crypto portfolio tracker with live prices, written once in Kotlin Multiplatform and running as
a native Android app, a native iOS app and a web app.

**[Open the web app](https://agustinmadina.github.io/tickr/)** to see it without installing anything.
It is the same Compose UI as the phone builds, compiled to WebAssembly.

## Why this project

Most KMP samples share a data layer and stop there. This one shares the entire UI as well, so the
three clients are one codebase and one design, and the interesting problems are the ones you only
hit when you actually do that: a live WebSocket feed that behaves on all three platforms, storage
that has no single answer across Android, iOS and a browser, and money arithmetic that must not
drift.

## What it does

- Live prices over a WebSocket feed, so values move on screen instead of refreshing on a timer
- Holdings with average cost, unrealised profit and loss, and allocation
- Works offline against the last known prices
- One UI, three platforms, no per-platform screens

## Stack

| Concern | Choice |
|---|---|
| UI | Compose Multiplatform |
| Async | Coroutines and Flow |
| Networking | Ktor client, WebSocket for the price feed |
| Persistence | SQLDelight on Android and iOS, browser storage on web |
| DI | Koin |
| Tests | Kotest |

## Architecture

Clean architecture with the boundaries enforced by the build system rather than by convention:
each feature is four Gradle modules, and a violation of the dependency direction fails to compile.

```
core/         domain-agnostic infrastructure (network, database, design system)
features/     one user-facing slice each, split into domain / data / ui / di
shared/       business logic used by more than one feature
```

- `domain` has no framework dependencies at all, only Kotlin and coroutines
- `ui` depends on `domain`, never on `data`
- `di` is the only module that sees all three, and is where implementations bind to interfaces

## Running it

```shell
./gradlew :androidApp:installDebug          # Android
./gradlew :shared:wasmJsBrowserDevelopmentRun  # web, on localhost
open iosApp/iosApp.xcodeproj                # iOS, then run from Xcode
```

## Status

Work in progress. See the commit history for what has landed.
