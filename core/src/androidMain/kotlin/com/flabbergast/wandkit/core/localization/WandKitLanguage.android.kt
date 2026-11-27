package com.flabbergast.wandkit.core.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import kotlin.concurrent.Volatile

internal object AndroidLocalization {
    @Volatile
    var context: Context? = null

    @Volatile
    private var cached: Pair<String, Resources>? = null

    /** Resources in the app's language, read through a context built for it rather than the application's, which can lag behind a per-app language. */
    fun resources(context: Context, languageTag: String): Resources {
        cached?.takeIf { it.first == languageTag }?.let { return it.second }
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        val resources = context.createConfigurationContext(configuration).resources
        cached = languageTag to resources
        return resources
    }
}

// Before WandKit is initialised there is no context to read resources from.
internal actual fun platformString(text: WandKitText): String {
    val context = AndroidLocalization.context ?: return text.name
    return AndroidLocalization.resources(context, WandKitLanguage.current()).getString(text.resId)
}

internal actual fun platformLanguage(): String = Locale.getDefault().toLanguageTag()
