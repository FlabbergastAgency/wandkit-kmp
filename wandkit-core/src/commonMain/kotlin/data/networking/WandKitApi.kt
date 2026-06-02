package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.call.body

internal class WandKitApi<ApiType>(
    private val api: ApiType,
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
        }.onFailure {
            println("[matko] network call failure $it")
        }.mapCatching { response ->
            val body = response.response.body<ApiResult>()
            RemoteSuccess(
                statusCode = response.response.status,
                data = body,
            )
        }.onFailure {
            println("[matko] network response mapping failure $it")
        }
}
