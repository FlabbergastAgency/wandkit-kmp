package com.flabbergast.wandkit.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.flabbergast.wandkit.core.components.root.WandKitComponent
import com.flabbergast.wandkit.core.components.root.WandKitComponentFactory

private const val WANDKIT_ROOT_COMPONENT_KEY = "WandKitRootComponent"

@Composable
public fun WandKitHost(
    theme: WandKitTheme = WandKitThemeDefaults.system(),
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
) {
    val componentContext = rememberWandKitComponentContext()
    val wandKitComponent = remember(componentContext) {
        componentContext.instanceKeeper.getOrCreate(WANDKIT_ROOT_COMPONENT_KEY) {
            WandKitRootComponentHolder(
                component = WandKitComponentFactory.get().create(context = componentContext),
            )
        }.component
    }

    WandKitThemeProvider(theme = theme) {
        WandKitRootView(
            component = wandKitComponent,
            contentAlignment = contentAlignment,
            modifier = modifier.fillMaxSize()
        )
    }
}

private class WandKitRootComponentHolder(
    val component: WandKitComponent,
) : InstanceKeeper.Instance {
    override fun onDestroy() = Unit
}
