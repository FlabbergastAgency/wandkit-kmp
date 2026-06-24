package com.flabbergast.wandkit.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.core.WandKit
import com.flabbergast.wandkit.ui.compose.WandKitHost
import kotlinx.coroutines.launch

private const val SAMPLE_REFERRAL_CAMPAIGN = "samplecampaign"

@Composable
fun App() {
    MaterialTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
        ) {
            Content(snackbarHostState = snackbarHostState)
            WandKitHost()
        }
    }
}

@Composable
private fun Content(snackbarHostState: SnackbarHostState) {
    var showContent by remember { mutableStateOf(false) }
    var userIdInput by remember { mutableStateOf("") }
    var referralUrl by remember { mutableStateOf<String?>(null) }
    var redeemCodeInput by remember { mutableStateOf("") }
    var redeemResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingReferral by remember { mutableStateOf(false) }
    var isRedeeming by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val activeUserId = userIdInput.trim().takeIf { it.isNotEmpty() }

    LaunchedEffect(showContent) {
        if (showContent) {
            WandKit.event(
                name = "my_event",
            )
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = userIdInput,
            onValueChange = { value ->
                userIdInput = value
                referralUrl = null
                redeemResult = null
                errorMessage = null

                val trimmedValue = value.trim()
                if (trimmedValue.isEmpty()) {
                    WandKit.clearUser()
                } else {
                    WandKit.identify(trimmedValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("User ID") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = activeUserId?.let { "Active user: $it" } ?: "Enter a user ID to enable referrals",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val currentUserId = activeUserId ?: return@Button
                coroutineScope.launch {
                    isGeneratingReferral = true
                    referralUrl = null
                    redeemResult = null
                    errorMessage = null

                    val referral = runCatching {
                        WandKit.invite(
                            userId = currentUserId,
                            campaign = SAMPLE_REFERRAL_CAMPAIGN,
                        )
                    }

                    referral
                        .onSuccess {
                            referralUrl = it?.url
                            if (it == null) {
                                errorMessage = "Failed to generate referral link"
                            }
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Failed to generate referral link"
                        }

                    isGeneratingReferral = false
                }
            },
            enabled = activeUserId != null && !isGeneratingReferral,
        ) {
            Text(if (isGeneratingReferral) "Generating..." else "Generate referral link")
        }
        if (referralUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val url = referralUrl ?: return@Button
                    clipboardManager.setText(AnnotatedString(url))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Copied referral link")
                    }
                },
            ) {
                Text("Copy referral link")
            }
        }
        AnimatedVisibility(activeUserId != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = redeemCodeInput,
                    onValueChange = {
                        redeemCodeInput = it
                        redeemResult = null
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Invite code") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val code = redeemCodeInput.trim()
                        if (code.isEmpty()) {
                            errorMessage = "Enter an invite code"
                            return@Button
                        }

                        coroutineScope.launch {
                            isRedeeming = true
                            redeemResult = null
                            errorMessage = null

                            val redemption = runCatching { WandKit.redeemCode(code) }
                            redemption
                                .onSuccess {
                                    redeemResult = it?.let { match ->
                                        "Redeemed ${match.shortPath} for ${match.inviterId}"
                                    }
                                    if (it == null) {
                                        errorMessage = "Failed to redeem invite"
                                    }
                                }
                                .onFailure {
                                    errorMessage = it.message ?: "Failed to redeem invite"
                                }

                            isRedeeming = false
                        }
                    },
                    enabled = !isRedeeming,
                ) {
                    Text(if (isRedeeming) "Redeeming..." else "Redeem invite")
                }
            }
        }
        if (redeemResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = redeemResult!!,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showContent = !showContent }) {
            Text("Feedback Form")
        }
        AnimatedVisibility(showContent) {
            val greeting = remember { Greeting().greet() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Compose: $greeting")
            }
        }
    }
}
