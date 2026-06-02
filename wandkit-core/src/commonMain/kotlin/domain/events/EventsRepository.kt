package com.flabbergast.wandkit.core.domain.events

import com.flabbergast.wandkit.core.domain.forms.models.FeedbackForm

internal interface EventsRepository {

    suspend fun trackEvent(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo,
    ): FeedbackForm?
}