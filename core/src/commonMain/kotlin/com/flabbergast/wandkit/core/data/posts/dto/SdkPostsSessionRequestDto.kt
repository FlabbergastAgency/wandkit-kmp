package com.flabbergast.wandkit.core.data.posts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** One posts-session mint. Field names are the API's (snake_case). */
@Serializable
internal data class SdkPostsSessionRequestDto(
    /**
     * Omitted when the app has not identified anyone, which is what makes the
     * session anonymous - and anonymous sessions come back read-only.
     */
    @SerialName("external_user_id")
    val externalUserId: String? = null,
    /** The host app's suggested name for this user, if any. */
    @SerialName("display_name")
    val displayName: String? = null,
    /** Whatever the host app wants the feedback UI to know about this user. */
    @SerialName("attributes")
    val attributes: JsonObject? = null,
    @SerialName("device")
    val device: SdkPostsSessionDeviceDto? = null,
)

/**
 * Device context the backend stamps onto anything the user submits during the
 * session, so a bug report carries the OS and app version without the user
 * having to type them.
 */
@Serializable
internal data class SdkPostsSessionDeviceDto(
    @SerialName("platform")
    val platform: String,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("locale")
    val locale: String? = null,
)
