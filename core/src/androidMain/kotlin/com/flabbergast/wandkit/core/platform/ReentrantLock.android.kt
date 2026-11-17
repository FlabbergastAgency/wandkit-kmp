package com.flabbergast.wandkit.core.platform

internal actual fun createReentrantLock(): ReentrantLock = JavaReentrantLock()

private class JavaReentrantLock : ReentrantLock {
    private val lock = java.util.concurrent.locks.ReentrantLock()

    override fun lock() = lock.lock()

    override fun unlock() = lock.unlock()
}
