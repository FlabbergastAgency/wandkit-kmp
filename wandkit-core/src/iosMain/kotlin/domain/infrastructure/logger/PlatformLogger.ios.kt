package com.flabbergast.wandkit.core.domain.infrastructure.logger

import platform.Foundation.NSLog

private const val DEFAULT_TAG = "WandKit"

internal actual fun createPlatformLogger(): Logger = object : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.VERBOSE, tag, message, throwable))
    }

    override fun debug(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.DEBUG, tag, message, throwable))
    }

    override fun info(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.INFO, tag, message, throwable))
    }

    override fun warn(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.WARN, tag, message, throwable))
    }

    override fun error(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.ERROR, tag, message, throwable))
    }

    override fun assert(tag: String?, message: String, throwable: Throwable?) {
        NSLog(formatMessage(LogLevel.ASSERT, tag, message, throwable))
    }
}

private fun formatMessage(
    level: LogLevel,
    tag: String?,
    message: String,
    throwable: Throwable?,
): String {
    val throwableMessage = throwable?.let { "\n$it" }.orEmpty()
    return "[${level.name}] [${tag.orDefaultTag()}] $message$throwableMessage"
}

private fun String?.orDefaultTag(): String = if (isNullOrBlank()) DEFAULT_TAG else this
