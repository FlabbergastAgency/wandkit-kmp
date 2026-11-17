package com.flabbergast.wandkit.core.platform

import platform.Foundation.NSRecursiveLock

internal actual fun createReentrantLock(): ReentrantLock = FoundationRecursiveLock()

private class FoundationRecursiveLock : ReentrantLock {
    private val lock = NSRecursiveLock()

    override fun lock() = lock.lock()

    override fun unlock() = lock.unlock()
}
