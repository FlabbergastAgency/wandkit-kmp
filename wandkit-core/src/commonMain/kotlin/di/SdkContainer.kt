package com.flabbergast.wandkit.core.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.config.createAppConfiguration
import com.flabbergast.wandkit.core.data.events.EventsApi
import com.flabbergast.wandkit.core.data.events.createEventsApi
import com.flabbergast.wandkit.core.data.events.createEventsRepository
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.createCommonInterceptor
import com.flabbergast.wandkit.core.data.networking.createHttpClient
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher
import com.flabbergast.wandkit.core.models.WandKitClient
import com.flabbergast.wandkit.core.models.WandKitClientImpl
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class WandKitSdkContainer private constructor(
    private val config: WandKitConfig,
): InstanceKeeper.Instance {
    internal val backgroundDispatcher = BackgroundDispatcher()
    internal val wandKitClient: WandKitClient = WandKitClientImpl(backgroundDispatcher)

    internal val appConfiguration = createAppConfiguration(config.isDebugLoggingEnabled)

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
    ) }

    internal val eventsApi: WandKitApi<EventsApi> by lazy {
        createEventsApi(httpClient, appConfiguration.baseUrl)
    }

    internal val eventsRepository: EventsRepository by lazy { createEventsRepository(eventsApi, appConfiguration) }


    internal companion object {
        private var instance: WandKitSdkContainer? = null

        fun get(): WandKitSdkContainer = instance ?: error("WandKit SDK isn't initialized.")
        fun init(config: WandKitConfig) {
            instance = WandKitSdkContainer(config)
        }
    }
}