package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.accessgate.WandKitAccessPass
import com.flabbergast.wandkit.core.platform.KeyValueStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** What the invite gate remembers between launches. */
internal interface AccessGateStore {
    /**
     * The last claimed code this install holds; `null` when there is none
     * (never claimed, cleared after a revoke/expiry/not-found, or reset).
     */
    var pass: WandKitAccessPass?

    /**
     * Whether the project last reported gating as on. `null` means unknown -
     * this install has never heard from the server - which the launch sequence
     * treats like "enabled" (fail closed).
     */
    var lastKnownEnabled: Boolean?
}

internal fun createAccessGateStore(
    keyValueStore: KeyValueStore,
    json: Json,
): AccessGateStore = KeyValueAccessGateStore(keyValueStore, json)

internal const val KEY_ACCESS_GATE_PASS = "wandkit.accessGate.pass"
internal const val KEY_ACCESS_GATE_LAST_KNOWN_ENABLED = "wandkit.accessGate.lastKnownEnabled"

private class KeyValueAccessGateStore(
    private val keyValueStore: KeyValueStore,
    private val json: Json,
) : AccessGateStore {
    override var pass: WandKitAccessPass?
        get() {
            val raw = keyValueStore.getString(KEY_ACCESS_GATE_PASS) ?: return null
            // A value that no longer parses can only come from a corrupted
            // store; treating it as "no pass" asks for the code again rather
            // than trusting something unreadable.
            return runCatching { json.decodeFromString<StoredAccessPass>(raw).toPass() }.getOrNull()
        }
        set(value) {
            if (value == null) {
                keyValueStore.remove(KEY_ACCESS_GATE_PASS)
            } else {
                keyValueStore.putString(KEY_ACCESS_GATE_PASS, json.encodeToString(StoredAccessPass.from(value)))
            }
        }

    // Stored as the string "true"/"false" rather than a boolean: the
    // KeyValueStore's getBoolean cannot tell "false" from "never written",
    // and absent has to mean unknown here.
    override var lastKnownEnabled: Boolean?
        get() = keyValueStore.getString(KEY_ACCESS_GATE_LAST_KNOWN_ENABLED)?.toBooleanStrictOrNull()
        set(value) {
            if (value == null) {
                keyValueStore.remove(KEY_ACCESS_GATE_LAST_KNOWN_ENABLED)
            } else {
                keyValueStore.putString(KEY_ACCESS_GATE_LAST_KNOWN_ENABLED, value.toString())
            }
        }
}

@Serializable
private data class StoredAccessPass(
    @SerialName("code")
    val code: String,
    @SerialName("claim_count")
    val claimCount: Int,
    /** ISO-8601. */
    @SerialName("claimed_at")
    val claimedAt: String,
) {
    fun toPass(): WandKitAccessPass = WandKitAccessPass(
        code = code,
        claimCount = claimCount,
        claimedAt = Instant.parse(claimedAt),
    )

    companion object {
        fun from(pass: WandKitAccessPass) = StoredAccessPass(
            code = pass.code,
            claimCount = pass.claimCount,
            claimedAt = pass.claimedAt.toString(),
        )
    }
}
