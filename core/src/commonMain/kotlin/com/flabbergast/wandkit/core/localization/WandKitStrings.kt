package com.flabbergast.wandkit.core.localization

import com.flabbergast.wandkit.core.InternalWandKitApi
import kotlin.concurrent.Volatile

/**
 * Every SDK-owned string, looked up when it is shown so a language set in
 * [com.flabbergast.wandkit.core.config.WandKitConfig.locale] or
 * [com.flabbergast.wandkit.core.WandKit.setLocale] applies to the next screen.
 * Keys match the iOS SDK's. On Android each key is a string resource with the
 * dots turned into underscores, so a host app rewords one by declaring the
 * same name in its own `strings.xml`.
 */
@InternalWandKitApi
public object WandKitStrings {
    public fun get(key: String, default: String): String = wandKitString(key, default)
}

internal fun wandKitString(key: String, default: String): String = platformString(key) ?: default

internal object WandKitLanguage {
    /** Normalised to a BCP 47 tag: hosts often pass `Locale.toString()` (`hr_HR`). */
    @Volatile
    var override: String? = null
        set(value) {
            field = value?.replace('_', '-')
        }

    /** The app's language as a BCP 47 tag, for the feedback web app and `Accept-Language`. */
    fun current(): String = override ?: platformLanguage()
}

internal expect fun platformString(key: String): String?

internal expect fun platformLanguage(): String
