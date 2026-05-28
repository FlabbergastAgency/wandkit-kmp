package com.flabbergast.wandkit.core.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface WandKitPresenter {
    public fun present(handle: SurveySessionHandle)
    public fun dismiss(sessionId: String)
}
public interface SurveySessionHandle {
    public val sessionId: String
    public val state: StateFlow<SurveyUiState>
    public val effects: Flow<SurveyEffect>
    public fun dispatch(action: SurveyAction)
}

public data class SurveyUiState(
    val sessionId: String,
    val isVisible: Boolean,
    val page: Any?, // todo create a page ui model
    val canDismiss: Boolean,
    val isSubmitting: Boolean,
    val isCompleted: Boolean
)

public sealed interface SurveyAction {
    public data class SelectStars(val pageId: String, val value: Int) : SurveyAction
    public data class SelectThumb(val pageId: String, val up: Boolean) : SurveyAction
    public data class ToggleOption(val pageId: String, val optionId: String) : SurveyAction
    public data class EnterText(val pageId: String, val text: String) : SurveyAction
    public data object Confirm : SurveyAction
    public data object Skip : SurveyAction
    public data object Dismiss : SurveyAction
}

public sealed interface SurveyEffect {
    public data object Close : SurveyEffect
    public data object PlaySuccessHaptic : SurveyEffect
    public data object PlayStepHaptic : SurveyEffect
    public data object DismissKeyboard : SurveyEffect
}