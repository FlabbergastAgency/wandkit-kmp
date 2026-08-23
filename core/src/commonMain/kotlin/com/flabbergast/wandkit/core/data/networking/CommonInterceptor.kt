package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.Url

private const val API_KEY_HEADER = "X-API-Key"

/**
 * Appends the project API key - but only to requests aimed at our own API.
 * Some requests (presigned attachment uploads) go straight to a third-party
 * host, and the key must never leak there.
 */
internal class CommonInterceptor(
    private val apiKey: String,
    private val apiHost: String,
) {
    fun intercept(request: HttpRequestBuilder) {
        if (request.url.host != apiHost) return

        request.headers {
            append(API_KEY_HEADER, apiKey)
        }
    }
}

internal fun createCommonInterceptor(apiKey: String, baseUrl: String) = CommonInterceptor(
    apiKey = apiKey,
    apiHost = Url(baseUrl).host,
)
