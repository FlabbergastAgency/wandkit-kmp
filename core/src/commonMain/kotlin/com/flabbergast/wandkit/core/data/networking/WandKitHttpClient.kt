package com.flabbergast.wandkit.core.data.networking

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.jvm.JvmInline

private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L

@JvmInline
internal value class WandKitHttpClient(
    val client: HttpClient
)

private const val LOGGER_TAG = "[KtorHttpClient]"

internal fun createHttpClient(
    json: Json,
    commonInterceptor: CommonInterceptor,
    appLogger: Logger,
): WandKitHttpClient {
    val client = HttpClient {
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    appLogger.debug(LOGGER_TAG, message)
                }

            }
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
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