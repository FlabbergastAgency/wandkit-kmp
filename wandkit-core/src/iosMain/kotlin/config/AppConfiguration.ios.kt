package com.flabbergast.wandkit.core.config

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

internal actual object PlatformInfo {
    actual val name = UIDevice.currentDevice.systemName()
    actual val version = UIDevice.currentDevice.systemVersion
    actual val libraryVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as String
}