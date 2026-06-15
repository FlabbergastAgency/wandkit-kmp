package com.flabbergast.wandkit.core.config

import com.flabbergast.wandkit.core.domain.infrastructure.logger.LogLevel

internal data class AppConfiguration(
    val baseUrl: String,
    val libraryVersion: String,
    val platformName: String,
    val platformVersion: String,
    val logLevel: LogLevel,
)

private const val BASE_URL = "https://api.wandkit.flabic.com"

internal fun createAppConfiguration(isDebugLoggingEnabled: Boolean) = AppConfiguration(
    baseUrl = BASE_URL,
    libraryVersion = LibraryBuildInfo.VERSION,
    platformName = PlatformInfo.name,
    platformVersion = PlatformInfo.version,
    logLevel = if (isDebugLoggingEnabled) LogLevel.DEBUG else LogLevel.NONE,
)
