package com.flabbergast.wandkit.core.platform

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal actual class PlatformContext internal constructor(
    internal val applicationContext: Context,
)

internal actual fun createInstallReferralCodeProvider(platformContext: PlatformContext?): InstallReferralCodeProvider =
    AndroidInstallReferralCodeProvider(platformContext?.applicationContext)

private class AndroidInstallReferralCodeProvider(
    private val context: Context?,
) : InstallReferralCodeProvider {
    override suspend fun getReferralCode(): String? {
        val applicationContext = context ?: return null

        return suspendCancellableCoroutine { continuation ->
            val referrerClient = InstallReferrerClient.newBuilder(applicationContext).build()

            continuation.invokeOnCancellation {
                runCatching { referrerClient.endConnection() }
            }

            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    val referralCode = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        runCatching {
                            referrerClient.installReferrer.installReferrer.extractReferralCode()
                        }.getOrNull()
                    } else {
                        null
                    }

                    runCatching { referrerClient.endConnection() }

                    if (continuation.isActive) {
                        continuation.resume(referralCode)
                    }
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            })
        }
    }
}

private fun String.extractReferralCode(): String? = parseReferrerParams()[REFERRAL_CODE_QUERY_PARAMETER]?.takeIf { it.isNotBlank() }

private fun String.parseReferrerParams(): Map<String, String> = split(QUERY_PARAMETER_SEPARATOR)
    .mapNotNull { parameter ->
        val parts = parameter.split(KEY_VALUE_SEPARATOR, limit = 2)
        val key = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val value = parts.getOrNull(1) ?: return@mapNotNull null

        key to value
    }.toMap()

private const val REFERRAL_CODE_QUERY_PARAMETER = "referral_code"
private const val QUERY_PARAMETER_SEPARATOR = "&"
private const val KEY_VALUE_SEPARATOR = "="
