package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers

private const val API_KEY_HEADER = "X-API-Key"

internal class CommonInterceptor(
    private val apiKey: String
) {
    fun intercept(request: HttpRequestBuilder) {
        request.headers {
            append(API_KEY_HEADER, apiKey)
        }
    }
}

internal fun createCommonInterceptor(apiKey: String) = CommonInterceptor(apiKey)