package com.flabbergast.wandkit.core.domain.infrastructure.logger

import android.util.Log

private const val DEFAULT_TAG = "WandKit"

internal actual fun createPlatformLogger(): Logger = object : Logger {
    override fun verbose(tag: String?, message: String, throwable: Throwable?) {
        Log.v(tag.orDefaultTag(), message, throwable)
    }

    override fun debug(tag: String?, message: String, throwable: Throwable?) {
        Log.d(tag.orDefaultTag(), message, throwable)
    }

    override fun info(tag: String?, message: String, throwable: Throwable?) {
        Log.i(tag.orDefaultTag(), message, throwable)
    }

    override fun warn(tag: String?, message: String, throwable: Throwable?) {
        Log.w(tag.orDefaultTag(), message, throwable)
    }

    override fun error(tag: String?, message: String, throwable: Throwable?) {
        Log.e(tag.orDefaultTag(), message, throwable)
    }

    override fun assert(tag: String?, message: String, throwable: Throwable?) {
        Log.wtf(tag.orDefaultTag(), message, throwable)
    }
}

private fun String?.orDefaultTag(): String = if (isNullOrBlank()) DEFAULT_TAG else this
