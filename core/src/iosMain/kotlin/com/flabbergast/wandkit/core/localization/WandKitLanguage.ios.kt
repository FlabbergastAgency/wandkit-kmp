package com.flabbergast.wandkit.core.localization

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// The KMP SDK shows no UI on iOS; the native WandKit iOS SDK owns those strings.
internal actual fun platformString(text: WandKitText): String = text.name

internal actual fun platformLanguage(): String = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
