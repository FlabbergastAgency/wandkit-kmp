package com.flabbergast.wandkit.core.domain.infrastructure.logger

internal fun createAppLogger(logLevel: LogLevel): Logger = BasicLogger(logLevel, createPlatformLogger())

private class BasicLogger(
    private val logLevel: LogLevel,
    private val delegate: Logger,
) : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.VERBOSE, tag, message, throwable, delegate::verbose)
    }

    override fun debug(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.DEBUG, tag, message, throwable, delegate::debug)
    }

    override fun info(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.INFO, tag, message, throwable, delegate::info)
    }

    override fun warn(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.WARN, tag, message, throwable, delegate::warn)
    }

    override fun error(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, tag, message, throwable, delegate::error)
    }

    override fun assert(tag: String?, message: String, throwable: Throwable?) {
        log(LogLevel.ASSERT, tag, message, throwable, delegate::assert)
    }

    private fun log(
        messageLevel: LogLevel,
        tag: String?,
        message: String,
        throwable: Throwable?,
        writer: (String?, String, Throwable?) -> Unit,
    ) {
        if (!logLevel.allows(messageLevel)) return
        writer(tag, message, throwable)
    }
}
