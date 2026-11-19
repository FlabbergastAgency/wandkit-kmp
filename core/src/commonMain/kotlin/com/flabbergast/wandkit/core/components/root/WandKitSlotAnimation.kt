package com.flabbergast.wandkit.core.components.root

import com.flabbergast.wandkit.core.InternalWandKitApi

/**
 * Timing of `WandKitHost`'s transition between overlay slots (form, feature
 * preview, screenshot prompt). Lives in `core` so code that must wait for an
 * overlay to be fully gone (the native review prompt) reads the same numbers
 * `ui-compose` animates with.
 */
@InternalWandKitApi
public object WandKitSlotAnimation {
    public const val DURATION_MILLIS: Int = 220

    /** The enter transition and the exit's scale-out both start this late. */
    public const val DELAY_MILLIS: Int = 90

    /** Until an outgoing slot is fully off screen: its scale-out runs after [DELAY_MILLIS]. */
    public const val EXIT_MILLIS: Int = DELAY_MILLIS + DURATION_MILLIS
}
