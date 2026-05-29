package com.flabbergast.wandkit.core.di

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.flabbergast.wandkit.core.data.events.EventsRepositoryImpl
import com.flabbergast.wandkit.core.domain.events.EventsRepository
import com.flabbergast.wandkit.core.domain.threading.BackgroundDispatcher

internal class WandKitSdkContainer private constructor(

): InstanceKeeper.Instance {


    internal val backgroundDispatcher by lazy { BackgroundDispatcher() }

    internal val eventsRepository: EventsRepository by lazy { EventsRepositoryImpl() }

    internal companion object {
        private var count = 0
        private var instance: WandKitSdkContainer? = null

        fun get(): WandKitSdkContainer {
            println("[matko] getting container instance, count $count")
            return instance ?: error("WandKit SDK isn't initialized.")
        }

        fun init() {
            instance = WandKitSdkContainer()
            println("[matko] set container instance to $instance, count $count")
            count += 1

        }
    }
}