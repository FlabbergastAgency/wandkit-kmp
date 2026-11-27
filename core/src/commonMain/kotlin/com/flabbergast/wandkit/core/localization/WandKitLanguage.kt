package com.flabbergast.wandkit.core.localization

import kotlin.concurrent.Volatile

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

internal expect fun platformLanguage(): String
