---
description: Data-layer classes (repositories, data sources, mappers) must never depend on use cases — use cases sit above repositories in the dependency hierarchy
paths:
  - "features/**/data/**/*.kt"
  - "shared/**/data/**/*.kt"
  - "sdks/**/data/**/*.kt"
---

# Data Layer Use Case Prohibition

## Rule

Repository implementations, data sources, and all other data-layer classes must never depend on use cases. The clean architecture dependency direction is `DataSource → Repository → UseCase → ViewModel` — use cases sit above repositories and orchestrate them. Inverting this creates circular dependencies and collapses the separation between data retrieval and business logic orchestration.

## What to Check

### MUST BLOCK

- A repository constructor (`*RepositoryImpl`) that accepts any type whose name ends in `UseCase` as a parameter
  - **Fix**: Identify what the use case was doing on behalf of the repository. If it was fetching data, call the relevant repository or data source directly from the use case instead. If it was applying business logic before persisting data, that logic belongs in the use case itself — not delegated through a repository that calls back up to another use case
- A data source constructor (`*DataSource`, `*RemoteDataSource`, `*LocalDataSource`, `*Dao`) that accepts any type whose name ends in `UseCase` as a parameter
  - **Fix**: Data sources are the lowest layer — they read and write raw data only. Any logic that requires a use case does not belong here; move it up to the use case layer where it can call both data sources and repositories directly

### MUST FLAG

- Any data-layer class that imports from a use case package (e.g., `dev.madina.tickr.feature.*.domain.usecase.*` or any path segment containing `usecase`) — even if the use case type is not used as a constructor parameter, the import indicates an upward dependency
  - **Fix**: Remove the import and identify why the data-layer class needs domain orchestration logic; that logic belongs in the use case, not in the data layer
- A data-layer class that holds a reference to a `UseCase` type via a property, local variable, or function parameter (rather than a constructor parameter)
  - **Fix**: Same as above — use cases must never be reachable from data-layer code in any form

## Common Mistakes

- Injecting a use case into a repository to "reuse" business logic that was already written — the correct fix is to extract the shared logic into a lower-level component (a data source, manager, or mapper) that both the use case and the repository can depend on independently
- Passing a use case into a data source to transform raw API responses using domain logic — transformation belongs in a mapper or in the use case after it receives raw data from the repository
- Calling a use case from inside a repository's `Flow` transformation chain (e.g., `.map { useCase(it) }`) — this is the same inversion even when it happens inline rather than in a constructor
- Injecting a use case into a repository because the repository needs to "pre-validate" input before persisting — validation of business rules belongs in the use case before it calls the repository; the repository receives already-validated data

## Examples

### Bad

```kotlin
// features/feature-cart/data/src/commonMain/kotlin/.../CartRepositoryImpl.kt
// BAD: ApplyDiscountUseCase is a domain use case — injecting it here inverts the dependency
// direction. CartRepositoryImpl now depends on domain orchestration logic that itself
// depends on CartRepository, creating a circular dependency.
internal class CartRepositoryImpl(
    private val cartApiClient: CartApiClient,
    private val cartLocalDataSource: CartLocalDataSource,
    private val applyDiscountUseCase: ApplyDiscountUseCase, // violation
) : CartRepository {

    override suspend fun saveCart(cart: Cart): Result<Unit> {
        val discounted = applyDiscountUseCase(ApplyDiscountUseCase.Params(cart)) // calling up to domain
            .getOrElse { return Result.failure(it) }
        return cartApiClient.persistCart(discounted)
    }
}
```

```kotlin
// features/feature-order/data/src/commonMain/kotlin/.../OrderRemoteDataSource.kt
// BAD: ValidateOrderUseCase is domain orchestration logic — a data source is a raw I/O
// component and must never reach into the domain use case layer.
internal class OrderRemoteDataSource(
    private val httpClient: HttpClient,
    private val validateOrderUseCase: ValidateOrderUseCase, // violation
) {

    suspend fun submitOrder(order: OrderDto): OrderDto {
        validateOrderUseCase(ValidateOrderUseCase.Params(order.toModel())) // inverted dependency
            .getOrThrow()
        return httpClient.post("/orders") { setBody(order) }.body()
    }
}
```

### Good

```kotlin
// features/feature-cart/domain/src/commonMain/kotlin/.../ApplyDiscountUseCase.kt
// GOOD: The use case sits above the repository. It calls CartRepository to fetch and save data,
// then applies discount logic itself. CartRepositoryImpl has zero knowledge of this use case.
class ApplyDiscountUseCase(
    private val cartRepository: CartRepository,
    private val discountRepository: DiscountRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<ApplyDiscountUseCase.Params, Cart>(coroutineDispatcher) {

    data class Params(val userId: String, val promoCode: String)

    override suspend fun execute(parameters: Params): Cart {
        val cart = cartRepository.getCart(parameters.userId).getOrThrow()
        val discount = discountRepository.getDiscount(parameters.promoCode).getOrThrow()
        val discounted = cart.applyDiscount(discount)
        cartRepository.saveCart(discounted).getOrThrow()
        return discounted
    }
}
```

```kotlin
// features/feature-cart/data/src/commonMain/kotlin/.../CartRepositoryImpl.kt
// GOOD: CartRepositoryImpl depends only on lower-level data components.
// Discount logic lives in ApplyDiscountUseCase (domain layer), not here.
internal class CartRepositoryImpl(
    private val cartApiClient: CartApiClient,
    private val cartLocalDataSource: CartLocalDataSource,
) : CartRepository {

    override suspend fun getCart(userId: String): Result<Cart> =
        cartLocalDataSource.getCart(userId)
            ?: cartApiClient.fetchCart(userId).also { cartLocalDataSource.saveCart(it) }

    override suspend fun saveCart(cart: Cart): Result<Unit> =
        cartApiClient.persistCart(cart.toDto())
}
```

```kotlin
// features/feature-order/data/src/commonMain/kotlin/.../OrderRemoteDataSource.kt
// GOOD: The data source only performs raw I/O. Order validation is the use case's
// responsibility — it runs validation before calling the repository, which calls this data source.
internal class OrderRemoteDataSource(
    private val httpClient: HttpClient,
) {

    suspend fun submitOrder(order: OrderDto): OrderDto =
        httpClient.post("/orders") { setBody(order) }.body()
}
```

## Severity

- `🚫 Blocking` — Repository implementation constructor contains a `UseCase` parameter
- `🚫 Blocking` — Data source constructor contains a `UseCase` parameter
- `⚠️ Change requested` — Data-layer class imports from a use case package or holds a `UseCase` reference in any form
- `💡 Suggestion` — Repository method that applies business logic inline that would be better extracted into a use case