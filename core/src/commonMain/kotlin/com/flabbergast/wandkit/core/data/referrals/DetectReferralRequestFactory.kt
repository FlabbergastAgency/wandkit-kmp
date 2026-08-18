package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.referrals.dto.DetectReferralRequestDto
import com.flabbergast.wandkit.core.platform.DeviceFingerprint
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive

/**
 * Builds the fingerprint the backend matches an install against a link with.
 *
 * `viewport_*` repeats the screen size on purpose: the fields exist because the
 * web SDK can distinguish the two, and sending the screen keeps a native install
 * comparable to the browser session that tapped the link.
 */
internal fun createDetectReferralRequest(
    installId: String,
    firstLaunchAt: Instant,
    appConfiguration: AppConfiguration,
    fingerprint: DeviceFingerprint,
): DetectReferralRequestDto =
    DetectReferralRequestDto(
        installId = installId,
        userAgent = appConfiguration.userAgent(fingerprint.deviceModel),
        language = fingerprint.language,
        languages = fingerprint.languages,
        timezone = fingerprint.timezone,
        timezoneOffsetMinutes = fingerprint.timezoneOffsetMinutes,
        platform = appConfiguration.platformName,
        screenWidth = fingerprint.screenWidth,
        screenHeight = fingerprint.screenHeight,
        viewportWidth = fingerprint.screenWidth,
        viewportHeight = fingerprint.screenHeight,
        devicePixelRatio = fingerprint.devicePixelRatio,
        clientTimestamp = Clock.System.now().toString(),
        extra = mapOf("first_launch_at" to JsonPrimitive(firstLaunchAt.toString())),
    )

private fun AppConfiguration.userAgent(deviceModel: String): String =
    "WandKit/$libraryVersion ($platformName $platformVersion; $deviceModel)"
