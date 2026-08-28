package com.flabbergast.wandkit.sample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.configure
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WandKit.configure(
            config = WandKitConfig(
                // The local dev stack (the production API has no posts
                // endpoints yet) - same project and hosts as the iOS example.
                // Plain http, so the sample manifest allows cleartext traffic.
                apiKey = "wk_ZcesAUIcwicpEB1SL28PKKVcRgKY3JNLsNPAF840Cps",
                isDebugLoggingEnabled = true,
                apiBaseUrl = "http://192.168.1.79:8081",
                feedbackWebUrl = "http://192.168.1.79:3002",
                screenshotReporting = true,
            ),
            context = applicationContext,
        )

        // Right after configure: fingerprint accuracy decays fast, so detection
        // has to run long before there is any UI to show the result in.
        WandKit.detectReferralOnFirstLaunchIfNeeded()

        setContent {
            App()
        }
    }
}
