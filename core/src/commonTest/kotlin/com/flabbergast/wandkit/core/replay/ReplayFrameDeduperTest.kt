package com.flabbergast.wandkit.core.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayFrameDeduperTest {
    @Test
    fun hashMatchesTheFnv1a64ReferenceVectors() {
        assertEquals(0xCBF29CE484222325uL, ReplayFrameDeduper.hash(ByteArray(0)))
        assertEquals(0xAF63DC4C8601EC8CuL, ReplayFrameDeduper.hash("a".encodeToByteArray()))
    }

    @Test
    fun identicalConsecutiveGridsAreSkipped() {
        val deduper = ReplayFrameDeduper()
        val grid = ByteArray(256) { it.toByte() }

        assertTrue(deduper.shouldKeep(ReplayFrameDeduper.hash(grid)))
        assertFalse(deduper.shouldKeep(ReplayFrameDeduper.hash(grid)))
        assertFalse(deduper.shouldKeep(ReplayFrameDeduper.hash(grid.copyOf())))
    }

    @Test
    fun aChangedGridIsKeptAndBecomesTheNewReference() {
        val deduper = ReplayFrameDeduper()
        val first = ReplayFrameDeduper.hash(ByteArray(256))
        val second = ReplayFrameDeduper.hash(ByteArray(256) { 1 })

        assertTrue(deduper.shouldKeep(first))
        assertTrue(deduper.shouldKeep(second))
        assertTrue(deduper.shouldKeep(first))
    }

    @Test
    fun resetForgetsTheLastHash() {
        val deduper = ReplayFrameDeduper()
        val hash = ReplayFrameDeduper.hash(ByteArray(256))
        deduper.shouldKeep(hash)

        deduper.reset()

        assertNull(deduper.lastHash)
        assertTrue(deduper.shouldKeep(hash))
    }
}
