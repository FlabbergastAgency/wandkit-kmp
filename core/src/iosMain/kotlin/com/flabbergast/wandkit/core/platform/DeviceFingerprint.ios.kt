package com.flabbergast.wandkit.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages
import platform.Foundation.secondsFromGMT
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

private const val SECONDS_PER_MINUTE = 60

@OptIn(ExperimentalForeignApi::class)
internal actual fun readDeviceFingerprint(): DeviceFingerprint {
    val screen = UIScreen.mainScreen
    val size = screen.bounds.useContents { size.width to size.height }
    val languages = NSLocale.preferredLanguages.filterIsInstance<String>()
    val timeZone = NSTimeZone.localTimeZone

    return DeviceFingerprint(
        deviceModel = UIDevice.currentDevice.model,
        language = languages.firstOrNull() ?: NSLocale.currentLocale.localeIdentifier,
        languages = languages,
        timezone = timeZone.name,
        timezoneOffsetMinutes = timeZone.secondsFromGMT.toInt() / SECONDS_PER_MINUTE,
        screenWidth = size.first.toInt(),
        screenHeight = size.second.toInt(),
        devicePixelRatio = screen.scale,
    )
}
