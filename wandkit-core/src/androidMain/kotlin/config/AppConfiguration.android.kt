package com.flabbergast.wandkit.core.config

import android.os.Build

internal actual object PlatformInfo {
    actual val name = "Android"
    actual val version = Build.VERSION.SDK_INT.toString()
    actual val libraryVersion = Build.VERSION.RELEASE.toString()
}