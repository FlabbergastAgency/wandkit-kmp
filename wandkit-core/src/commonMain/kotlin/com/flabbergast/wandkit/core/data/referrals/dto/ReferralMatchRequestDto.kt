package com.flabbergast.wandkit.core.data.referrals.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ReferralMatchRequestDto(
    @SerialName("install_id")
    val installId: String,
    @SerialName("user_agent")
    val userAgent: String,
    @SerialName("language")
    val language: String,
    @SerialName("languages")
    val languages: List<String>,
    @SerialName("timezone")
    val timezone: String,
    @SerialName("timezone_offset_minutes")
    val timezoneOffsetMinutes: Int,
    @SerialName("platform")
    val platform: String,
    @SerialName("screen_width")
    val screenWidth: Int,
    @SerialName("screen_height")
    val screenHeight: Int,
    @SerialName("viewport_width")
    val viewportWidth: Int,
    @SerialName("viewport_height")
    val viewportHeight: Int,
    @SerialName("device_pixel_ratio")
    val devicePixelRatio: Double,
    @SerialName("client_timestamp")
    val clientTimestamp: String,
    @SerialName("extra")
    val extra: Map<String, JsonElement>? = null,
)
