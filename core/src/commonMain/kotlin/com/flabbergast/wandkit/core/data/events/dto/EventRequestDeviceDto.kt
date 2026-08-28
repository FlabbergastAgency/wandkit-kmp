package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Coarse device/app context sent alongside every event so the backend can
 * target feedback forms by app version. Every field is optional - mirrors
 * [com.flabbergast.wandkit.core.data.forms.dto.SubmitFormDeviceDto].
 */
@Serializable
internal data class EventRequestDeviceDto(
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
