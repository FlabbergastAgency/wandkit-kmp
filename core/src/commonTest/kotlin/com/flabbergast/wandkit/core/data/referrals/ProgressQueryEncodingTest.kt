package com.flabbergast.wandkit.core.data.referrals

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The progress call passes a customer's own user id straight through as a query
 * value, so reserved characters in it must not be able to alter the URL.
 */
class ProgressQueryEncodingTest {
    @Test
    fun reservedCharactersInQueryValuesAreEncoded() {
        val builder = HttpRequestBuilder()
        builder.url("https://api.example.com/api/v1/referrals/progress")
        builder.parameter("campaign", "summer&winter")
        builder.parameter("user_id", "user 1+2/3?x=y#z")

        val url = Url(builder.url.buildString())

        assertEquals("summer&winter", url.parameters["campaign"])
        assertEquals("user 1+2/3?x=y#z", url.parameters["user_id"])
    }
}
