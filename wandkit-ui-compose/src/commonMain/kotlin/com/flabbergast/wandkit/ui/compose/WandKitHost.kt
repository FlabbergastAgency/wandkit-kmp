package com.flabbergast.wandkit.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import com.flabbergast.wandkit.core.components.root.WandKitComponentFactory

@Composable
fun WandKitHost() {
    val componentContext = rememberWandKitComponentContext()
    val wandKitComponent = retain(componentContext) {
        WandKitComponentFactory.get().create(context = componentContext)
    }

    WandKitRootView(wandKitComponent)
}