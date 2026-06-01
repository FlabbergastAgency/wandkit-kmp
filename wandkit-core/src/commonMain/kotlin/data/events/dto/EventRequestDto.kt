package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventRequestDto(
    @SerialName("event_name")
    val eventName: String,
    @SerialName("user")
    val user: EventRequestUserDto,
    @SerialName("properties")
    val properties: Map<String, String>? = null,
    @SerialName("occurred_at")
    val occurredAt: String,
    @SerialName("sdk")
    val sdk: EventRequestSdkDto,
)
