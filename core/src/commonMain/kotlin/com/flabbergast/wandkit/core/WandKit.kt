package com.flabbergast.wandkit.core

import com.flabbergast.wandkit.core.config.WandKitConfig
import com.flabbergast.wandkit.core.di.WandKitSdkContainer
import com.flabbergast.wandkit.core.models.WandKitClient
import com.flabbergast.wandkit.core.domain.referrals.GetReferralResponse
import com.flabbergast.wandkit.core.domain.referrals.ReferralDetection
import com.flabbergast.wandkit.core.domain.referrals.ReferralInfo
import com.flabbergast.wandkit.core.domain.referrals.ReferralMatch
import com.flabbergast.wandkit.core.domain.referrals.ReferralProgress
import com.flabbergast.wandkit.core.feedback.WandKitFeedbackScreen
import com.flabbergast.wandkit.core.feedback.presentFeedbackScreen
import kotlin.time.Instant

public object WandKit {
    private val client: WandKitClient
        get() = WandKitSdkContainer.get().wandKitClient

    public fun configure(
        config: WandKitConfig,
    ) {
        WandKitSdkContainer.init(config)
    }

    /**
     * This device's install ID, the same one [redeemCode] claims with.
     *
     * Forward it to your own backend so it can report referral conversions.
     */
    public val installId: String
        get() = WandKitSdkContainer.get().installIdentity.installId

    /**
     * Identifies the current user, optionally suggesting a display name for
     * them.
     *
     * [displayName] is only a suggestion: it is shown on the user's feedback
     * posts until they set their own name in the feedback UI, and re-identifying
     * with a new suggestion updates it - but never overwrites a name the user
     * has already set themselves.
     */
    public fun identify(
        userId: String,
        displayName: String? = null,
    ) {
        WandKitSdkContainer.get().setUserId(userId, displayName)
    }

    public fun clearUser() {
        WandKitSdkContainer.get().setUserId(null)
    }

    public fun event(
        name: String,
        properties: Map<String, String> = emptyMap(),
        occurredAt: Instant? = null,
    ) {
        client.trackEvent(
            name = name,
            properties = properties,
            occurredAt = occurredAt,
        )
    }

    /**
     * Presents the feedback UI - the feed, the composer, the roadmap - full
     * screen on top of whatever is currently on screen.
     *
     * A one-liner: `WandKit.presentFeedback()`. On Android the SDK launches its
     * own Activity from the one currently in the foreground, so there is nothing
     * to thread through from your navigation. The UI is a WandKit-hosted web
     * app in a WebView, so it changes when WandKit ships, not when your app
     * does; it closes itself through its own UI and the back gesture.
     *
     * It uses whichever user [identify] last named. Without one the session is
     * anonymous, which the backend makes read-only: the user can read the feed
     * and the roadmap but not post or vote.
     *
     * `startAt = WandKitFeedbackScreen.Composer(prefill)` opens straight on the
     * new-post composer, optionally seeded with a title, description, type and
     * image attachments. Read-only sessions land on the feed instead.
     *
     * Android only. The iOS targets of this library log a warning; use the
     * native WandKit iOS SDK there.
     */
    public fun presentFeedback(
        startAt: WandKitFeedbackScreen = WandKitFeedbackScreen.Feed,
    ) {
        presentFeedbackScreen(WandKitSdkContainer.get(), startAt)
    }

    public suspend fun getInstallReferralCode(): String? = WandKitSdkContainer.get().installReferralCodeProvider.getReferralCode()

    public suspend fun invite(
        userId: String,
        campaign: String,
        properties: Map<String, String> = emptyMap(),
    ): ReferralInfo? = WandKitSdkContainer.get().referralsRepository.invite(userId, campaign, properties)

    /**
     * How far an inviter is toward their reward.
     *
     * Null when the campaign does not exist, or when this inviter has no referral
     * yet - create one with [invite] first.
     */
    public suspend fun getReferralProgress(
        userId: String,
        campaign: String,
    ): ReferralProgress? = WandKitSdkContainer.get().referralsRepository.getReferralProgress(userId, campaign)

    public suspend fun getReferral(path: String): GetReferralResponse? =
        WandKitSdkContainer.get().referralsRepository.getReferral(path)

    /**
     * Reports which referral this install probably came from, binding nothing.
     *
     * Offer the returned `code` back to the user to confirm or replace, then pass
     * their answer to [redeemCode] - that is what actually claims it.
     */
    public suspend fun detectReferral(): ReferralDetection? =
        WandKitSdkContainer.get().referralsRepository.detectReferral()

    /**
     * The referral detected for this install, if any. Survives launches, so it is
     * still readable when you get round to asking the user about it.
     */
    public val detectedReferral: ReferralDetection?
        get() = WandKitSdkContainer.get().referralsRepository.detectedReferral

    /**
     * Forgets the detected referral. [redeemCode] already does this on success,
     * so call it only when the user dismisses the prefilled code instead.
     */
    public fun clearDetectedReferral() {
        WandKitSdkContainer.get().referralsRepository.clearDetectedReferral()
    }

    /**
     * Detects this install's referral once, in the background.
     *
     * Call it right after [configure]. Fingerprint accuracy decays quickly and the
     * server-side match window is short, so detection has to happen early - long
     * before the user has agreed to anything. Nothing is claimed here; the result
     * is persisted and readable via [detectedReferral].
     *
     * Runs once per install. A transient failure is retried on later launches,
     * but only up to a ceiling, so an install that can never get an answer stops
     * fingerprinting instead of retrying forever.
     */
    public fun detectReferralOnFirstLaunchIfNeeded() {
        val container = WandKitSdkContainer.get()
        container.fireAndForgetTask {
            container.referralsRepository.detectReferralOnFirstLaunchIfNeeded()
        }
    }

    /**
     * Redeems the Play install referrer code outright, without asking the user.
     *
     * Predates [detectReferral] and binds the install immediately. Prefer
     * detection unless you specifically want the old auto-claim behaviour.
     */
    public suspend fun matchReferral(): ReferralMatch? = WandKitSdkContainer.get().referralsRepository.matchReferral()

    /**
     * Claims a referral for this install using the code the user confirmed or
     * typed. Clears [detectedReferral] on success, since the question it exists
     * to answer has now been answered.
     */
    public suspend fun redeemCode(code: String): ReferralMatch? =
        WandKitSdkContainer.get().referralsRepository.redeemCode(code)
}
