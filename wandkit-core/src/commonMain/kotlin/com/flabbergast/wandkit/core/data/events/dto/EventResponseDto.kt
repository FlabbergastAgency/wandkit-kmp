package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventResponseDto(
    @SerialName("event_id")
    val eventId: String,
    @SerialName("form")
    val form: EventFormDto?,
)
