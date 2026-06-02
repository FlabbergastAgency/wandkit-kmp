package com.flabbergast.wandkit.core.config

internal data class AppConfiguration(
    val baseUrl: String,
    val libraryVersion: String,
    val platformName: String,
    val platformVersion: String,
    val isLoggingEnabled: Boolean,
)

private const val BASE_URL = "https://api.wandkit.flabic.com"

internal fun createAppConfiguration(isLoggingEnabled: Boolean) = AppConfiguration(
    baseUrl = BASE_URL,
    libraryVersion = LibraryBuildInfo.VERSION,
    platformName = PlatformInfo.name,
    platformVersion = PlatformInfo.version,
    isLoggingEnabled = isLoggingEnabled,
)
