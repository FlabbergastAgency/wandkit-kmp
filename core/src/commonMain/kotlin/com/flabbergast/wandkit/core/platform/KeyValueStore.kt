package com.flabbergast.wandkit.core.platform

/**
 * The small amount of state the SDK has to remember between launches.
 *
 * Deliberately tiny: referral attribution needs an install to look like the same
 * install tomorrow, and nothing here is worth a database or an extra dependency.
 */
internal interface KeyValueStore {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun getBoolean(key: String): Boolean

    fun putBoolean(
        key: String,
        value: Boolean,
    )
}

internal expect fun createKeyValueStore(platformContext: PlatformContext?): KeyValueStore

/**
 * Fallback for when no platform storage is reachable.
 *
 * Values live only as long as the process, so anything built on top degrades to
 * "every launch looks like a fresh install". That is the same silent degradation
 * [InstallReferralCodeProvider] already takes when it has no context.
 */
internal class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String

    override fun putString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun getBoolean(key: String): Boolean = values[key] as? Boolean ?: false

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        values[key] = value
    }
}
