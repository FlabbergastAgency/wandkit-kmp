package com.flabbergast.wandkit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.config.WandKitConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initWandkit(apiKey = "wk_3H6I5RYCcP1_sTONVQodC7sp6mioBzjNFwd3p6zdAg8", isDebugLoggingEnabled = true)

        setContent {
            App()
        }
    }
}