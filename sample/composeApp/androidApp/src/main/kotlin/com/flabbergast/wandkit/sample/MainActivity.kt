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
                apiKey = "wk_3H6I5RYCcP1_sTONVQodC7sp6mioBzjNFwd3p6zdAg8",
                isDebugLoggingEnabled = true,
            ),
            context = applicationContext,
        )

        setContent {
            App()
        }
    }
}
