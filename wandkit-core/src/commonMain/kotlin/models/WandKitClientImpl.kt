package com.flabbergast.wandkit.core.models

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

internal class WandKitClientImpl(
    backgroundDispatcher: BackgroundDispatcher,
) : WandKitClient {
    private val container: WandKitSdkContainer
        get() = WandKitSdkContainer.get()
    private val scope = CoroutineScope(backgroundDispatcher.dispatcher + SupervisorJob())

    override fun trackEvent(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    ) {
        scope.launch {
            WandKitSdkContainer.get().eventsRepository.trackEvent(
                event = WandKitEvent(
                    name = name,
                    properties = properties,
                    occurredAt = occurredAt ?: Clock.System.now(),
                ),
                identifyInfo = container.identityInfo,
            )
        }
    }
}