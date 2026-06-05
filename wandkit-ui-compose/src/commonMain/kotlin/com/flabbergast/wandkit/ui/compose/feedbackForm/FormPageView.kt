package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent

@Composable
internal fun FormPageView(
    component: FormPageComponent,
) {
    val state by component.viewState.subscribeAsState()
    Text(state.page?.toString().orEmpty())
}