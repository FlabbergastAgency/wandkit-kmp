package com.flabbergast.wandkit.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackHandler
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import com.arkivanov.essenty.lifecycle.asEssentyLifecycle
import com.arkivanov.essenty.statekeeper.stateKeeper

/**
 * Unique within the host Activity so we never collide with the host app's own
 * [com.arkivanov.decompose.defaultComponentContext] (which claims the fixed
 * `"STATE_KEEPER_STATE"` key).
 */
private const val WANDKIT_HOST_KEY = "com.flabbergast.wandkit.WandKitHost"

@Composable
internal actual fun rememberWandKitComponentContext(): ComponentContext {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("WandKitHost must be used inside a ComponentActivity")

    return remember(activity) {
        activity.instanceKeeper().getOrCreate(WANDKIT_HOST_KEY) {
            WandKitHostComponentContextHolder(
                context = DefaultComponentContext(
                    lifecycle = activity.lifecycle.asEssentyLifecycle(),
                    stateKeeper = activity.stateKeeper(key = WANDKIT_HOST_KEY),
                    // Own dispatcher so WandKit's retained instances don't share
                    // the Activity ViewModelStore keyspace with the host app.
                    instanceKeeper = InstanceKeeperDispatcher(),
                    backHandler = BackHandler(activity.onBackPressedDispatcher),
                ),
            )
        }.context
    }
}

private class WandKitHostComponentContextHolder(
    val context: ComponentContext,
) : InstanceKeeper.Instance {
    override fun onDestroy() = Unit
}
