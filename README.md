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
