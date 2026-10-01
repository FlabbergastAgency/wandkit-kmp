package com.flabbergast.wandkit.core.config

import com.flabbergast.wandkit.core.domain.infrastructure.logger.LogLevel

internal data class AppConfiguration(
    val baseUrl: String,
    val libraryVersion: String,
    val platformName: String,
    val platformVersion: String,
    val logLevel: LogLevel,
) {
    /**
     * The canonical lowercase value the backend keys applications on
     * (`ios` / `android`), as opposed to [platformName], which is the OS's own
     * spelling and stays in the user agent. iPadOS is an iOS application as far
     * as the backend is concerned, and UIDevice reports it by its own name.
     */
    val platform: String
        get() = platformName.lowercase().let { if (it == "ipados") "ios" else it }
}

private const val BASE_URL = "https://api.wandkit.flabic.com"

internal fun createAppConfiguration(
    isDebugLoggingEnabled: Boolean,
    apiBaseUrl: String? = null,
) = AppConfiguration(
    baseUrl = apiBaseUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: BASE_URL,
    libraryVersion = LibraryBuildInfo.VERSION,
    platformName = PlatformInfo.name,
    platformVersion = PlatformInfo.version,
    logLevel = if (isDebugLoggingEnabled) LogLevel.DEBUG else LogLevel.NONE,
)
