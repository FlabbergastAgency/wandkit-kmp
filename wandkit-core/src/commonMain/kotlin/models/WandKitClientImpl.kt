package com.flabbergast.wandkit.core.models

import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.domain.events.WandKitEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

internal fun createWandKitClient(): WandKitClient = WandKitClientImpl()

private class WandKitClientImpl : WandKitClient {
    private val container: WandKitSdkContainer
        get() = WandKitSdkContainer.get()
    private val scope = CoroutineScope(container.backgroundDispatcher.dispatcher + SupervisorJob())

    override fun trackEvent(
        name: String,
        properties: Map<String, String>,
        occurredAt: Instant?,
    ) {
        scope.launch {
            container.trackEventUseCase(
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