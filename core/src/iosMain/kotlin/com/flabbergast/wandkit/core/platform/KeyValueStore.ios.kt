package com.flabbergast.wandkit.core.platform

import platform.Foundation.NSUserDefaults

internal actual fun createKeyValueStore(platformContext: PlatformContext?): KeyValueStore =
    UserDefaultsKeyValueStore(NSUserDefaults.standardUserDefaults)

private class UserDefaultsKeyValueStore(
    private val defaults: NSUserDefaults,
) : KeyValueStore {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(
        key: String,
        value: String,
    ) {
        defaults.setObject(value, key)
    }

    override fun getBoolean(key: String): Boolean = defaults.boolForKey(key)

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        defaults.setBool(value, key)
    }
}
