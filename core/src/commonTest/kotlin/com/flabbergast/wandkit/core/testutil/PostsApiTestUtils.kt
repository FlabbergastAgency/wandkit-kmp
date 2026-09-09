package com.flabbergast.wandkit.core.testutil

import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.createCommonInterceptor
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.data.posts.createPostsApi
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/** A [Logger] that discards everything - for tests that don't assert on logging. */
internal object NoOpLogger : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun debug(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun info(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun warn(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String?, message: String, throwable: Throwable?) = Unit
    override fun assert(tag: String?, message: String, throwable: Throwable?) = Unit
}

/** A [PostsApi] backed by a [MockEngine] built from [handler]. */
internal fun createTestPostsApi(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): WandKitApi<PostsApi> = createTestPostsApi(MockEngine { request -> handler(request) })

/**
 * A [PostsApi] wired onto a caller-supplied [engine] - use this overload when
 * the test needs to inspect the engine afterwards (e.g. `engine.requestHistory`).
 */
internal fun createTestPostsApi(engine: MockEngine): WandKitApi<PostsApi> {
    val json = createJson()
    val client = HttpClient(engine) {
        // Mirrors the DefaultRequest block installed by the real
        // createHttpClient() - without an outgoing Content-Type,
        // ContentNegotiation skips serializing the request body entirely.
        install(DefaultRequest) { contentType(ContentType.Application.Json) }
        install(ContentNegotiation) { json(json) }
    }

    return createPostsApi(
        httpClient = WandKitHttpClient(client),
        baseUrl = "https://example.test",
        logger = NoOpLogger,
    )
}

/** Like [createTestPostsApi], but also wires [createCommonInterceptor] the way the real `createHttpClient()` does. */
internal fun createTestPostsApiWithInterceptor(
    apiHost: String,
    apiKey: String = "test-api-key",
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): WandKitApi<PostsApi> {
    val json = createJson()
    val engine = MockEngine { request -> handler(request) }
    val baseUrl = "https://$apiHost"
    val commonInterceptor = createCommonInterceptor(apiKey = apiKey, baseUrl = baseUrl)
    val client = HttpClient(engine) {
        install(DefaultRequest) { contentType(ContentType.Application.Json) }
        install(ContentNegotiation) { json(json) }
    }
    client.plugin(HttpSend).intercept { request ->
        commonInterceptor.intercept(request)
        execute(request)
    }

    return createPostsApi(
        httpClient = WandKitHttpClient(client),
        baseUrl = baseUrl,
        logger = NoOpLogger,
    )
}
