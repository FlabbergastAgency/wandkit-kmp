package com.flabbergast.wandkit.core.localization

import com.flabbergast.wandkit.core.InternalWandKitApi

/**
 * Every text WandKit shows, looked up when it is shown so a language set in
 * [com.flabbergast.wandkit.core.config.WandKitConfig.locale] or
 * [com.flabbergast.wandkit.core.WandKit.setLocale] applies to the next screen.
 * Same names as the iOS SDK's `WandKitText`. On Android each one is a string
 * resource in `values/wandkit_strings.xml`, mapped in `WandKitText.android.kt`
 * so a missing resource fails the build.
 */
@InternalWandKitApi
public enum class WandKitText {
    ACCESS_GATE_BUTTON,
    ACCESS_GATE_ERROR_ALREADY_USED,
    ACCESS_GATE_ERROR_EXPIRED,
    ACCESS_GATE_ERROR_GENERIC,
    ACCESS_GATE_ERROR_NOT_FOUND,
    ACCESS_GATE_ERROR_RATE_LIMITED,
    ACCESS_GATE_ERROR_REVOKED,
    ACCESS_GATE_HELP_LINK,
    ACCESS_GATE_MESSAGE,
    ACCESS_GATE_OFFLINE_MESSAGE,
    ACCESS_GATE_OFFLINE_TITLE,
    ACCESS_GATE_PLACEHOLDER,
    ACCESS_GATE_RETRY,
    ACCESS_GATE_TITLE,
    CLOSE,
    FEATURE_PREVIEW_CLOSE,
    FEATURE_PREVIEW_GENERIC_MESSAGE,
    FEATURE_PREVIEW_GENERIC_TITLE,
    FEATURE_PREVIEW_VOTE_ERROR,
    FEEDBACK_LOAD_FAILED_MESSAGE,
    FEEDBACK_LOAD_FAILED_RETRY,
    FEEDBACK_LOAD_FAILED_TITLE,
    POWERED_BY,
    SCREENSHOT_PROMPT_COMPOSE_PLACEHOLDER,
    SCREENSHOT_PROMPT_DEBUG_DISCLOSURE,
    SCREENSHOT_PROMPT_FAILED_GENERIC,
    SCREENSHOT_PROMPT_INCLUDE_REPLAY,
    SCREENSHOT_PROMPT_MESSAGE,
    SCREENSHOT_PROMPT_NOT_NOW,
    SCREENSHOT_PROMPT_REPORT,
    SCREENSHOT_PROMPT_RETRY,
    SCREENSHOT_PROMPT_SEND,
    SCREENSHOT_PROMPT_SENT_MESSAGE,
    SCREENSHOT_PROMPT_SENT_TITLE,
    SCREENSHOT_PROMPT_THUMBNAIL,
    SCREENSHOT_PROMPT_TITLE,
    SURVEY_CONTINUE,
    SURVEY_DISPLAY_NAME_PLACEHOLDER,
    ;

    public val string: String get() = platformString(this)
}

internal expect fun platformString(text: WandKitText): String
