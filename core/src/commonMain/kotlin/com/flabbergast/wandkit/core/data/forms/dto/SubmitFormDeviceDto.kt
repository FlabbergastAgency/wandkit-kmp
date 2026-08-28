package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Coarse device/app context sent alongside a form response so the dashboard can
 * show what a submission came from. Every field is optional - the backend
 * already falls back to the triggering event's platform when this is absent.
 */
@Serializable
internal data class SubmitFormDeviceDto(
    @SerialName("platform")
    val platform: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("locale")
    val locale: String? = null,
)
