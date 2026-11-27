package com.flabbergast.wandkit.core.localization

import com.flabbergast.wandkit.core.R

// A `when` rather than a lookup by name: names depend on the host's package
// and break when R8 shrinks resources, and this way a missing entry or
// resource fails the build.
internal val WandKitText.resId: Int
    get() = when (this) {
        WandKitText.ACCESS_GATE_BUTTON -> R.string.wandkit_access_gate_button
        WandKitText.ACCESS_GATE_ERROR_ALREADY_USED -> R.string.wandkit_access_gate_error_already_used
        WandKitText.ACCESS_GATE_ERROR_EXPIRED -> R.string.wandkit_access_gate_error_expired
        WandKitText.ACCESS_GATE_ERROR_GENERIC -> R.string.wandkit_access_gate_error_generic
        WandKitText.ACCESS_GATE_ERROR_NOT_FOUND -> R.string.wandkit_access_gate_error_not_found
        WandKitText.ACCESS_GATE_ERROR_RATE_LIMITED -> R.string.wandkit_access_gate_error_rate_limited
        WandKitText.ACCESS_GATE_ERROR_REVOKED -> R.string.wandkit_access_gate_error_revoked
        WandKitText.ACCESS_GATE_HELP_LINK -> R.string.wandkit_access_gate_help_link
        WandKitText.ACCESS_GATE_MESSAGE -> R.string.wandkit_access_gate_message
        WandKitText.ACCESS_GATE_OFFLINE_MESSAGE -> R.string.wandkit_access_gate_offline_message
        WandKitText.ACCESS_GATE_OFFLINE_TITLE -> R.string.wandkit_access_gate_offline_title
        WandKitText.ACCESS_GATE_PLACEHOLDER -> R.string.wandkit_access_gate_placeholder
        WandKitText.ACCESS_GATE_RETRY -> R.string.wandkit_access_gate_retry
        WandKitText.ACCESS_GATE_TITLE -> R.string.wandkit_access_gate_title
        WandKitText.CLOSE -> R.string.wandkit_close
        WandKitText.FEATURE_PREVIEW_CLOSE -> R.string.wandkit_feature_preview_close
        WandKitText.FEATURE_PREVIEW_GENERIC_MESSAGE -> R.string.wandkit_feature_preview_generic_message
        WandKitText.FEATURE_PREVIEW_GENERIC_TITLE -> R.string.wandkit_feature_preview_generic_title
        WandKitText.FEATURE_PREVIEW_VOTE_ERROR -> R.string.wandkit_feature_preview_vote_error
        WandKitText.FEEDBACK_LOAD_FAILED_MESSAGE -> R.string.wandkit_feedback_load_failed_message
        WandKitText.FEEDBACK_LOAD_FAILED_RETRY -> R.string.wandkit_feedback_load_failed_retry
        WandKitText.FEEDBACK_LOAD_FAILED_TITLE -> R.string.wandkit_feedback_load_failed_title
        WandKitText.POWERED_BY -> R.string.wandkit_powered_by
        WandKitText.SCREENSHOT_PROMPT_COMPOSE_PLACEHOLDER -> R.string.wandkit_screenshot_prompt_compose_placeholder
        WandKitText.SCREENSHOT_PROMPT_DEBUG_DISCLOSURE -> R.string.wandkit_screenshot_prompt_debug_disclosure
        WandKitText.SCREENSHOT_PROMPT_FAILED_GENERIC -> R.string.wandkit_screenshot_prompt_failed_generic
        WandKitText.SCREENSHOT_PROMPT_INCLUDE_REPLAY -> R.string.wandkit_screenshot_prompt_include_replay
        WandKitText.SCREENSHOT_PROMPT_MESSAGE -> R.string.wandkit_screenshot_prompt_message
        WandKitText.SCREENSHOT_PROMPT_NOT_NOW -> R.string.wandkit_screenshot_prompt_not_now
        WandKitText.SCREENSHOT_PROMPT_REPORT -> R.string.wandkit_screenshot_prompt_report
        WandKitText.SCREENSHOT_PROMPT_RETRY -> R.string.wandkit_screenshot_prompt_retry
        WandKitText.SCREENSHOT_PROMPT_SEND -> R.string.wandkit_screenshot_prompt_send
        WandKitText.SCREENSHOT_PROMPT_SENT_MESSAGE -> R.string.wandkit_screenshot_prompt_sent_message
        WandKitText.SCREENSHOT_PROMPT_SENT_TITLE -> R.string.wandkit_screenshot_prompt_sent_title
        WandKitText.SCREENSHOT_PROMPT_THUMBNAIL -> R.string.wandkit_screenshot_prompt_thumbnail
        WandKitText.SCREENSHOT_PROMPT_TITLE -> R.string.wandkit_screenshot_prompt_title
        WandKitText.SURVEY_CONTINUE -> R.string.wandkit_survey_continue
        WandKitText.SURVEY_DISPLAY_NAME_PLACEHOLDER -> R.string.wandkit_survey_display_name_placeholder
    }
