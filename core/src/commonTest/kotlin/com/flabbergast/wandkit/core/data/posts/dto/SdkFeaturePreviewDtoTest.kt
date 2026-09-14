package com.flabbergast.wandkit.core.data.posts.dto

import com.flabbergast.wandkit.core.data.networking.createJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [SdkFeaturePreviewDto] decodes the feature-preview endpoint's response.
 * Confirms the shared `Json` config (`ignoreUnknownKeys`) lets it decode a
 * payload with fields it doesn't model, and that the nested `copy` object and
 * a `null`/absent `store_url` decode as expected.
 */
class SdkFeaturePreviewDtoTest {
    @Test
    fun decodesComingSoonBodyAndIgnoresUnknownFields() {
        val json = createJson()

        val dto = json.decodeFromString(
            SdkFeaturePreviewDto.serializer(),
            """
            {
              "state": "coming_soon",
              "post_id": "post-1",
              "post_status": "planned",
              "viewer_voted": false,
              "copy": {
                "title": "FAA support is almost here",
                "message": "Coming soon.",
                "primary_label": "Let me know when FAA is ready",
                "secondary_label": "Continue with EASA"
              },
              "store_url": null,
              "some_future_field": "ignored"
            }
            """.trimIndent(),
        )

        assertEquals("coming_soon", dto.state)
        assertEquals("post-1", dto.postId)
        assertEquals("planned", dto.postStatus)
        assertFalse(dto.viewerVoted)
        assertEquals("FAA support is almost here", dto.previewCopy.title)
        assertEquals("Coming soon.", dto.previewCopy.message)
        assertEquals("Let me know when FAA is ready", dto.previewCopy.primaryLabel)
        assertEquals("Continue with EASA", dto.previewCopy.secondaryLabel)
        assertNull(dto.storeUrl)
    }

    @Test
    fun decodesAvailableBodyWithStoreUrlAndViewerVoted() {
        val json = createJson()

        val dto = json.decodeFromString(
            SdkFeaturePreviewDto.serializer(),
            """
            {
              "state": "available",
              "post_id": "post-1",
              "post_status": "done",
              "viewer_voted": true,
              "copy": {
                "title": "It's here",
                "message": "Update the app to use this feature.",
                "primary_label": "Update app",
                "secondary_label": "Continue with EASA"
              },
              "store_url": "https://play.google.com/store/apps/details?id=com.example"
            }
            """.trimIndent(),
        )

        assertEquals("available", dto.state)
        assertTrue(dto.viewerVoted)
        assertEquals("https://play.google.com/store/apps/details?id=com.example", dto.storeUrl)
    }

    @Test
    fun decodesMinimalPayloadWithDefaults() {
        val json = createJson()

        val dto = json.decodeFromString(
            SdkFeaturePreviewDto.serializer(),
            """
            {
              "state": "coming_soon",
              "post_id": "post-1",
              "post_status": "planned",
              "copy": {
                "title": "Almost here",
                "message": "Coming soon.",
                "primary_label": "Let me know",
                "secondary_label": "Not now"
              }
            }
            """.trimIndent(),
        )

        assertFalse(dto.viewerVoted)
        assertNull(dto.storeUrl)
    }
}
