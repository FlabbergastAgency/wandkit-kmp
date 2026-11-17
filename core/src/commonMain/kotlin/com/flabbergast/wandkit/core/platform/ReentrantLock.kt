package com.flabbergast.wandkit.core.platform

/**
 * A reentrant lock, for the few places that must change more than one value
 * as one step while other threads read them (the device id and the user it
 * is linked to, see `InstallIdentity`).
 */
internal interface ReentrantLock {
    fun lock()

    fun unlock()
}

internal expect fun createReentrantLock(): ReentrantLock

internal inline fun <T> ReentrantLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
