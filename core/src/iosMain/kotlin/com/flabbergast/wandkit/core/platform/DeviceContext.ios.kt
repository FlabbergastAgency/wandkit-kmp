package com.flabbergast.wandkit.core.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIDevice

internal actual fun readDeviceContext(platformContext: PlatformContext?): DeviceContext = DeviceContext(
    osVersion = UIDevice.currentDevice.systemVersion,
    appVersion = NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String,
    deviceModel = UIDevice.currentDevice.model,
    locale = NSLocale.currentLocale.localeIdentifier,
)
