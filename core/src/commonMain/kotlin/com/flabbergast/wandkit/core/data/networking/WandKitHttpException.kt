package com.flabbergast.wandkit.core.data.networking

/**
 * A non-2xx response, carrying the status so callers can decide whether trying
 * again could plausibly give a different answer.
 *
 * [errorBody] is only read for calls made through
 * [WandKitApi.invokeReadingErrorBody]; every other call leaves it `null`.
 */
internal class WandKitHttpException(
    val statusCode: Int,
    val errorBody: WandKitApiErrorDto? = null,
) : Exception("Non 2xx response code: $statusCode")
