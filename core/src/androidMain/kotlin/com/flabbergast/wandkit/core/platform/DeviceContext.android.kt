package com.flabbergast.wandkit.core.platform

import android.os.Build
import java.util.Locale

internal actual fun readDeviceContext(platformContext: PlatformContext?): DeviceContext {
    val context = platformContext?.applicationContext
    val appVersion = context?.let {
        runCatching {
            it.packageManager.getPackageInfo(it.packageName, 0).versionName
        }.getOrNull()
    }

    return DeviceContext(
        osVersion = Build.VERSION.RELEASE,
        appVersion = appVersion,
        deviceModel = Build.MODEL,
        locale = Locale.getDefault().toLanguageTag(),
    )
}
