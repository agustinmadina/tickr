package dev.madina.tickr.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the app's HTTP client with the same configuration everywhere.
 *
 * Only the engine differs per platform: OkHttp on Android, Darwin on iOS, and the browser's own
 * networking on web. Ktor has no engine covering all three, so [platformHttpClient] is the seam,
 * and every plugin above it is shared.
 */
fun createHttpClient(): HttpClient =
    platformHttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json(TickrJson)
        }
    }

/**
 * `ignoreUnknownKeys` because these are third-party feeds: an exchange adding a field to its
 * payload must not take the app down, and it will add fields without telling anyone.
 */
val TickrJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

internal expect fun platformHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient
