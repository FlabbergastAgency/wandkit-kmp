package com.flabbergast.wandkit.ui.compose.replay

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.replay.WandKitReplayMaskProvider
import com.flabbergast.wandkit.core.replay.WandKitReplayMasking
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(InternalWandKitApi::class)
@Composable
internal actual fun InstallReplayMasking() {
    remember { WandKitReplayMasking.addProvider(ComposeReplayMaskProvider) }
}

/**
 * Compose UI isn't made of Views, so the core masker's View walk can't see a
 * Compose text field. This provider finds every Compose root under the
 * captured window (each `AndroidComposeView` is a [ViewRootForTest]) and walks
 * its *unmerged* semantics tree - the same tree accessibility services and
 * UI tests read - masking:
 *
 * - always: password fields ([SemanticsProperties.Password]);
 * - with `maskTextInputs`: editable text ([SemanticsProperties.EditableText]);
 * - with `maskAllText`: any text;
 * - anything marked with [wandKitReplayMasked] or a test tag containing
 *   `wandkit-mask`.
 *
 * `boundsInWindow` is already clipped by scrolling parents and is in the same
 * window coordinates the recorder captures.
 */
@OptIn(InternalWandKitApi::class)
internal object ComposeReplayMaskProvider : WandKitReplayMaskProvider {
    private const val MASK_MARKER = "wandkit-mask"

    override fun collectMaskRects(
        root: View,
        maskTextInputs: Boolean,
        maskAllText: Boolean,
        into: MutableList<Rect>,
    ) {
        visitView(root, maskTextInputs, maskAllText, into)
    }

    private fun visitView(view: View, maskTextInputs: Boolean, maskAllText: Boolean, into: MutableList<Rect>) {
        if (view.visibility != View.VISIBLE) return

        if (view is ViewRootForTest) {
            val rootNode = runCatching { view.semanticsOwner.unmergedRootSemanticsNode }.getOrNull()
            if (rootNode != null) visitNode(rootNode, maskTextInputs, maskAllText, into)
            // Interop Views inside it (AndroidView) are reached by the core
            // View walk; nothing else to do here.
            return
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                visitView(view.getChildAt(index), maskTextInputs, maskAllText, into)
            }
        }
    }

    private fun visitNode(node: SemanticsNode, maskTextInputs: Boolean, maskAllText: Boolean, into: MutableList<Rect>) {
        val config = node.config
        val masked = WandKitReplayMaskedKey in config ||
            SemanticsProperties.Password in config ||
            (maskTextInputs && SemanticsProperties.EditableText in config) ||
            (maskAllText && (SemanticsProperties.Text in config || SemanticsProperties.EditableText in config)) ||
            config.getOrNull(SemanticsProperties.TestTag)?.contains(MASK_MARKER) == true

        if (masked) {
            val bounds = node.boundsInWindow
            if (bounds.width > 0f && bounds.height > 0f) {
                into += Rect(
                    floor(bounds.left).toInt(),
                    floor(bounds.top).toInt(),
                    ceil(bounds.right).toInt(),
                    ceil(bounds.bottom).toInt(),
                )
            }
            // The rect already covers its children.
            return
        }

        for (child in node.children) {
            visitNode(child, maskTextInputs, maskAllText, into)
        }
    }
}
