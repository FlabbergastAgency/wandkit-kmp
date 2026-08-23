package com.flabbergast.wandkit.core.feedback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposerPrefillTest {
    @Test
    fun defaultPrefillIsEmpty() {
        assertTrue(WandKitComposerPrefill().isEmpty)
    }

    @Test
    fun blankWhitespaceOnlyTitleIsEmpty() {
        assertTrue(WandKitComposerPrefill(title = "  \n").isEmpty)
    }

    @Test
    fun typeOnlyIsNotEmpty() {
        assertFalse(WandKitComposerPrefill(type = WandKitPostType.BUG).isEmpty)
    }

    @Test
    fun descriptionOnlyIsNotEmpty() {
        assertFalse(WandKitComposerPrefill(description = "It crashes when...").isEmpty)
    }

    @Test
    fun attachmentsOnlyIsNotEmpty() {
        val prefill = WandKitComposerPrefill(
            attachments = listOf(
                WandKitComposerAttachment(byteArrayOf(1, 2, 3), "image/png", "a.png"),
            ),
        )

        assertFalse(prefill.isEmpty)
    }

    @Test
    fun featureRequestWireValueIsSnakeCase() {
        assertEquals("feature_request", WandKitPostType.FEATURE_REQUEST.wireValue)
    }

    @Test
    fun attachmentEqualityIsContentBasedNotReferenceBased() {
        val a = WandKitComposerAttachment(byteArrayOf(1, 2, 3), "image/png", "a.png")
        val b = WandKitComposerAttachment(byteArrayOf(1, 2, 3), "image/png", "a.png")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun attachmentsWithDifferentBytesAreNotEqual() {
        val a = WandKitComposerAttachment(byteArrayOf(1, 2, 3), "image/png", "a.png")
        val b = WandKitComposerAttachment(byteArrayOf(1, 2, 4), "image/png", "a.png")

        assertFalse(a == b)
    }
}
