package com.flabbergast.wandkit.core.platform

internal expect class PlatformContext

internal interface InstallReferralCodeProvider {
    suspend fun getReferralCode(): String?
}

internal expect fun createInstallReferralCodeProvider(platformContext: PlatformContext?): InstallReferralCodeProvider
