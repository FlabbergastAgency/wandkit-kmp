package com.flabbergast.wandkit.sample

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.core.accessgate.WandKitAccessGateState

/**
 * Test hooks for driving the invite gate end to end on a device. Only ever
 * used by builds made with `-Pwandkit.sample.accessGate=true`; the default
 * sample never installs or renders any of it.
 *
 * - Every state callback is logged under [TAG] and kept in [history].
 * - The host screen ([DebugHostContent]) is red, so host frames are obvious in
 *   screenshots, and shows the latest state, the history and the pass.
 * - `adb shell am broadcast -p com.flabbergast.wandkit.sample -a <action>`
 *   with the actions below triggers the same hooks while the gate covers the
 *   host's buttons.
 */
object AccessGateDebug {
    const val TAG = "WandKitGateE2E"

    private const val ACTION_PREFIX = "com.flabbergast.wandkit.sample.debug."
    const val ACTION_RESET = ACTION_PREFIX + "RESET"
    const val ACTION_PRESENT_FEEDBACK = ACTION_PREFIX + "PRESENT_FEEDBACK"
    const val ACTION_CONFIGURE_AGAIN = ACTION_PREFIX + "CONFIGURE_AGAIN"
    const val ACTION_DUMP = ACTION_PREFIX + "DUMP"
    const val EXTRA_DELAY_MS = "delay_ms"

    /** Launched by class name: the extra host Activities are declared in the debug manifest only. */
    const val SECOND_ACTIVITY_CLASS = "com.flabbergast.wandkit.sample.SecondHostActivity"

    val history = mutableStateListOf<String>()
    val latest = mutableStateOf<String>("(none yet)")

    private val mainHandler = Handler(Looper.getMainLooper())
    private var application: SampleApplication? = null

    fun onStateChange(state: WandKitAccessGateState) {
        val text = state.toString()
        history += text
        latest.value = text
        Log.i(TAG, "state #${history.size}: $text | pass=${WandKit.accessPass?.code} count=${WandKit.accessPass?.claimCount}")
    }

    fun install(app: SampleApplication) {
        application = app
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val delay = intent.getLongExtra(EXTRA_DELAY_MS, 0L)
                Log.i(TAG, "hook ${intent.action} (delay ${delay}ms)")
                mainHandler.postDelayed({ run(intent.action) }, delay)
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_RESET)
            addAction(ACTION_PRESENT_FEEDBACK)
            addAction(ACTION_CONFIGURE_AGAIN)
            addAction(ACTION_DUMP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(receiver, filter)
        }
        Log.i(TAG, "installed; state after configure: ${WandKit.accessGateState} pass=${WandKit.accessPass?.code}")
    }

    fun run(action: String?) {
        when (action) {
            ACTION_RESET -> WandKit.resetAccessGate()
            ACTION_PRESENT_FEEDBACK -> WandKit.presentFeedback()
            ACTION_CONFIGURE_AGAIN -> application?.configureWandKit()
            ACTION_DUMP -> Unit
        }
        dump(action)
    }

    fun dump(reason: String?) {
        Log.i(
            TAG,
            "dump after $reason: state=${WandKit.accessGateState} pass=${WandKit.accessPass?.code} " +
                "count=${WandKit.accessPass?.claimCount} history=${history.joinToString(" > ")}",
        )
    }
}

/** The host screen of a gated debug build: red, with the gate's state and the hooks. */
@Composable
fun DebugHostContent(activity: Activity, label: String) {
    val pass = WandKit.accessPass
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD50000))
            .safeDrawingPadding()
            .semantics { testTagsAsResourceId = true }
            .testTag("sample.host.root")
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("HOST CONTENT - $label", color = Color.White, modifier = Modifier.testTag("sample.host.marker"))
        Text("State: ${AccessGateDebug.latest.value}", color = Color.White, modifier = Modifier.testTag("sample.host.state"))
        Text(
            "Pass: ${pass?.code ?: "null"} (claims ${pass?.claimCount ?: "-"})",
            color = Color.White,
            modifier = Modifier.testTag("sample.host.pass"),
        )
        Text(
            "History: ${AccessGateDebug.history.joinToString(" > ")}",
            color = Color.White,
            modifier = Modifier.testTag("sample.host.history"),
        )
        Button(onClick = { AccessGateDebug.run(AccessGateDebug.ACTION_RESET) }, modifier = Modifier.testTag("sample.host.reset")) {
            Text("Reset access gate")
        }
        Button(onClick = { AccessGateDebug.run(AccessGateDebug.ACTION_PRESENT_FEEDBACK) }, modifier = Modifier.testTag("sample.host.feedback")) {
            Text("Present feedback")
        }
        Button(
            onClick = {
                Handler(Looper.getMainLooper()).postDelayed({ AccessGateDebug.run(AccessGateDebug.ACTION_PRESENT_FEEDBACK) }, 5_000)
            },
            modifier = Modifier.testTag("sample.host.feedbackDelayed"),
        ) {
            Text("Present feedback in 5 s")
        }
        Button(
            onClick = { activity.startActivity(Intent().setClassName(activity, AccessGateDebug.SECOND_ACTIVITY_CLASS)) },
            modifier = Modifier.testTag("sample.host.openSecond"),
        ) {
            Text("Open another host Activity")
        }
    }
}
