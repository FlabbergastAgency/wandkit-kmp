package com.flabbergast.wandkit.core.domain.forms

internal interface FeedbackFormRepository {
    suspend fun submit(formId: String)
    fun dismiss(formId: String)
}