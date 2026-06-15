package com.flabbergast.wandkit.core.data.referrals.mappers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun Map<String, String>.toReferralRequestProperties(): Map<String, JsonElement> =
    mapValues { (_, value) -> JsonPrimitive(value) }

internal fun Map<String, JsonElement>?.toReferralMatchProperties(json: Json): Map<String, String> =
    this?.mapValues { (_, value) -> value.toReferralPropertyString(json) } ?: emptyMap()

private fun JsonElement.toReferralPropertyString(json: Json): String =
    when (this) {
        JsonNull -> "null"
        is JsonPrimitive -> content
        is JsonObject, is JsonArray -> json.encodeToString(JsonElement.serializer(), this)
    }
