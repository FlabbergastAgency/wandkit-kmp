package com.flabbergast.wandkit.ui.compose.replay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics

/**
 * Semantics marker read by the session replay masker. Carried in semantics
 * rather than a test tag so it never clobbers a tag the host app set itself.
 */
internal val WandKitReplayMaskedKey: SemanticsPropertyKey<Unit> = SemanticsPropertyKey("WandKitReplayMasked")

/**
 * Paints over this element in session replay frames - the Compose
 * counterpart of iOS's `.wandKitReplayMasked()`. Use it for anything
 * sensitive the default masking (password fields, text fields, optionally all
 * text) doesn't already cover: account numbers, avatars, a map. Only the
 * recorded frame is masked; what the user sees is untouched.
 *
 * `Modifier.testTag("...wandkit-mask...")` works too, as does a View `tag`
 * or `contentDescription` containing `wandkit-mask` for View-based UI.
 * No effect on iOS targets or when session replay is off.
 */
public fun Modifier.wandKitReplayMasked(): Modifier = semantics {
    this[WandKitReplayMaskedKey] = Unit
}

/**
 * Registers the Compose-aware mask provider with the core recorder, so
 * Compose text fields are masked like `EditText`s. Called from [com.flabbergast.wandkit.ui.compose.WandKitHost],
 * which every app using screenshot reporting already hosts. No-op on iOS.
 */
@Composable
internal expect fun InstallReplayMasking()
