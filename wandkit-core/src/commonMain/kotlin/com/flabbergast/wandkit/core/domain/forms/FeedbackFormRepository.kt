package com.flabbergast.wandkit.core.domain.forms

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.forms.models.PageInput

internal interface FeedbackFormRepository {
    suspend fun submit(impressionId: String, results: Map<FeedbackFormPageId, PageInput>)
    suspend fun dismiss(impressionId: String)
}