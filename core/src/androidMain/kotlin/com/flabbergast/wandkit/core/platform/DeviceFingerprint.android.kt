package com.flabbergast.wandkit.core.platform

import android.content.res.Resources
import android.os.Build
import java.util.Locale
import java.util.TimeZone

private const val MILLIS_PER_MINUTE = 60_000

internal actual fun readDeviceFingerprint(): DeviceFingerprint {
    // Resources.getSystem() gives display metrics without a context, so the
    // fingerprint stays available even when the SDK was configured without one.
    val metrics = Resources.getSystem().displayMetrics
    val timeZone = TimeZone.getDefault()
    val locales = Resources.getSystem().configuration.locales

    return DeviceFingerprint(
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        language = locales.takeIf { !it.isEmpty }?.get(0)?.toLanguageTag()
            ?: Locale.getDefault().toLanguageTag(),
        languages = List(locales.size()) { index -> locales[index].toLanguageTag() },
        timezone = timeZone.id,
        timezoneOffsetMinutes = timeZone.getOffset(System.currentTimeMillis()) / MILLIS_PER_MINUTE,
        screenWidth = metrics.widthPixels,
        screenHeight = metrics.heightPixels,
        devicePixelRatio = metrics.density.toDouble(),
    )
}
