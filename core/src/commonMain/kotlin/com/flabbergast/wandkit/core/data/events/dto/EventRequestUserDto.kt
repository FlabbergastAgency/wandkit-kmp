package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventRequestUserDto(
    /**
     * Omitted (not `null`) while nobody is identified: the backend then keys
     * the user by [deviceId] alone - or, once that id is linked, by the user
     * it was linked to.
     */
    @SerialName("external_user_id")
    val externalUserId: String? = null,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("display_name")
    val displayName: String? = null,
)
