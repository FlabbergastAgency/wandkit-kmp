package com.flabbergast.wandkit.core.domain.infrastructure.logger

internal enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    ASSERT,
    NONE,
    ;

    fun allows(messageLevel: LogLevel): Boolean {
        if (this == NONE) return false
        return messageLevel.ordinal >= ordinal
    }
}
