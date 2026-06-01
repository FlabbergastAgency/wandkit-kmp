package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventRequestSdkDto(
    @SerialName("platform")
    val platform: String,
    @SerialName("version")
    val version: String,
)
