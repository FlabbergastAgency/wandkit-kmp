package com.flabbergast.wandkit.core.data.networking

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

private const val LOGGER_TAG = "[WandKitApi]"

internal class WandKitApi<ApiType>(
    private val api: ApiType,
    private val logger: Logger,
) {
    suspend inline operator fun <reified ApiResult : Any> invoke(
        crossinline apiCall: suspend ApiType.() -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>> =
        safeApiCall {
            apiCall(api)
        }

    /**
     * Like [invoke], but only checks the status code rather than deserializing
     * a body - for calls whose response isn't ours to parse (a presigned
     * upload PUT may answer with the storage provider's own XML, or nothing
     * at all).
     */
    suspend inline fun status(
        crossinline apiCall: suspend ApiType.() -> WandKitHttpResponse<*>,
    ): Result<Unit> =
        runCatching {
            apiCall(api)
        }.mapCatching { response ->
            if (response.response.status.value !in 200..299) {
                throw WandKitHttpException(response.response.status.value)
            }
        }.onFailure {
            logger.warn(LOGGER_TAG, "Network call failed.", it)
        }

    /**
     * Like [invoke], but reads [absentStatus] as "nothing here" rather than a
     * failure - the ordinary case of an install with no referral yet, which must
     * not surface as an error to the app or be logged as one.
     *
     * Which status carries that meaning is per-endpoint and deliberately explicit:
     * detect answers 204 with an empty body, while progress still answers 404.
     */
    suspend inline fun <reified ApiResult : Any> optional(
        absentStatus: HttpStatusCode,
        crossinline apiCall: suspend ApiType.() -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>?> =
        safeOptionalApiCall(absentStatus) {
            apiCall(api)
        }

    private suspend inline fun <reified ApiResult : Any> safeOptionalApiCall(
        absentStatus: HttpStatusCode,
        block: suspend () -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>?> =
        runCatching {
            block()
        }.mapCatching { response ->
            when {
                // Ahead of the 2xx branch on purpose: a 204 is inside that range
                // and has no body, so letting it reach the deserializer would
                // throw and read as a failure rather than an answer.
                response.response.status == absentStatus -> null
                response.response.status.value in 200..299 -> RemoteSuccess(
                    statusCode = response.response.status,
                    data = response.response.body<ApiResult>(),
                )
                else -> throw WandKitHttpException(response.response.status.value)
            }
        }.onFailure {
            logger.warn(LOGGER_TAG, "Network call failed.", it)
        }

    private suspend inline fun <reified ApiResult : Any> safeApiCall(
        block: suspend () -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>> =
        runCatching {
            block()
        }.mapCatching { response ->
            when (response.response.status.value) {
                in 200..299 -> {
                    val body = response.response.body<ApiResult>()
                    RemoteSuccess(
                        statusCode = response.response.status,
                        data = body,
                    )
                }
                else -> throw WandKitHttpException(response.response.status.value)
            }

        }.onFailure {
            logger.warn(LOGGER_TAG, "Network call failed.", it)
        }
}
