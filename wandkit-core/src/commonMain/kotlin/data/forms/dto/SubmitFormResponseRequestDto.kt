package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitFormResponseRequestDto(
    @SerialName("answers")
    val answers: List<SubmitFormAnswerDto>,
    @SerialName("completed_at")
    val completedAt: String,
)
