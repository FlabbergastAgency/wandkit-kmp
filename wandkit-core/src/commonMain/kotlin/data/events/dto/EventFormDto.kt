package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventFormDto(
    @SerialName("public_id")
    val publicId: String,
    @SerialName("impression_id")
    val impressionId: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("pages")
    val pages: List<EventPageDto>,
)
