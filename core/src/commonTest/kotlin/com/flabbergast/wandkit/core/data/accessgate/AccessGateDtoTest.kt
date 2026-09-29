package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimResponseDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApiErrorDto
import com.flabbergast.wandkit.core.data.networking.createJson
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateCodeStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AccessGateDtoTest {
    private val json = createJson()

    private fun encode(request: AccessGateStatusRequestDto): JsonObject =
        Json.parseToJsonElement(json.encodeToString(request)).jsonObject

    @Test
    fun statusRequestOmitsCodeWhenThereIsNoPass() {
        val body = encode(AccessGateStatusRequestDto(installId = "install-1"))

        assertEquals("install-1", body["install_id"]?.jsonPrimitive?.content)
        assertFalse("code" in body)
    }

    @Test
    fun statusRequestCarriesTheCachedCode() {
        val body = encode(AccessGateStatusRequestDto(installId = "install-1", code = "K7QM-2XFT"))

        assertEquals("K7QM-2XFT", body["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun claimRequestUsesSnakeCase() {
        val body = Json.parseToJsonElement(
            json.encodeToString(
                AccessGateClaimRequestDto(installId = "install-1", code = "k7qm 2xft", platform = "android", sdkVersion = "android-0.1.8"),
            ),
        ).jsonObject

        assertEquals(setOf("install_id", "code", "platform", "sdk_version"), body.keys)
        assertEquals("k7qm 2xft", body["code"]?.jsonPrimitive?.content)
        assertEquals("android-0.1.8", body["sdk_version"]?.jsonPrimitive?.content)
    }

    @Test
    fun statusResponseWithNullFieldsDecodes() {
        val response = json.decodeFromString<AccessGateStatusResponseDto>(
            """{"enabled":false,"title":null,"message":null,"help_url":null,"code":null}""",
        )

        assertEquals(AccessGateStatusResponseDto(enabled = false), response)
    }

    @Test
    fun statusResponseWithMissingOptionalFieldsDecodes() {
        val response = json.decodeFromString<AccessGateStatusResponseDto>("""{"enabled":true}""")

        assertEquals(true, response.enabled)
        assertNull(response.code)
    }

    @Test
    fun statusResponseDecodesCopyAndCodeStatus() {
        val status = json.decodeFromString<AccessGateStatusResponseDto>(
            """{"enabled":true,"title":"Beta","message":"  ","help_url":"https://example.com/request",
               "code":{"status":"revoked","claim_count":3}}""",
        ).toAccessGateStatus()

        assertEquals("Beta", status.title)
        assertNull(status.message, "blank copy falls back to the SDK default")
        assertEquals("https://example.com/request", status.helpUrl)
        assertEquals(AccessGateCodeStatus.REVOKED, status.code?.status)
        assertEquals(3, status.code?.claimCount)
    }

    @Test
    fun everyKnownCodeStatusMaps() {
        val expected = mapOf(
            "active" to AccessGateCodeStatus.ACTIVE,
            "expired" to AccessGateCodeStatus.EXPIRED,
            "revoked" to AccessGateCodeStatus.REVOKED,
            "not_found" to AccessGateCodeStatus.NOT_FOUND,
        )
        expected.forEach { (raw, status) ->
            val response = json.decodeFromString<AccessGateStatusResponseDto>(
                """{"enabled":true,"code":{"status":"$raw","claim_count":1}}""",
            )
            assertEquals(status, response.toAccessGateStatus().code?.status, raw)
        }
    }

    @Test
    fun unknownCodeStatusDecodes_andFailsClosedAsNotFound() {
        val response = json.decodeFromString<AccessGateStatusResponseDto>(
            """{"enabled":true,"code":{"status":"suspended","claim_count":1}}""",
        )

        assertEquals(AccessGateCodeStatus.NOT_FOUND, response.toAccessGateStatus().code?.status)
    }

    @Test
    fun unknownExtraFieldsAreIgnored() {
        val response = json.decodeFromString<AccessGateStatusResponseDto>(
            """{"enabled":true,"new_field":{"x":1},"code":{"status":"active","claim_count":2,"last_claimed_at":"2026-01-01T00:00:00Z"}}""",
        )

        assertEquals(AccessGateCodeStatus.ACTIVE, response.toAccessGateStatus().code?.status)
    }

    @Test
    fun claimResponseDecodes() {
        val response = json.decodeFromString<AccessGateClaimResponseDto>(
            """{"code":"K7QM-2XFT","claim_count":4,"expires_at":null,"extra":true}""",
        )

        assertEquals(AccessGateClaimResponseDto(code = "K7QM-2XFT", claimCount = 4, expiresAt = null), response)
    }

    @Test
    fun errorEnvelopeDecodes() {
        val error = json.decodeFromString<WandKitApiErrorDto>(
            """{"error":"Not Found","code":"access_code_not_found","message":"access code not found"}""",
        )

        assertEquals(WandKitApiErrorDto("Not Found", "access_code_not_found", "access code not found"), error)
    }
}
