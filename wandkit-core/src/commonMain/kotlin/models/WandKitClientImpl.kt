package com.flabbergast.wandkit.core.models

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

internal class WandKitClientImpl(
    backgroundDispatcher: BackgroundDispatcher,
) : WandKitClient {

    private val scope = CoroutineScope(backgroundDispatcher.dispatcher + SupervisorJob())

    override fun event(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    ) {
        scope.launch {
            WandKitSdkContainer.get().eventsRepository.trackEvent(
                WandKitEvent(
                    name = name,
                    properties = properties,
                    occurredAt = occurredAt ?: Clock.System.now(),
                )
            )
        }
    }
}