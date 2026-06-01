package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitFormAnswerDto(
    @SerialName("page_id")
    val pageId: String,
    @SerialName("thumb")
    val thumb: SubmitFormThumbDto? = null,
    @SerialName("stars")
    val stars: Int? = null,
    @SerialName("selected_option_ids")
    val selectedOptionIds: List<String>? = null,
    @SerialName("text")
    val text: String? = null,
)
