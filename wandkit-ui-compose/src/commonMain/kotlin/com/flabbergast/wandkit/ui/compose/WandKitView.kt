package com.flabbergast.wandkit.ui.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.flabbergast.wandkit.core.components.root.WandKitComponent
import com.flabbergast.wandkit.core.components.root.WandKitComponentFactory

@Composable
internal fun WandKitRootView(
    component: WandKitComponent,
) {
    Children(component.stack) { child ->
        when (child.instance) {
            WandKitComponent.Child.Test -> Text("Child test instance")
        }
    }
}

@Composable
internal fun InternalWandKitHost() {
    val componentContext = rememberWandKitComponentContext()
    val wandKitComponent = retain(componentContext) { WandKitComponentFactory.get().create(context = componentContext) }

    WandKitRootView(wandKitComponent)
}