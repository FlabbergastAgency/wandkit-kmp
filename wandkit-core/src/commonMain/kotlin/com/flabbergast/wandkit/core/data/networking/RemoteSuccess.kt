package com.flabbergast.wandkit.core.data.networking

import io.ktor.http.HttpStatusCode

internal data class RemoteSuccess<T>(
    val statusCode: HttpStatusCode,
    val data: T,
)