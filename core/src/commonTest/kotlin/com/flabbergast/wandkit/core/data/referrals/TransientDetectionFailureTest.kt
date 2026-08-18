package com.flabbergast.wandkit.core.data.referrals

import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransientDetectionFailureTest {
    @Test
    fun notFoundIsTransient() {
        // Detect answers "no referral" with 204, so a 404 can only mean the
        // endpoint is not there - deploy skew, or a baseURL that does not serve
        // it. A later launch may well get further.
        assertTrue(isTransientDetectionFailure(WandKitHttpException(404)))
    }

    @Test
    fun serverAndThrottlingFailuresAreTransient() {
        assertTrue(isTransientDetectionFailure(WandKitHttpException(408)))
        assertTrue(isTransientDetectionFailure(WandKitHttpException(429)))
        assertTrue(isTransientDetectionFailure(WandKitHttpException(500)))
        assertTrue(isTransientDetectionFailure(WandKitHttpException(503)))
    }

    @Test
    fun rejectedRequestsArePermanent() {
        // These fail the same way every launch, so retrying only repeats the
        // fingerprint POST for the life of the install.
        assertFalse(isTransientDetectionFailure(WandKitHttpException(400)))
        assertFalse(isTransientDetectionFailure(WandKitHttpException(401)))
        assertFalse(isTransientDetectionFailure(WandKitHttpException(403)))
    }

    @Test
    fun unreadableBodyIsPermanent() {
        assertFalse(isTransientDetectionFailure(SerializationException("bad body")))
    }

    @Test
    fun unknownFailuresAreTransient() {
        // Connectivity problems arrive as arbitrary IO throwables; assuming
        // transient is the safe default, and the retry ceiling bounds it.
        assertTrue(isTransientDetectionFailure(RuntimeException("connection reset")))
    }
}
