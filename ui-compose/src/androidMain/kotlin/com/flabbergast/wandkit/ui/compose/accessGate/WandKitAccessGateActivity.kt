package com.flabbergast.wandkit.ui.compose.accessGate

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.net.toUri
import com.flabbergast.wandkit.core.InternalWandKitApi
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateUi
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateViewState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.replay.InstallReplayMasking

private const val TAG = "WandKitAccessGate"

/**
 * The invite gate screen, full screen over the host app while the gate is
 * checking or blocked. `core` starts it by class name (it cannot depend on
 * this module) as soon as the host's first Activity is created, and again
 * whenever a host Activity resumes while the gate is still up.
 *
 * Everything it shows comes from the gate controller through
 * [WandKitAccessGateUi]. Its accent is the one configured through the SDK
 * (`WandKitFeedbackTheme.primaryColor`), else the host app theme's primary
 * colour, else the SDK theme's tint. It finishes itself, without an animation, once the
 * state no longer blocks. Back sends the whole task to the background instead
 * of revealing the app. If it is ever created with nothing to block - a
 * process-death restore before `configure` ran, a stale start - it finishes
 * at once.
 *
 * Public only because the manifest has to name a public class; never start
 * it yourself.
 */
@OptIn(InternalWandKitApi::class)
public class WandKitAccessGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (WandKitAccessGateUi.currentViewState.phase == WandKitAccessGateViewState.Phase.Hidden) {
            finishWithoutAnimation()
            return
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Leaving is fine, getting past the gate is not.
                    moveTaskToBack(true)
                }
            },
        )

        // Resolved once per window: a configure or theme change recreates the
        // Activity (uiMode) or never happens while the gate is up.
        val sdkAccent = AccessGateColors.parseOpaqueHex(WandKitAccessGateUi.configuredAccentColor)
        val appThemeAccent = if (sdkAccent == null) appThemeAccentColor() else null

        setContent {
            InstallReplayMasking()
            val state by WandKitAccessGateUi.viewState.collectAsState(initial = WandKitAccessGateUi.currentViewState)
            // Survives rotation, dark mode and font scale changes, and the
            // offline detour after a failed claim.
            var code by rememberSaveable { mutableStateOf("") }

            LaunchedEffect(state.phase) {
                if (state.phase == WandKitAccessGateViewState.Phase.Hidden) {
                    finishWithoutAnimation()
                }
            }

            WandKitThemeProvider(theme = WandKitThemeDefaults.system()) {
                AccessGateView(
                    state = state,
                    code = code,
                    onCodeChange = { code = it },
                    onSubmit = { WandKitAccessGateUi.submitCode(code) },
                    onRetry = WandKitAccessGateUi::retry,
                    onOpenHelp = ::openHelpUrl,
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
                    accent = AccessGateColors.resolveAccent(
                        sdkAccent = sdkAccent,
                        appThemeAccent = appThemeAccent,
                        defaultTint = WandKitColors.tintColor,
                    ),
                )
            }
        }
    }

    private fun openHelpUrl(url: String) {
        val uri = url.toUri()
        if (uri.scheme != "https" && uri.scheme != "http") {
            Log.w(TAG, "Ignoring a help URL that is not http(s)")
            return
        }
        try {
            // Its own task, so the browser never sits on top of the gate in
            // the host's task.
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app can open the help URL", e)
        }
    }

    private fun finishWithoutAnimation() {
        if (isFinishing) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
