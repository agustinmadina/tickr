---
description: Enforce correct coroutine dispatcher usage and thread safety in KMP shared code
paths:
  - "**/commonMain/**/*.kt"
  - "**/src/commonMain/**/*.kt"
  - "**/*ViewModel*.kt"
  - "**/*Repository*.kt"
  - "**/*UseCase*.kt"
  - "**/*DataSource*.kt"
---

# Coroutine & Thread Safety Review

## Rule

Shared KMP code must use coroutine dispatchers correctly and avoid JVM-only or platform-specific threading APIs. All suspending functions and Flow producers must document their threading guarantees.

## Dispatcher Rules

### MUST BLOCK
- `Dispatchers.IO` in `commonMain` — this is JVM-only and will fail to compile on iOS/native
  - **Fix**: Use `Dispatchers.Default` for CPU-bound work, or inject a dispatcher via constructor for I/O operations
- `Dispatchers.Main` in repository or data layer code — data layer should be main-safe by default
  - **Fix**: Use `withContext(Dispatchers.Default)` or an injected dispatcher internally; let the caller choose the final dispatcher
- `runBlocking` in shared code — blocks the thread and can cause deadlocks on main thread
  - **Fix**: Use `suspend` functions or `coroutineScope { }` instead
- `GlobalScope.launch` — leaks coroutines, no structured concurrency
  - **Fix**: Use a scoped `CoroutineScope` injected via constructor or `viewModelScope`

### MUST FLAG
- `Dispatchers.Unconfined` — rarely correct, causes non-deterministic execution
  - **Acceptable**: Only in tests or specific framework integration points
- `withContext(Dispatchers.Main)` in ViewModel — usually unnecessary since `viewModelScope` already uses Main
- `Thread.sleep()` or `Object.wait()` in shared code — JVM-only blocking calls
  - **Fix**: Use `delay()` instead
- `synchronized` blocks in `commonMain` — JVM-only construct
  - **Fix**: Use `kotlinx.atomicfu` or `Mutex` from `kotlinx.coroutines.sync`

## Flow Safety

### Proper Flow usage
- `StateFlow` and `SharedFlow` should be used for state management in ViewModels, not `MutableLiveData`
- `flow { }` builders should not perform UI operations
- `.stateIn()` and `.shareIn()` must specify appropriate `SharingStarted` strategy:
  - `SharingStarted.WhileSubscribed(5000)` for UI-bound flows (allows resubscription without re-fetch)
  - `SharingStarted.Lazily` for data that should survive configuration changes
  - `SharingStarted.Eagerly` only when the flow must start immediately regardless of subscribers
- `flowOn()` should be used to specify the upstream dispatcher, not `withContext` wrapping a `flow { }` block

### suspend modifier on Flow-returning functions

Functions that return `Flow<T>`, `StateFlow<T>`, `SharedFlow<T>`, or `Channel<T>` must NOT be `suspend`.

**Why**: These types are already asynchronous by nature — a cold `Flow` is lazy and does no work until collected; a `StateFlow`/`SharedFlow` is a hot stream that exists independently of the call. The function call only creates or returns the stream; no suspension happens at call time. Marking such a function `suspend` forces every caller into a coroutine context unnecessarily and signals incorrect intent to readers.

#### MUST FLAG
- `suspend fun observeX(): Flow<X>` in a repository interface or implementation
  - **Fix**: Remove `suspend` — `fun observeX(): Flow<X>`
- `suspend fun getAuthState(): StateFlow<AuthState>` in a ViewModel or repository
  - **Fix**: Remove `suspend`, or better, expose as a property `val authState: StateFlow<AuthState>`
- `suspend fun events(): SharedFlow<Event>` in any layer
  - **Fix**: Remove `suspend` — `fun events(): SharedFlow<Event>`
- `suspend fun channel(): Channel<T>` — same reasoning
  - **Fix**: Remove `suspend` — `fun channel(): Channel<T>`

#### One narrow exception
A function that must perform a suspending operation **before** it can construct and return the flow (e.g., performing an authenticated network handshake to obtain a session token that the resulting flow will use) may be `suspend`. This pattern should itself be avoided — prefer lazy initialisation inside the flow body using `flow { val token = fetchToken(); emit(…) }` — but if it is unavoidable, it must be documented with a KDoc comment explaining why suspension at the call site is required.

### Common Flow mistakes
- Collecting a Flow inside another `flow { }` builder without `emitAll()` or `flatMapLatest`
- Using `collect` in a `launch` block without cancellation handling — for ongoing Flow observation in ViewModels, prefer `.onEach { }.catch { }.launchIn(viewModelScope)` over `launch { collect { } }` (see `viewmodel-flow-collection.md`)
- Creating hot flows (`SharedFlow`) without a replay/buffer strategy
- Not using `.catch { }` operator for error handling in Flow chains

## ViewModel Threading

- `viewModelScope` dispatches on `Dispatchers.Main` by default — that's correct for state updates
- Long-running operations must be moved off Main: use `withContext(Dispatchers.Default)` or call suspend functions from repositories that handle their own dispatching
- Never call `delay()` on Main for anything other than UI debouncing

## Severity

- `🚫 Blocking` — `Dispatchers.IO` in commonMain, `runBlocking` in shared code, `GlobalScope` usage, `Thread.sleep()` in commonMain
- `⚠️ Change requested` — `suspend` modifier on a function returning `Flow`, `StateFlow`, `SharedFlow`, or `Channel`; missing `flowOn()`; incorrect `SharingStarted` strategy; `Dispatchers.Unconfined` without justification
- `💡 Suggestion` — Dispatcher injection for testability, `WhileSubscribed` timeout tuning
