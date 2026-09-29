package com.flabbergast.wandkit.core.domain.events

import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger

internal interface TrackEventUseCase {
    suspend operator fun invoke(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo,
    )
}

/**
 * @param isAccessGateBlocking While the invite gate is checking or blocked, a
 * form the backend returns is dropped rather than published - nothing may
 * show over (or behind) the gate.
 */
internal fun createTrackEventUseCase(
    eventsRepository: EventsRepository,
    feedbackFormController: FeedbackFormController,
    isAccessGateBlocking: () -> Boolean = { false },
    logger: Logger? = null,
): TrackEventUseCase = TrackEventUseCaseImpl(
    eventsRepository = eventsRepository,
    feedbackFormController = feedbackFormController,
    isAccessGateBlocking = isAccessGateBlocking,
    logger = logger,
)

private const val LOGGER_TAG = "[TrackEventUseCase]"

private class TrackEventUseCaseImpl(
    private val eventsRepository: EventsRepository,
    private val feedbackFormController: FeedbackFormController,
    private val isAccessGateBlocking: () -> Boolean,
    private val logger: Logger?,
): TrackEventUseCase {
    override suspend fun invoke(
        event: WandKitEvent,
        identifyInfo: IdentifyInfo
    ) {
        eventsRepository.trackEvent(event, identifyInfo)?.let { form ->
            if (isAccessGateBlocking()) {
                logger?.debug(LOGGER_TAG, "Dropped form ${form.formId} for event ${event.name}: the invite gate is up")
                return
            }
            feedbackFormController.publish(form)
        }
    }
}
