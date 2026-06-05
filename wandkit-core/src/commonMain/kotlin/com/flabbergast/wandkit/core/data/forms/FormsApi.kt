package com.flabbergast.wandkit.core.data.forms

import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormResponseRequestDto

internal interface FormsApi {
    suspend fun submitFormResponse(
        impressionId: String,
        request: SubmitFormResponseRequestDto,
    )
    suspend fun dismissForm(impressionId: String)
}