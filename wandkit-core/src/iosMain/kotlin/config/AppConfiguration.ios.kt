package com.flabbergast.wandkit.core.config

import platform.UIKit.UIDevice

internal actual object PlatformInfo {
    actual val name = UIDevice.currentDevice.systemName()
    actual val version = UIDevice.currentDevice.systemVersion
}
