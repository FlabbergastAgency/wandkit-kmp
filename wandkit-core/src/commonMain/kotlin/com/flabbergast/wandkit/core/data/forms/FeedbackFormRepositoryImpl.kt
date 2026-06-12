package com.flabbergast.wandkit.core.data.forms

import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormAnswerDto
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormResponseRequestDto
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormThumbDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormRepository
import com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPageId
import com.flabbergast.wandkit.core.domain.forms.models.PageInput
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import kotlin.time.Clock

internal fun createFeedbackFormRepository(
    formsApi: WandKitApi<FormsApi>,
    logger: Logger,
): FeedbackFormRepository = FeedbackFormRepositoryImpl(
    formsApi = formsApi,
    logger = logger
)

private const val LOGGER_TAG = "[FeedbackFormRepository]"

private class FeedbackFormRepositoryImpl(
    private val formsApi: WandKitApi<FormsApi>,
    private val logger: Logger,
): FeedbackFormRepository {
    override suspend fun submit(
        impressionId: String,
        results: Map<FeedbackFormPageId, PageInput>
    ) {
        formsApi {
            submitFormResponse(
                impressionId = impressionId,
                request = SubmitFormResponseRequestDto(
                    answers = results.map { (pageId, pageInput) -> SubmitFormAnswerDto(
                        pageId = pageId,
                        thumb = pageInput.isThumbsUp?.let(SubmitFormThumbDto::fromBoolean),
                        stars = pageInput.stars,
                        selectedOptionIds = pageInput.optionIds,
                        text = pageInput.text,
                    ) },
                    completedAt = Clock.System.now().toString(),
                )
            )
        }.onSuccess {
            logger.debug(LOGGER_TAG, "Submitted form with impressionId: $impressionId, results: $results")
        }.onFailure {
            logger.debug(LOGGER_TAG, "Couldn't submit form with impressionId: $impressionId, $it")
        }
    }

    override suspend fun dismiss(impressionId: String) {
        formsApi {
            dismissForm(impressionId)
        }.onSuccess {
            logger.debug(LOGGER_TAG, "Dismissed form with impressionId: $impressionId")
        }.onFailure {
            logger.debug(LOGGER_TAG, "Couldn't dismiss form with impressionId: $impressionId, $it")
        }
    }
}