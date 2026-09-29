package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The backend's error envelope, e.g.
 * `{"error":"Not Found","code":"access_code_not_found","message":"access code not found"}`.
 * Every field is optional: a proxy or rate limiter in front of the API may
 * answer with something else entirely.
 */
@Serializable
internal data class WandKitApiErrorDto(
    @SerialName("error")
    val error: String? = null,
    @SerialName("code")
    val code: String? = null,
    @SerialName("message")
    val message: String? = null,
)

/** `null` when the body is empty, not JSON, or not shaped like [WandKitApiErrorDto]. */
internal suspend fun HttpResponse.readErrorBodyOrNull(): WandKitApiErrorDto? =
    try {
        body<WandKitApiErrorDto>()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
