package com.flabbergast.wandkit.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** A second host screen for the invite gate device tests (debug builds only). */
class SecondHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DebugHostContent(activity = this, label = "SecondHostActivity") }
    }
}

/** Starts [SecondHostActivity] and finishes at once, like a routing/splash Activity (debug builds only). */
class TrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SecondHostActivity::class.java))
        finish()
    }
}
