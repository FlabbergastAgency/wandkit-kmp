# WandKit KMP

This is the Compose Multiplatform SDK for WandKit.

This README covers:
- SDK setup
- event tracking
- form rendering
- Compose UI host setup
- available configuration

## Modules

- `wandkit-core`: SDK configuration, identity, and event tracking
- `wandkit-ui-compose`: Compose host and UI for rendering forms

## What You Can Configure

`WandKitConfig` currently supports:

- `apiKey`: your WandKit API key
- `isDebugLoggingEnabled`: enables SDK debug logging

Example:

```kotlin
WandKitConfig(
    apiKey = "your_api_key",
    isDebugLoggingEnabled = true,
)
```

## Setup

Add the SDK modules to your app:

```kotlin
implementation("com.flabbergast.wandkit:core:<version>")
implementation("com.flabbergast.wandkit:ui-compose:<version>")
```

Configure WandKit must be called before calling other WandKit methods.

### Android

```kotlin
WandKit.configure(
    config = WandKitConfig(
        apiKey = "your_api_key",
        isDebugLoggingEnabled = true,
    ),
    context = applicationContext,
)
```

### Common

```kotlin
WandKit.configure(
    config = WandKitConfig(
        apiKey = "your_api_key",
        isDebugLoggingEnabled = true,
    ),
)
```

## Identify A User

If you have a known user id, identify the user before sending events:

```kotlin
WandKit.identify(userId = "user_123")
```

Clear the identified user when needed:

```kotlin
WandKit.clearUser()
```

## Track Events

Send events with a name and optional string properties:

```kotlin
WandKit.event(
    name = "checkout_started",
    properties = mapOf(
        "plan" to "pro",
        "entry_point" to "pricing_screen",
    ),
)
```

You can also provide a custom event timestamp:

```kotlin
WandKit.event(
    name = "signup_completed",
    occurredAt = occurredAt,
)
```

## Referrals

The SDK also supports creating and redeeming referral links and codes.

### Create A Referral Link

Identify the current user first, then create a referral for a campaign:

```kotlin
WandKit.identify(userId = "user_123")

val referral = WandKit.invite(
    userId = "user_123",
    campaign = "samplecampaign",
)

val referralUrl = referral?.url
```

`WandKit.invite(...)` returns `ReferralInfo?`. Use `ReferralInfo.url` as the shareable referral link.

You can also pass optional string properties:

```kotlin
val referral = WandKit.invite(
    userId = "user_123",
    campaign = "samplecampaign",
    properties = mapOf(
        "source" to "profile_screen",
    ),
)
```

### Inspect A Referral

If you have a referral short path, you can fetch its metadata:

```kotlin
val referral = WandKit.getReferral(path = "abc123")
```

`WandKit.getReferral(...)` returns `GetReferralResponse?`.

### Track An Inviter's Progress

How far an inviter is toward their reward - what drives an in-app
"3 of 5 friends joined" meter:

```kotlin
val progress = WandKit.getReferralProgress(
    userId = "user_123",
    campaign = "samplecampaign",
)

val joined = progress?.convertedCount
val goal = progress?.reward?.threshold
```

`WandKit.getReferralProgress(...)` returns `ReferralProgress?`, and `null` when the
campaign does not exist or this inviter has no referral yet - call
`WandKit.invite(...)` first.

`convertedCount` is what counts toward the reward: claims that went on to sign up.
`claimedCount` is the larger number of installs that merely entered the code.

### Detect Which Referral An Install Came From

Ask the backend which referral this install probably came from. **Nothing is bound
by this** - the returned code is meant to be offered back to the user to confirm or
replace, and `redeemCode` is what actually claims it:

```kotlin
val detection = WandKit.detectReferral()
val prefill = detection?.code
```

Fingerprint accuracy decays quickly and the server-side match window is short, so
detection has to run early - long before the user has agreed to anything. Call this
right after `configure`:

```kotlin
WandKit.detectReferralOnFirstLaunchIfNeeded()
```

