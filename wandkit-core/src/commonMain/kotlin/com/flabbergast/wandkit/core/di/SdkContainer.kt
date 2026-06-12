package com.flabbergast.wandkit.core.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.config.createAppConfiguration
import com.flabbergast.wandkit.core.data.events.EventsApi
import com.flabbergast.wandkit.core.data.events.createEventsApi
import com.flabbergast.wandkit.core.data.events.createEventsRepository
import com.flabbergast.wandkit.core.data.forms.FormsApi
import com.flabbergast.wandkit.core.data.forms.createFeedbackFormRepository
import com.flabbergast.wandkit.core.data.forms.createFormsApi
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.createCommonInterceptor
import com.flabbergast.wandkit.core.data.networking.createHttpClient
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.platform.createInstallReferralCodeProvider
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.events.TrackEventUseCase
import com.flabbergast.wandkit.core.domain.events.createTrackEventUseCase
import com.flabbergast.wandkit.core.domain.forms.DismissFormUseCase
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormRepository
import com.flabbergast.wandkit.core.domain.forms.SubmitFormUseCase
import com.flabbergast.wandkit.core.domain.forms.createDismissFormUseCase
import com.flabbergast.wandkit.core.domain.forms.createFeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.createSubmitFormUseCase
import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.createFireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.infrastructure.logger.createAppLogger
import com.flabbergast.wandkit.core.domain.infrastructure.threading.BackgroundDispatcher
import com.flabbergast.wandkit.core.models.createWandKitClient
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class WandKitSdkContainer private constructor(
    private val config: WandKitConfig,
    platformContext: PlatformContext?,
): InstanceKeeper.Instance {
    internal val backgroundDispatcher = BackgroundDispatcher()
    internal val wandKitClient = createWandKitClient()

    internal val appConfiguration = createAppConfiguration(config.isDebugLoggingEnabled)

    internal val logger: Logger by lazy { createAppLogger(appConfiguration.logLevel) }

    internal var externalUserId: String? = null
        private set

    internal fun setUserId(userId: String?) { externalUserId = userId }

    internal val deviceId = Uuid.generateV4().toString()

    internal val identityInfo: IdentifyInfo
        get() = IdentifyInfo(externalUserId ?: Uuid.generateV4().toString(), deviceId)

    internal val json: Json by lazy { createJson() }

    internal val httpClient: WandKitHttpClient by lazy { createHttpClient(
        json = json,
        commonInterceptor = createCommonInterceptor(config.apiKey),
        appLogger = logger,
    ) }

    internal val fireAndForgetTask by lazy { createFireAndForgetTask(dispatcher = backgroundDispatcher, logger = logger) }

    internal val installReferralCodeProvider by lazy { createInstallReferralCodeProvider(platformContext) }

    internal val eventsApi: WandKitApi<EventsApi> by lazy {
        createEventsApi(
            httpClient = httpClient,
            baseUrl = appConfiguration.baseUrl,
            logger = logger,
        )
    }

    internal val formsApi: WandKitApi<FormsApi> by lazy {
        createFormsApi(
            httpClient = httpClient,
            baseUrl = appConfiguration.baseUrl,
            logger = logger,
        )
    }

    internal val eventsRepository: EventsRepository by lazy {
        createEventsRepository(
            eventsApi = eventsApi,
            appConfiguration = appConfiguration,
            logger = logger,
        )
    }

    internal val feedbackFormRepository: FeedbackFormRepository by lazy {
        createFeedbackFormRepository(
            formsApi = formsApi,
            logger = logger,
        )
    }

    internal val feedbackFormController: FeedbackFormController by lazy {
        createFeedbackFormController(logger)
    }

    internal val trackEventUseCase: TrackEventUseCase
        get() = createTrackEventUseCase(eventsRepository, feedbackFormController)

    internal val dismissFormUseCase: DismissFormUseCase
        get() = createDismissFormUseCase(feedbackFormRepository, feedbackFormController)

    internal val submitFormUseCase: SubmitFormUseCase
        get() = createSubmitFormUseCase(feedbackFormRepository, feedbackFormController)

    internal companion object {
        private var instance: WandKitSdkContainer? = null

        fun get(): WandKitSdkContainer = instance ?: error("WandKit SDK isn't initialized.")
        fun init(config: WandKitConfig, platformContext: PlatformContext? = null) {
            instance = WandKitSdkContainer(config, platformContext)
        }
    }
}
