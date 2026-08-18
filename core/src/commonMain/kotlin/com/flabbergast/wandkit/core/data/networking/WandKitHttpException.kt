package com.flabbergast.wandkit.core.data.networking

/**
 * A non-2xx response, carrying the status so callers can decide whether trying
 * again could plausibly give a different answer.
 */
internal class WandKitHttpException(
    val statusCode: Int,
) : Exception("Non 2xx response code: $statusCode")
