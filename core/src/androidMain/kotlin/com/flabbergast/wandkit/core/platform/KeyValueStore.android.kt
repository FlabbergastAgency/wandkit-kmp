package com.flabbergast.wandkit.core.platform

import android.content.Context
import android.content.SharedPreferences

private const val PREFERENCES_NAME = "com.flabbergast.wandkit.storage"

/**
 * Falls back to in-memory storage when the SDK was configured without a context,
 * which is the same degradation the install referrer provider already accepts.
 */
internal actual fun createKeyValueStore(platformContext: PlatformContext?): KeyValueStore {
    val context = platformContext?.applicationContext ?: return InMemoryKeyValueStore()

    return SharedPreferencesKeyValueStore(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )
}

private class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean = preferences.getBoolean(key, false)

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        preferences.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
