package com.flabbergast.wandkit.core.platform

/**
 * The device/app fields the backend accepts on a form response submission.
 *
 * Deliberately smaller than [DeviceFingerprint]: this rides along with an
 * already-identified submission and only needs the fields the dashboard shows,
 * not the anti-fraud fingerprint fields (screen size, timezone, ...).
 */
internal data class DeviceContext(
    val osVersion: String?,
    val appVersion: String?,
    val deviceModel: String?,
    val locale: String?,
)

/**
 * Synchronous, unlike [readDeviceFingerprint]: none of these reads touch UIKit
 * layout state, so there is no need to hop to the main thread.
 */
internal expect fun readDeviceContext(platformContext: PlatformContext?): DeviceContext
