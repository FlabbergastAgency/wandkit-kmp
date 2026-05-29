package com.flabbergast.wandkit.ui.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.flabbergast.wandkit.core.components.root.WandKitComponent

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