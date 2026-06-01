package com.flabbergast.wandkit.core.config

internal data class AppConfiguration(
    val baseUrl: String,
    val libraryVersion: String,
    val platformName: String,
    val platformVersion: String,
)

private const val BASE_URL = "http://localhost:8080"

internal fun createAppConfiguration() = AppConfiguration(
    baseUrl = BASE_URL,
    libraryVersion = LibraryBuildInfo.VERSION,
    platformName = PlatformInfo.name,
    platformVersion = PlatformInfo.version,
)
