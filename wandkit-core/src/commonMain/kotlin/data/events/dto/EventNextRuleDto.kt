package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventNextRuleDto(
    @SerialName("page_id")
    val pageId: String,
    @SerialName("condition")
    val condition: String? = null,
)
