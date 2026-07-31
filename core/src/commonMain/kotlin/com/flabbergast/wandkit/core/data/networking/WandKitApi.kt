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
     * Like [invoke], but reads 404 as "nothing here" rather than a failure.
     *
     * Detection and progress both answer 404 for the ordinary case of an install
     * with no referral yet, which must not surface as an error to the app or be
     * logged as one.
     */
    suspend inline fun <reified ApiResult : Any> optional(
        crossinline apiCall: suspend ApiType.() -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>?> =
        safeOptionalApiCall {
            apiCall(api)
        }

    private suspend inline fun <reified ApiResult : Any> safeOptionalApiCall(
        block: suspend () -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>?> =
        runCatching {
            block()
        }.mapCatching { response ->
            when {
                response.response.status.value in 200..299 -> RemoteSuccess(
                    statusCode = response.response.status,
                    data = response.response.body<ApiResult>(),
                )
                response.response.status == HttpStatusCode.NotFound -> null
                else -> throw Error("Non 2xx response code: ${response.response.status}")
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
                else -> throw Error("Non 2xx response code: ${response.response.status}")
            }

        }.onFailure {
            logger.warn(LOGGER_TAG, "Network call failed.", it)
        }
}
