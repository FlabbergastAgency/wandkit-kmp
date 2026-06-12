package com.flabbergast.wandkit.core.platform

internal actual class PlatformContext

internal actual fun createInstallReferralCodeProvider(platformContext: PlatformContext?): InstallReferralCodeProvider =
    StubInstallReferralCodeProvider

private data object StubInstallReferralCodeProvider : InstallReferralCodeProvider {
    override suspend fun getReferralCode(): String? = null
}
