package com.flabbergast.wandkit.core.data.forms.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SubmitFormResponseRequestDto(
    @SerialName("answers")
    val answers: List<SubmitFormAnswerDto>,
    @SerialName("completed_at")
    val completedAt: String,
    @SerialName("device")
    val device: SubmitFormDeviceDto? = null,
    /**
     * The user-confirmed name from the form's server-spliced `display_name`
     * page. Never sent as an `answers[]` entry - the server rejects any
     * answer keyed to that page's id. Trimmed and omitted (rather than sent
     * blank) when the page was skipped or left empty.
     */
    @SerialName("display_name")
    val displayName: String? = null,
)
