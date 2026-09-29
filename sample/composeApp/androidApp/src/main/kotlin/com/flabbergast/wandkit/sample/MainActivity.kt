package com.flabbergast.wandkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** WandKit itself is configured in [SampleApplication]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            if (BuildConfig.WANDKIT_ACCESS_GATE) {
                // Gate test builds only: a red host screen with test hooks.
                DebugHostContent(activity = this, label = "MainActivity")
            } else {
                App()
            }
        }
    }
}