It runs once per install, in the background, and persists the result. Read it back
whenever your UI is ready:

```kotlin
val detection = WandKit.detectedReferral
```

A network failure deliberately does not count as an attempt, so the next launch
retries - a dropped attempt costs an inviter a referral they earned.

### Redeem A Referral Code

Redeem a code, whether the user typed it or confirmed a detected one:

```kotlin
val match = WandKit.redeemCode(code = "INVITE_CODE")
```

`WandKit.redeemCode(...)` returns `ReferralMatch?`. This is the only call that
creates a claim.

### Report Conversions From Your Own Backend

`WandKit.installId` is this device's install ID, the same one `redeemCode` claims
with, and it is stable across launches. Forward it to your own backend so it can
report referral conversions server-to-server:

```kotlin
myBackend.reportReferralAttribution(wandkitInstallId = WandKit.installId)
```

### Match An Install Referral

You can also ask the SDK to read the install referral code from the platform provider and redeem it:

```kotlin
val match = WandKit.matchReferral()
```

`WandKit.matchReferral()` returns `ReferralMatch?`.

Note that this predates `detectReferral()` and claims the referral immediately,
without asking the user. Prefer detection unless you specifically want the old
auto-claim behaviour.

### Install Referral Code Provider

Important: if you want to use the install referral code provider, you must pass `context` when calling `WandKit.configure(...)` on Android.

Without `context`, the SDK cannot create the Android install referrer client, so `WandKit.getInstallReferralCode()` and `WandKit.matchReferral()` will not be able to read the install referral code.

The SDK exposes the raw install referral code lookup as well:

```kotlin
val installReferralCode = WandKit.getInstallReferralCode()
```

Required Android setup:

```kotlin
WandKit.configure(
    config = WandKitConfig(
        apiKey = "your_api_key",
        isDebugLoggingEnabled = true,
    ),
    context = applicationContext,
)
```

On Android, this uses the Play Install Referrer API and extracts the `referral_code` query parameter from the install referrer payload.

`WandKit.matchReferral()` uses this provider internally. If a referral code is available, it redeems that code automatically.

On iOS, the install referral code provider is currently not implemented and always returns `null`. Because of that:

- `WandKit.getInstallReferralCode()` returns `null` on iOS
- `WandKit.matchReferral()` also returns `null` on iOS unless install referral support is implemented there later

## How Forms Work

Forms are event-driven.

When you call `WandKit.event(...)`, the backend may return a form for that event. If your app has the Compose host mounted, the SDK will present that form automatically.

There is no separate public API for manually opening a form.

## Compose Host Setup

To render forms, add `WandKitHost()` to your Compose UI tree.

```kotlin
@Composable
fun App() {
    MaterialTheme {
        Box {
            MainContent()
            WandKitHost()
        }
    }
}
```

`WandKitHost()` should be mounted at the root of the composable container where forms can appear.

If that container uses `ModalBottomSheetLayout`, place the host at that root level so the SDK can present forms correctly inside the same container.

In practice, do not place it deep inside a screen subtree that may not be present when an event returns a form.

## UI Customization

You can provide a custom theme to `WandKitHost()`:

```kotlin
WandKitHost(
    theme = WandKitThemeDefaults.system(),
)
```

Available defaults:

- `WandKitThemeDefaults.light()`
- `WandKitThemeDefaults.dark()`
- `WandKitThemeDefaults.system()`

You can also construct a custom `WandKitTheme` with your own colors and typography.

## Minimal Example

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WandKit.configure(
            config = WandKitConfig(
                apiKey = "your_api_key",
                isDebugLoggingEnabled = true,
            ),
            context = applicationContext,
        )

        setContent {
            MaterialTheme {
                Box {
                    ScreenContent(
                        onAction = {
                            WandKit.identify("user_123")
                            WandKit.event(name = "screen_action_tapped")
                        }
                    )

                    WandKitHost()
                }
            }
        }
    }
}
```
