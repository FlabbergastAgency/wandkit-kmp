package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.statement.HttpResponse

internal data class WandKitHttpResponse<Response>(
    val response: HttpResponse
)
