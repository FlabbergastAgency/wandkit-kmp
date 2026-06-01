package com.flabbergast.wandkit.core.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.config.createAppConfiguration
import com.flabbergast.wandkit.core.data.events.EventsApi
import com.flabbergast.wandkit.core.data.events.EventsApiImpl
import com.flabbergast.wandkit.core.data.events.EventsRepositoryImpl
import com.flabbergast.wandkit.core.data.events.createEventsApi
import com.flabbergast.wandkit.core.data.events.createEventsRepository
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpClient
import com.flabbergast.wandkit.core.data.networking.createCommonInterceptor
import com.flabbergast.wandkit.core.data.networking.createHttpClient
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher
import com.flabbergast.wandkit.core.models.WandKitClientImpl
import kotlinx.serialization.json.Json

private const val BASE_URL = "http://localhost:8080"
internal class WandKitSdkContainer private constructor(
    private val config: WandKitConfig,
): InstanceKeeper.Instance {
    internal val backgroundDispatcher = BackgroundDispatcher()
    internal val wandKitClient = WandKitClientImpl(backgroundDispatcher)

    internal val appConfiguration = createAppConfiguration().also { println("[matko] $it") }

    internal val json: Json by lazy { createJson() }

    internal val httpClient: WandKitHttpClient by lazy { createHttpClient(
        json = json,
        commonInterceptor = createCommonInterceptor(config.apiKey),
    ) }

    internal val eventsApi: WandKitApi<EventsApi> by lazy {
        createEventsApi(httpClient, BASE_URL)
    }

    internal val eventsRepository: EventsRepository by lazy { createEventsRepository(eventsApi) }


    internal companion object {
        private var instance: WandKitSdkContainer? = null

        fun get(): WandKitSdkContainer = instance ?: error("WandKit SDK isn't initialized.")
        fun init(config: WandKitConfig) {
            instance = WandKitSdkContainer(config)
        }
    }
}