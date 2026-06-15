package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventOptionDto(
    @SerialName("id")
    val id: String,
    @SerialName("label")
    val label: String,
)
