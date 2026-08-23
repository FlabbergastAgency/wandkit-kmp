# WandKit KMP

This is the Compose Multiplatform SDK for WandKit.

This README covers:
- SDK setup
- event tracking
- form rendering
- feedback and screenshot reporting
- Compose UI host setup
- available configuration

## Modules

- `wandkit-core`: SDK configuration, identity, and event tracking
- `wandkit-ui-compose`: Compose host and UI for rendering forms

## What You Can Configure

`WandKitConfig` currently supports:

- `apiKey`: your WandKit API key
- `isDebugLoggingEnabled`: enables SDK debug logging
- `apiBaseUrl`: overrides the API host (events, forms, referrals, feedback sessions); `null` uses production
- `feedbackWebUrl`: origin the feedback web app is served from, for pointing a build at a staging deployment (see [Feedback](#feedback))
- `feedbackTheme`: styling for the feedback web app (see [Theming](#theming))
- `screenshotReporting`: turns a screenshot into a "Report a problem?" prompt (see [Screenshot reporting](#screenshot-reporting))

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

A transient failure does not count as an attempt, so the next launch retries - a
dropped attempt costs an inviter a referral they earned. Retries are capped, and
a permanent failure (a rejected key, an unreadable response) gives up at once, so
an install that can never get an answer stops fingerprinting rather than
re-sending on every launch.

`redeemCode` clears the detection on success, since the question it exists to
answer has been answered. If the user dismisses the prefilled code instead, clear
it yourself:

```kotlin
WandKit.clearDetectedReferral()
```

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

## Feedback

`WandKit.presentFeedback()` opens the feedback screen - the feed, the composer, and the roadmap - on top of whatever is currently visible:

```kotlin
WandKit.presentFeedback()
```

It uses whichever user `identify(...)` last named. Without one the session is anonymous, which the backend makes **read-only**: the user can read the feed and the roadmap but not post, comment, or vote. Nothing errors and nothing is hidden - the web app simply renders without the write actions.

Open straight on the new-post composer, optionally seeded with something the user already typed elsewhere in your app:

```kotlin
WandKit.presentFeedback(
    startAt = WandKitFeedbackScreen.Composer(
        WandKitComposerPrefill(description = "It crashes when…"),
    ),
)
```

`WandKitComposerPrefill` can also pre-select the type and attach an image - say, a screenshot your own "report a bug" button captured:

```kotlin
WandKit.presentFeedback(
    startAt = WandKitFeedbackScreen.Composer(
        WandKitComposerPrefill(
            type = WandKitPostType.BUG,
            attachments = listOfNotNull(WandKitComposerAttachment.image(bitmap)),
        ),
    ),
)
```

`WandKitComposerAttachment.image(bitmap)` is an Android helper: it JPEG-encodes the bitmap, downscaled to 2000 px on the long edge, and returns `null` only if the image can't be made to fit the SDK's payload cap. A `type` the project has disabled is ignored by the composer.

The screen itself is a WandKit-hosted web app rendered in a WebView, inside an Activity the SDK declares in its own manifest - there is nothing to add to yours. It changes when WandKit ships, not when your app does.

**Android only.** The iOS targets of this library log a warning and do nothing; use the native WandKit iOS SDK there.

For custom launching - a notification tap, a deep link handler, anywhere else that already holds a `Context` - build the `Intent` yourself instead of going through `presentFeedback`:

```kotlin
val intent = WandKit.feedbackIntent(context, startAt)
context.startActivity(intent)
```

### Theming

Style the feedback web app with `feedbackTheme` at configure time. It is serialized into the webview and applied as CSS custom properties.

```kotlin
WandKit.configure(
    config = WandKitConfig(
        apiKey = "your_api_key",
        feedbackTheme = WandKitFeedbackTheme(
            primaryColor = "#4F46E5",
            backgroundColor = "#FFFFFF",
            cornerRadius = 16.0,
            fontFamily = "-apple-system, system-ui, sans-serif",
            preferredColorScheme = WandKitColorSchemePreference.SYSTEM,
        ),
    ),
    context = applicationContext,
)
```

| Field | Type | Default | Notes |
|---|---|---|---|
| `primaryColor` | `String?` | `null` | Buttons, links, anything accented |
| `backgroundColor` | `String?` | `null` | Page background, also painted behind the webview so a slow first paint doesn't flash white |
| `cornerRadius` | `Double?` | `null` | |
| `fontFamily` | `String?` | `null` | A CSS font family the webview can resolve - a web-safe stack, or a font the hosted app bundles. A font that only exists inside your app will not resolve |
| `preferredColorScheme` | `WandKitColorSchemePreference` | `SYSTEM` | `LIGHT` and `DARK` also override the native chrome around the webview |

Colors are CSS hex strings (`#RRGGBB`, or `#RRGGBBAA` when translucent); the native chrome around the webview - window background, spinner - only honours the opaque form. Omit the theme entirely and the web app keeps its own defaults.

`feedbackWebUrl` overrides the origin the web app is served from, and `apiBaseUrl` the API host, for pointing a build at a staging deployment. Both take plain `http://` origins for a local stack, which also needs `android:usesCleartextTraffic="true"` (or a network security config) in your manifest.

### Screenshot reporting

Opt in at configure time and a screenshot turns into a "Report a problem?" prompt:

```kotlin
WandKit.configure(
    config = WandKitConfig(
        apiKey = "your_api_key",
        screenshotReporting = true,
    ),
    context = applicationContext,
)
```

When the user takes a screenshot, `WandKitHost()` shows a small card over your app with a thumbnail of what they just captured. The whole flow is native - there is no webview involved:

1. **Card.** "Report a problem" moves to the text box; "Not now" or tapping outside dismisses it.
2. **Composer.** The thumbnail stays visible above a plain text field ("What went wrong?"). "Send" uploads the screenshot and creates the report post directly against the API, using a short-lived posts session minted just for that send (the token never touches disk). A failure shows an inline message under the field and relabels the button "Try again"; nothing is retried automatically.
3. **Thank-you.** On success the card shows a short thank-you and auto-dismisses about 1.2 seconds later.

The report lands as a pending post (`type=bug`) in the project's triage inbox, same as anything else a user sends - the team publishes it from there. Another screenshot within two seconds of the last card is debounced rather than shown again.

Requirements:

- **Android 14+.** Uses `Activity.ScreenCaptureCallback`; earlier versions never see a card. The SDK's own manifest merges the `android.permission.DETECT_SCREEN_CAPTURE` normal permission into your app - there is no runtime prompt to wire up.
- **An identified user** (`identify(...)`). Anonymous sessions cannot post, so without one the screenshot is skipped silently rather than shown to a user who could never submit it.
- **`WandKitHost()` mounted** on the screen, the same as for survey forms.
- **`configure` called in `Application.onCreate`**, so the SDK sees the first Activity and can register its capture callback as soon as it resumes.

What it does not do:

- **Read the gallery.** The image is read back from your app's own window via `PixelCopy`, not from Photos, so there is no permission prompt for it. `SurfaceView` content comes out black, and a window flagged `FLAG_SECURE` never triggers a callback at all.
- **Upload anything until Send.** The image stays in memory while the card or text box is open, and only leaves the device once the user taps Send.
- **Prompt on Android 13 and below.** There is no capture callback to hook there. If you want a screenshot-report entry point on older devices, wire your own trigger to the (webview) composer directly - this deep-links to the simplified web composer, which still accepts a `type`:

  ```kotlin
  WandKit.presentFeedback(
      startAt = WandKitFeedbackScreen.Composer(
          WandKitComposerPrefill(type = WandKitPostType.BUG),
      ),
  )
  ```

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

`WandKitHost()` should be mounted at the root of the composable container where forms can appear. It also renders the screenshot-report card (see [Screenshot reporting](#screenshot-reporting)) when `screenshotReporting` is enabled.

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
