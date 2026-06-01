package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmInline

@JvmInline
internal value class WandKitHttpClient(
    val client: HttpClient
)

internal fun createHttpClient(
    json: Json,
    commonInterceptor: CommonInterceptor,
): WandKitHttpClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    client.plugin(HttpSend).intercept { request ->
        commonInterceptor.intercept(request)
        execute(request)
    }

    return WandKitHttpClient(client)
}

internal fun createJson() = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}