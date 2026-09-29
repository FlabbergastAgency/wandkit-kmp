package com.flabbergast.wandkit.sample

import android.app.Application
import android.util.Log
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.config.WandKitAccessGateOptions
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.config.WandKitSessionReplayOptions
import com.flabbergast.wandkit.core.configure
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachment
import com.flabbergast.wandkit.core.feedback.WandKitDebugAttachmentsProvider

/**
 * Configures WandKit from `Application.onCreate`, before any Activity exists:
 * that is what lets the SDK see every Activity (screenshot reporting, the
 * feedback screen) and put the invite gate up before the first frame.
 */
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        configureWandKit()

        // Right after configure: fingerprint accuracy decays fast, so detection
        // has to run long before there is any UI to show the result in.
        WandKit.detectReferralOnFirstLaunchIfNeeded()

        if (BuildConfig.WANDKIT_ACCESS_GATE) {
            AccessGateDebug.install(this)
        }
    }

    /** Also called a second time by the gate debug hooks (see [AccessGateDebug]). */
    fun configureWandKit() {
        WandKit.configure(
            config = WandKitConfig(
                // The local dev stack (the production API has no posts
                // endpoints yet) - same project and hosts as the iOS example.
                // Plain http, so the sample manifest allows cleartext traffic.
                apiKey = BuildConfig.WANDKIT_API_KEY_OVERRIDE.ifBlank { "wk_ZcesAUIcwicpEB1SL28PKKVcRgKY3JNLsNPAF840Cps" },
                isDebugLoggingEnabled = BuildConfig.WANDKIT_DEBUG_LOGGING,
                apiBaseUrl = BuildConfig.WANDKIT_API_BASE_URL_OVERRIDE.ifBlank { "http://192.168.1.79:8081" },
                feedbackWebUrl = "http://192.168.1.79:3002",
                screenshotReporting = true,
                // Sample only: a couple of generated lines standing in for
                // whatever your app's own log file/JSON dump would contain.
                debugAttachmentsProvider = WandKitDebugAttachmentsProvider {
                    val now = System.currentTimeMillis()
                    listOf(
                        WandKitDebugAttachment.text(
                            text = "[$now] sample app log\n[$now] screenshot report triggered",
                            fileName = "sample-app.log",
                        ),
                    )
                },
                // Records the last minute of frames, touches and
                // WandKit.event calls, attached to a screenshot report when
                // the user leaves the replay switch on. Frames go to the
                // app's cache directory rather than the heap.
                sessionReplay = WandKitSessionReplayOptions(),
                // Off unless built with -Pwandkit.sample.accessGate=true (see
                // build.gradle.kts): the gate screen covers the app while the
                // project has invite gating on in the dashboard.
                accessGate = if (BuildConfig.WANDKIT_ACCESS_GATE) {
                    WandKitAccessGateOptions(onStateChange = { state ->
                        Log.i("WandKitSample", "Access gate state: $state")
                        AccessGateDebug.onStateChange(state)
                    })
                } else {
                    null
                },
            ),
            context = this,
        )
    }
}
