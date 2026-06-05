package com.flabbergast.wandkit.core.domain.events

import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController

internal interface TrackEventUseCase {
    suspend operator fun invoke(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo,
    )
}

internal fun createTrackEventUseCase(
    eventsRepository: EventsRepository,
    feedbackFormController: FeedbackFormController,
): TrackEventUseCase = TrackEventUseCaseImpl(
    eventsRepository = eventsRepository,
    feedbackFormController = feedbackFormController,
)

private class TrackEventUseCaseImpl(
    private val eventsRepository: EventsRepository,
    private val feedbackFormController: FeedbackFormController,
): TrackEventUseCase {
    override suspend fun invoke(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo
    ) {
        eventsRepository.trackEvent(event, identifyInfo)?.let { form ->
            feedbackFormController.publish(form)
        }
    }
}
