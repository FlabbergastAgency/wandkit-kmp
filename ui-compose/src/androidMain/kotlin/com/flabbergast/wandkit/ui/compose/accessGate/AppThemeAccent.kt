package com.flabbergast.wandkit.ui.compose.accessGate

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.ui.graphics.Color

/**
 * The host app's own accent, read from the application theme (the gate runs
 * under its own theme): AppCompat / Material Components `colorPrimary`, then
 * the platform `android:colorPrimary` and `android:colorAccent`.
 *
 * Only a value the app (or a library it bundles) set counts. The framework
 * themes' defaults - Theme.Material's grey primary, DeviceDefault's
 * wallpaper colours - say nothing about the brand, so they are skipped and
 * the gate keeps the SDK's tint instead. `null` when nothing qualifies.
 */
internal fun Context.appThemeAccentColor(): Color? {
    val themeRes = applicationInfo.theme
    if (themeRes == 0) return null
    val theme = runCatching { ContextThemeWrapper(this, themeRes).theme }.getOrNull() ?: return null

    val candidates = listOfNotNull(
        resources.getIdentifier("colorPrimary", "attr", packageName).takeIf { it != 0 },
        android.R.attr.colorPrimary,
        android.R.attr.colorAccent,
    )
    for (attr in candidates) {
        val value = TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) continue
        if (isFrameworkDefault(value)) continue
        val argb = when {
            value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT -> value.data
            value.resourceId != 0 -> runCatching { getColorStateList(value.resourceId).defaultColor }.getOrNull()
            else -> null
        } ?: continue
        // A translucent primary would not make a solid button; treat it as unset.
        if ((argb ushr 24) != 0xFF) continue
        return Color(argb)
    }
    return null
}

private fun Context.isFrameworkDefault(value: TypedValue): Boolean {
    // The style the value came from tells best (API 29+); otherwise the
    // colour resource it points at.
    val sourceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) value.sourceResourceId else 0
    val id = if (sourceId != 0) sourceId else value.resourceId
    if (id == 0) return false
    val packageName = runCatching { resources.getResourcePackageName(id) }.getOrNull()
    return packageName == "android"
}
