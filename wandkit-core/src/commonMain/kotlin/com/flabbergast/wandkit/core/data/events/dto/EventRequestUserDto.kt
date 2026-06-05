package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventRequestUserDto(
    @SerialName("external_user_id")
    val externalUserId: String,
    @SerialName("device_id")
    val deviceId: String,
)
