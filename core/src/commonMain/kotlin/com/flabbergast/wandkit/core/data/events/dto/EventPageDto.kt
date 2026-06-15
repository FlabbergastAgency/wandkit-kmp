package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventPageDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: EventPageTypeDto,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("next_button_label")
    val nextButtonLabel: String? = null,
    @SerialName("required")
    val required: Boolean = false,
    @SerialName("options")
    val options: List<EventOptionDto>? = null,
    @SerialName("allow_multiple")
    val allowMultiple: Boolean? = null,
    @SerialName("max_length")
    val maxLength: Int? = null,
    @SerialName("placeholder")
    val placeholder: String? = null,
    @SerialName("next")
    val next: List<EventNextRuleDto> = emptyList(),
)
