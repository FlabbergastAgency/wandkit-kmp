package com.flabbergast.wandkit.core.replay

/**
 * Platform-neutral half of frame dedupe: given a hash of a small grayscale
 * grid the platform side renders the frame down to, decides whether the frame
 * differs from the last accepted one. A timer tick on an otherwise static
 * screen hashes identically and is skipped, so a quiet screen costs nothing.
 * Same algorithm (FNV-1a 64 over a 16x16 grid) as the iOS SDK.
 */
internal class ReplayFrameDeduper {
    var lastHash: ULong? = null
        private set

    /**
     * `true` when [gridHash] differs from the last accepted hash (or there is
     * none yet) - and records it as the new last-accepted hash in that case.
     * Consecutive identical grids return `false` without recording anything.
     */
    fun shouldKeep(gridHash: ULong): Boolean {
        if (gridHash == lastHash) return false
        lastHash = gridHash
        return true
    }

    fun reset() {
        lastHash = null
    }

    companion object {
        private const val FNV_OFFSET_BASIS: ULong = 0xCBF29CE484222325uL
        private const val FNV_PRIME: ULong = 0x100000001B3uL

        /** FNV-1a 64 over [grid] (e.g. a 16x16 = 256 byte grayscale downscale). */
        fun hash(grid: ByteArray): ULong {
            var hash = FNV_OFFSET_BASIS
            for (byte in grid) {
                hash = hash xor byte.toUByte().toULong()
                hash *= FNV_PRIME
            }
            return hash
        }
    }
}
