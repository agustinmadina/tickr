package dev.madina.tickr.core.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

/**
 * The browser's own networking. Web sockets from a page are not subject to CORS preflight, which is
 * why the exchange feed can be read directly from the web build with no proxy in between.
 */
internal actual fun platformHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js) { configure() }
