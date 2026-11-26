package com.flabbergast.wandkit.core.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.flabbergast.wandkit.core.R
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

internal actual fun platformString(key: String): String? {
    val context = AndroidLocalization.context ?: return null
    val id = stringIds[key] ?: return null
    return AndroidLocalization.resources(context, WandKitLanguage.current()).getString(id)
}

internal actual fun platformLanguage(): String = Locale.getDefault().toLanguageTag()

// Mapped rather than looked up by name: resource lookups by name depend on the
// host's package name and break when R8 shrinks resources.
private val stringIds: Map<String, Int> by lazy {
    mapOf(
        "wandkit.access_gate.button" to R.string.wandkit_access_gate_button,
        "wandkit.access_gate.error.already_used" to R.string.wandkit_access_gate_error_already_used,
        "wandkit.access_gate.error.expired" to R.string.wandkit_access_gate_error_expired,
        "wandkit.access_gate.error.generic" to R.string.wandkit_access_gate_error_generic,
        "wandkit.access_gate.error.not_found" to R.string.wandkit_access_gate_error_not_found,
        "wandkit.access_gate.error.rate_limited" to R.string.wandkit_access_gate_error_rate_limited,
        "wandkit.access_gate.error.revoked" to R.string.wandkit_access_gate_error_revoked,
        "wandkit.access_gate.help_link" to R.string.wandkit_access_gate_help_link,
        "wandkit.access_gate.message" to R.string.wandkit_access_gate_message,
        "wandkit.access_gate.offline.message" to R.string.wandkit_access_gate_offline_message,
        "wandkit.access_gate.offline.title" to R.string.wandkit_access_gate_offline_title,
        "wandkit.access_gate.placeholder" to R.string.wandkit_access_gate_placeholder,
        "wandkit.access_gate.retry" to R.string.wandkit_access_gate_retry,
        "wandkit.access_gate.title" to R.string.wandkit_access_gate_title,
        "wandkit.close" to R.string.wandkit_close,
        "wandkit.feature_preview.close" to R.string.wandkit_feature_preview_close,
        "wandkit.feature_preview.generic.message" to R.string.wandkit_feature_preview_generic_message,
        "wandkit.feature_preview.generic.title" to R.string.wandkit_feature_preview_generic_title,
        "wandkit.feature_preview.vote_error" to R.string.wandkit_feature_preview_vote_error,
        "wandkit.feedback.load_failed.message" to R.string.wandkit_feedback_load_failed_message,
        "wandkit.feedback.load_failed.retry" to R.string.wandkit_feedback_load_failed_retry,
        "wandkit.feedback.load_failed.title" to R.string.wandkit_feedback_load_failed_title,
        "wandkit.powered_by" to R.string.wandkit_powered_by,
        "wandkit.screenshot_prompt.compose_placeholder" to R.string.wandkit_screenshot_prompt_compose_placeholder,
        "wandkit.screenshot_prompt.debug_disclosure" to R.string.wandkit_screenshot_prompt_debug_disclosure,
        "wandkit.screenshot_prompt.failed_generic" to R.string.wandkit_screenshot_prompt_failed_generic,
        "wandkit.screenshot_prompt.include_replay" to R.string.wandkit_screenshot_prompt_include_replay,
        "wandkit.screenshot_prompt.message" to R.string.wandkit_screenshot_prompt_message,
        "wandkit.screenshot_prompt.not_now" to R.string.wandkit_screenshot_prompt_not_now,
        "wandkit.screenshot_prompt.report" to R.string.wandkit_screenshot_prompt_report,
        "wandkit.screenshot_prompt.retry" to R.string.wandkit_screenshot_prompt_retry,
        "wandkit.screenshot_prompt.send" to R.string.wandkit_screenshot_prompt_send,
        "wandkit.screenshot_prompt.sent_message" to R.string.wandkit_screenshot_prompt_sent_message,
        "wandkit.screenshot_prompt.sent_title" to R.string.wandkit_screenshot_prompt_sent_title,
        "wandkit.screenshot_prompt.thumbnail" to R.string.wandkit_screenshot_prompt_thumbnail,
        "wandkit.screenshot_prompt.title" to R.string.wandkit_screenshot_prompt_title,
        "wandkit.survey.continue" to R.string.wandkit_survey_continue,
        "wandkit.survey.display_name_placeholder" to R.string.wandkit_survey_display_name_placeholder,
    )
}
