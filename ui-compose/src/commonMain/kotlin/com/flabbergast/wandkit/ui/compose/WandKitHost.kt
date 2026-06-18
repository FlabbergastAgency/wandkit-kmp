package com.flabbergast.wandkit.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.flabbergast.wandkit.core.components.root.WandKitComponentFactory

@Composable
public fun WandKitHost(
    theme: WandKitTheme = WandKitThemeDefaults.system(),
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
) {
    val componentContext = rememberWandKitComponentContext()
    val wandKitComponent = retain(componentContext) {
        WandKitComponentFactory.get().create(context = componentContext)
    }

    WandKitThemeProvider(theme = theme) {
        WandKitRootView(
            component = wandKitComponent,
            contentAlignment = contentAlignment,
            modifier = modifier.fillMaxSize()
        )
    }
}
