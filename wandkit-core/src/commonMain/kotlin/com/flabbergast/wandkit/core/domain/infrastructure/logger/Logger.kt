package com.flabbergast.wandkit.core.domain.infrastructure.logger

internal interface Logger {
    fun verbose(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )

    fun debug(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )

    fun info(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )

    fun warn(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )

    fun error(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )

    fun assert(
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    )
}