package com.flabbergast.wandkit.core.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.flabbergast.wandkit.core.accessgate.createAccessGatePresenter
import com.flabbergast.wandkit.core.accessgate.isBlocking
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.data.accessgate.AccessGateApi
import com.flabbergast.wandkit.core.data.accessgate.AccessGateStore
import com.flabbergast.wandkit.core.data.accessgate.createAccessGateApi
import com.flabbergast.wandkit.core.data.accessgate.createAccessGateRepository
import com.flabbergast.wandkit.core.data.accessgate.createAccessGateStore
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateController
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateRepository
import com.flabbergast.wandkit.core.platform.InMemoryKeyValueStore
import com.flabbergast.wandkit.core.platform.KeyValueStore
import com.flabbergast.wandkit.core.platform.discardStateOfPreviousApiKey
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
import com.flabbergast.wandkit.core.data.posts.PostsApi
import com.flabbergast.wandkit.core.data.posts.createPostsApi
import com.flabbergast.wandkit.core.data.posts.createPostsSessionRepository
import com.flabbergast.wandkit.core.data.referrals.ReferralsApi
import com.flabbergast.wandkit.core.data.referrals.createReferralsApi
import com.flabbergast.wandkit.core.data.referrals.ReferralDetectionStore
import com.flabbergast.wandkit.core.data.referrals.createReferralDetectionStore
import com.flabbergast.wandkit.core.data.referrals.createReferralsRepository
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.platform.createInstallReferralCodeProvider
import com.flabbergast.wandkit.core.platform.createKeyValueStore
import com.flabbergast.wandkit.core.platform.createReentrantLock
import com.flabbergast.wandkit.core.platform.withLock
import com.flabbergast.wandkit.core.platform.readDeviceContext
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import com.flabbergast.wandkit.core.replay.SessionReplayRecorder
import kotlin.concurrent.Volatile
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.events.TrackEventUseCase
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.events.createTrackEventUseCase
import com.flabbergast.wandkit.core.domain.featurepreview.FEATURE_PREVIEW_FOLLOWED_QUERY
import com.flabbergast.wandkit.core.domain.featurepreview.FeaturePreviewController
import com.flabbergast.wandkit.core.domain.featurepreview.ResolveFeaturePreviewUseCase
import com.flabbergast.wandkit.core.domain.featurepreview.VoteFeaturePreviewPostUseCase
import com.flabbergast.wandkit.core.domain.featurepreview.createFeaturePreviewController
import com.flabbergast.wandkit.core.domain.featurepreview.createResolveFeaturePreviewUseCase
import com.flabbergast.wandkit.core.domain.featurepreview.createVoteFeaturePreviewPostUseCase
import com.flabbergast.wandkit.core.domain.forms.DismissFormUseCase
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.FeedbackFormRepository
import com.flabbergast.wandkit.core.domain.forms.NativeReviewHooks
import com.flabbergast.wandkit.core.domain.forms.SubmitFormUseCase
import com.flabbergast.wandkit.core.domain.forms.createDismissFormUseCase
import com.flabbergast.wandkit.core.domain.forms.createFeedbackFormController
import com.flabbergast.wandkit.core.domain.forms.createSubmitFormUseCase
import com.flabbergast.wandkit.core.domain.infrastructure.concurrency.createFireAndForgetTask
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.infrastructure.logger.LogLevel
import com.flabbergast.wandkit.core.domain.infrastructure.logger.createAppLogger
import com.flabbergast.wandkit.core.domain.install.InstallIdentity
import com.flabbergast.wandkit.core.domain.install.createInstallIdentity
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.domain.referrals.ReferralsRepository
import com.flabbergast.wandkit.core.domain.screenshot.ScreenshotPromptController
import com.flabbergast.wandkit.core.domain.screenshot.SubmitScreenshotReportUseCase
import com.flabbergast.wandkit.core.domain.screenshot.createScreenshotPromptController
import com.flabbergast.wandkit.core.domain.screenshot.createSubmitScreenshotReportUseCase
import com.flabbergast.wandkit.core.domain.infrastructure.threading.BackgroundDispatcher
import com.flabbergast.wandkit.core.feedback.WandKitFeedbackScreen
import com.flabbergast.wandkit.core.feedback.presentFeedbackScreen
import com.flabbergast.wandkit.core.models.createWandKitClient
import com.flabbergast.wandkit.core.review.NativeReviewPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class WandKitSdkContainer private constructor(
    internal val config: WandKitConfig,
    internal val platformContext: PlatformContext?,
    private val keyValueStoreOverride: KeyValueStore?,
): InstanceKeeper.Instance {
    internal val backgroundDispatcher = BackgroundDispatcher()
    internal val wandKitClient = createWandKitClient()

    internal val appConfiguration = createAppConfiguration(config.isDebugLoggingEnabled, config.apiBaseUrl)

    internal val logger: Logger by lazy { createAppLogger(appConfiguration.logLevel) }

    /**
     * Makes [identify] and [clearUser] one step for every reader of
     * [identityInfo]: a request never pairs the new user with the previous
     * device id, or the previous user with the new one - that pair would link
     * the id to the wrong user for good.
     */
    private val identityLock = createReentrantLock()

    /** The identified user's id, or `null` when nobody is identified - never blank. */
    @Volatile
    internal var externalUserId: String? = null
        private set

    @Volatile
    internal var displayName: String? = null
        private set

    /**
     * Sets the identified user id and, optionally, a display name suggestion,
     * and keeps the anonymous device id in step with it (see
     * [InstallIdentity.deviceId]). Ids are compared trimmed, as the backend
     * does, so `" user_1 "` is the same user as `"user_1"`.
     *
     * - A user other than the one the device id is linked to gets a fresh
     *   device id first, then the id is linked to them.
     * - The same user again changes nothing but the name: a `null`
     *   [displayName] leaves the previously set name untouched rather than
     *   clearing it. A different user never inherits the previous one's name.
     * - A blank [userId] means "not identified (yet)", not a sign-out: the
     *   next requests are anonymous, but the link and the device id are left
     *   alone - an app passing `user?.id ?: ""` on every launch before its
     *   user has loaded must not get a new device id per launch. Only
     *   [clearUser] ends a link without a new user.
     */
    internal fun identify(userId: String, displayName: String? = null) {
        val linkedAs = userId.trim()
        identityLock.withLock {
            if (linkedAs.isEmpty()) {
                externalUserId = null
                this.displayName = null
                return
            }
            installIdentity.linkDeviceIdTo(linkedAs)
            if (linkedAs != externalUserId?.trim()) {
                this.displayName = displayName
            } else if (displayName != null) {
                this.displayName = displayName
            }
            externalUserId = userId
        }
    }

    /**
     * Sign-out: clears the user and the name, and replaces the device id with
     * a fresh one if it was linked to a user. Nothing linked (an app clearing
     * on every signed-out launch), nothing changes.
     */
    internal fun clearUser() {
        identityLock.withLock {
            installIdentity.unlinkDeviceId()
            externalUserId = null
            displayName = null
        }
    }

    /**
     * The anonymous id the next request carries as `device_id`. It changes on
     * logout and account switch, so read it per request. See
     * [InstallIdentity.deviceId].
     */
    internal val deviceId: String
        get() = installIdentity.deviceId

    /**
     * The platform's session replay recorder while it is running - set by the
     * Android `configure` when [WandKitConfig.sessionReplay] and screenshot
     * reporting are both on; always `null` on iOS.
     */
    @Volatile
    internal var sessionReplayRecorder: SessionReplayRecorder? = null

    /** The `device` object for a replay header - the same one the posts session is minted with. */
    internal fun replayDevice(): SdkPostsSessionDeviceDto {
        val deviceContext = readDeviceContext(platformContext)
        return SdkPostsSessionDeviceDto(
            platform = appConfiguration.platform,
            osVersion = deviceContext.osVersion,
            appVersion = deviceContext.appVersion,
            deviceModel = deviceContext.deviceModel,
            locale = deviceContext.locale,
            appIdentifier = deviceContext.appIdentifier,
        )
    }

    /** The replay header's `sdk`, e.g. `android-0.1.7` - the counterpart of iOS's `ios-<semver>`. */
    internal val replaySdkName: String
        get() = "${appConfiguration.platform}-${appConfiguration.libraryVersion}"

    internal val keyValueStore: KeyValueStore by lazy { keyValueStoreOverride ?: createKeyValueStore(platformContext) }

    internal val installIdentity: InstallIdentity by lazy { createInstallIdentity(keyValueStore) }

    internal val referralDetectionStore: ReferralDetectionStore by lazy {
        createReferralDetectionStore(keyValueStore = keyValueStore, json = json)
    }

    /**
     * Who the next request is from: the user and the device id as one
     * consistent snapshot. Read it once per request and use that copy for
     * both ids.
     */
    internal val identityInfo: IdentifyInfo
        get() = identityLock.withLock {
            // No made-up id for an anonymous user: the backend keys it by
            // `device_id` alone, and would otherwise take a random id at face
            // value as a new identified user (and later stamp an invite-code
            // claim onto it).
            val userId = externalUserId
            IdentifyInfo(
                userId = userId,
                deviceId = deviceId,
                displayName = userId?.let { displayName?.trim()?.takeIf { name -> name.isNotBlank() } },
            )
        }

    internal val json: Json by lazy { createJson() }

    internal val httpClient: WandKitHttpClient by lazy { createHttpClient(
        json = json,
        commonInterceptor = createCommonInterceptor(config.apiKey, appConfiguration.baseUrl),
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

    internal val postsApi: WandKitApi<PostsApi> by lazy {
        createPostsApi(
            httpClient = httpClient,
            baseUrl = appConfiguration.baseUrl,
            logger = logger,
        )
    }

    internal val referralsApi: WandKitApi<ReferralsApi> by lazy {
        createReferralsApi(
            httpClient = httpClient,
            baseUrl = appConfiguration.baseUrl,
            logger = logger,
        )
    }

    internal val accessGateApi: WandKitApi<AccessGateApi> by lazy {
        createAccessGateApi(
            httpClient = httpClient,
            baseUrl = appConfiguration.baseUrl,
            logger = logger,
        )
    }

    internal val accessGateRepository: AccessGateRepository by lazy {
        createAccessGateRepository(
            accessGateApi = accessGateApi,
            installIdentity = installIdentity,
            identity = { identityInfo },
            platform = appConfiguration.platform,
            sdkVersion = replaySdkName,
        )
    }

    internal val accessGateStore: AccessGateStore by lazy {
        createAccessGateStore(keyValueStore = keyValueStore, json = json)
    }

    /**
     * Errors only, whatever [WandKitConfig.isDebugLoggingEnabled] says: a gate
     * that is off (or cannot let anyone in) because of the app's setup must
     * show up in a release build's log too.
     */
    private val accessGateMisconfigurationLogger: Logger by lazy { createAppLogger(LogLevel.ERROR) }

    internal val accessGateController: AccessGateController by lazy {
        AccessGateController(
            repository = accessGateRepository,
            store = accessGateStore,
            presenterFactory = { createAccessGatePresenter(platformContext, accessGateMisconfigurationLogger) },
            isStorePersistent = keyValueStore !is InMemoryKeyValueStore,
            logger = logger,
            misconfigurationLogger = accessGateMisconfigurationLogger,
            dispatcher = Dispatchers.Main.immediate,
        )
    }

    /**
     * Starts the invite gate when [WandKitConfig.accessGate] opted in. Without
     * it the controller is never even created: no gate network call, ever.
     */
    private fun startAccessGate() {
        val options = config.accessGate ?: return
        // Published before start() so the platform presenter, which reads the
        // live state through [activeAccessGate], sees the first transition.
        activeAccessGate.value = accessGateController
        accessGateController.start(options)
    }

    internal val eventsRepository: EventsRepository by lazy {
        createEventsRepository(
            eventsApi = eventsApi,
            appConfiguration = appConfiguration,
            logger = logger,
            platformContext = platformContext,
        )
    }

    internal val feedbackFormRepository: FeedbackFormRepository by lazy {
        createFeedbackFormRepository(
            formsApi = formsApi,
            logger = logger,
            appConfiguration = appConfiguration,
            platformContext = platformContext,
        )
    }

    internal val referralsRepository: ReferralsRepository by lazy {
        createReferralsRepository(
            referralsApi = referralsApi,
            installReferralCodeProvider = installReferralCodeProvider,
            installIdentity = installIdentity,
            detectionStore = referralDetectionStore,
            appConfiguration = appConfiguration,
            json = json,
            logger = logger,
        )
    }

    internal val postsSessionRepository: PostsSessionRepository by lazy {
        createPostsSessionRepository(
            postsApi = postsApi,
            appConfiguration = appConfiguration,
            platformContext = platformContext,
            externalUserId = { externalUserId },
            displayName = { displayName },
            logger = logger,
        )
    }

    internal val feedbackFormController: FeedbackFormController by lazy {
        createFeedbackFormController(
            logger = logger,
            nativeReviewHooks = object : NativeReviewHooks {
                override fun prefetch() = NativeReviewPresenter.prefetch(platformContext, logger)
                override fun request() = NativeReviewPresenter.request(logger)
                override fun discard() = NativeReviewPresenter.discard()
            },
        )
    }

    internal val submitScreenshotReportUseCase: SubmitScreenshotReportUseCase by lazy {
        createSubmitScreenshotReportUseCase(
            postsApi = postsApi,
            postsSessionRepository = postsSessionRepository,
            debugAttachmentsProvider = { config.debugAttachmentsProvider },
            logger = logger,
        )
    }

    internal val screenshotPromptController: ScreenshotPromptController by lazy {
        createScreenshotPromptController(
            submitReport = submitScreenshotReportUseCase,
            fireAndForgetTask = fireAndForgetTask,
            logger = logger,
        )
    }

    internal val resolveFeaturePreviewUseCase: ResolveFeaturePreviewUseCase by lazy {
        createResolveFeaturePreviewUseCase(
            postsApi = postsApi,
            postsSessionRepository = postsSessionRepository,
            logger = logger,
        )
    }

    internal val voteFeaturePreviewPostUseCase: VoteFeaturePreviewPostUseCase by lazy {
        createVoteFeaturePreviewPostUseCase(
            postsApi = postsApi,
            postsSessionRepository = postsSessionRepository,
            logger = logger,
        )
    }

    internal val featurePreviewController: FeaturePreviewController by lazy {
        createFeaturePreviewController(
            voteUseCase = voteFeaturePreviewPostUseCase,
            recordEvent = { name, properties ->
                // Deliberately bypasses trackEventUseCase: a form the backend
                // returns for this event must never auto-present over the
                // feature-preview sheet, so the response's `form` is dropped
                // here rather than published to feedbackFormController.
                eventsRepository.trackEvent(
                    WandKitEvent(name = name, properties = properties, occurredAt = Clock.System.now()),
                    identityInfo,
                )
                Unit
            },
            openFeedbackPost = { postId ->
                presentFeedbackScreen(this@WandKitSdkContainer, WandKitFeedbackScreen.Post(postId), FEATURE_PREVIEW_FOLLOWED_QUERY)
            },
            fireAndForgetTask = fireAndForgetTask,
            logger = logger,
        )
    }

    internal val trackEventUseCase: TrackEventUseCase
        get() = createTrackEventUseCase(
            eventsRepository = eventsRepository,
            feedbackFormController = feedbackFormController,
            isAccessGateBlocking = { isAccessGateBlocking },
            logger = logger,
        )

    internal val dismissFormUseCase: DismissFormUseCase
        get() = createDismissFormUseCase(feedbackFormRepository, feedbackFormController)

    internal val submitFormUseCase: SubmitFormUseCase
        get() = createSubmitFormUseCase(feedbackFormRepository, feedbackFormController)

    internal companion object {
        private var instance: WandKitSdkContainer? = null

        /**
         * The invite gate of the latest `configure` that opted in; `null`
         * when it did not. Process-wide, so the gate screen and the lifecycle
         * glue follow a re-configure instead of holding on to a stale gate.
         */
        val activeAccessGate = MutableStateFlow<AccessGateController?>(null)

        /** While true, no other WandKit UI may be presented. */
        val isAccessGateBlocking: Boolean
            get() = activeAccessGate.value?.isBlocking == true

        @OptIn(ExperimentalCoroutinesApi::class)
        val accessGateBlocking: Flow<Boolean> = activeAccessGate
            .flatMapLatest { controller -> controller?.state?.map { it.isBlocking } ?: flowOf(false) }
            .distinctUntilChanged()

        fun get(): WandKitSdkContainer = instance ?: error("WandKit SDK isn't initialized.")

        /** The latest configured container, or `null` before `configure`. */
        val currentOrNull: WandKitSdkContainer?
            get() = instance
        /** [keyValueStore] is for tests: one store over several configures stands in for relaunches. */
        fun init(
            config: WandKitConfig,
            platformContext: PlatformContext? = null,
            keyValueStore: KeyValueStore? = null,
        ) {
            // A second configure must not stack a second gate: the previous
            // one stops for good before the new one starts.
            activeAccessGate.value?.shutdown()
            activeAccessGate.value = null

            val container = WandKitSdkContainer(config, platformContext, keyValueStore)
            // Before anything can read the gate pass or referral state.
            discardStateOfPreviousApiKey(container.keyValueStore, config.apiKey)
            instance = container
            container.startAccessGate()
        }
    }
}
