package com.flabbergast.wandkit.core.platform

/**
 * The device traits the backend fingerprints an install by.
 *
 * None of it identifies a person; it is the coarse shape of a device, used only
 * to match an app open against a link that was tapped moments earlier.
 */
internal data class DeviceFingerprint(
    val deviceModel: String,
    val language: String,
    val languages: List<String>,
    val timezone: String,
    val timezoneOffsetMinutes: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val devicePixelRatio: Double,
)

/**
 * Suspending because UIKit screen metrics must be read on the main thread, and
 * detection runs on a background dispatcher.
 */
internal expect suspend fun readDeviceFingerprint(): DeviceFingerprint
