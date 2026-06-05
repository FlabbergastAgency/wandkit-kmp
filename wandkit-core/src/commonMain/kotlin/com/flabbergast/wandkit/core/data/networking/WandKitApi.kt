package com.flabbergast.wandkit.core.data.networking

import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import io.ktor.client.call.body

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

    private suspend inline fun <reified ApiResult : Any> safeApiCall(
        block: suspend () -> WandKitHttpResponse<ApiResult>,
    ): Result<RemoteSuccess<ApiResult>> =
        runCatching {
            block()
        }.mapCatching { response ->
            val body = response.response.body<ApiResult>()
            RemoteSuccess(
                statusCode = response.response.status,
                data = body,
            )
        }.onFailure {
            logger.warn(LOGGER_TAG, "Network call failed.", it)
        }
}
