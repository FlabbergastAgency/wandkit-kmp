package com.flabbergast.wandkit.core.domain.accessgate

/**
 * The two invite-gate calls. Failures come back as [Result.failure]: an
 * [AccessGateApiException] for a non-2xx answer, anything else for a transport
 * problem. Coroutine cancellation is rethrown, never wrapped.
 */
internal interface AccessGateRepository {
    /** The silent status check; [code] is the cached pass's code, `null` when there is none. */
    suspend fun status(code: String?): Result<AccessGateStatus>

    /** Claims [code] (as typed, trimmed) for this install. Always records a claim. */
    suspend fun claim(code: String): Result<AccessGateClaim>
}

internal data class AccessGateStatus(
    val enabled: Boolean,
    val title: String?,
    val message: String?,
    val helpUrl: String?,
    /** `null` when no code was sent. */
    val code: AccessGateCodeCheck?,
)

internal data class AccessGateCodeCheck(
    val status: AccessGateCodeStatus,
    val claimCount: Int,
)

internal enum class AccessGateCodeStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,

    /** Also every status string this SDK does not know yet. */
    NOT_FOUND,
}

internal data class AccessGateClaim(
    val code: String,
    val claimCount: Int,
)

/** A non-2xx answer. [errorCode] is the envelope's `code`, e.g. `access_code_not_found`. */
internal class AccessGateApiException(
    val statusCode: Int,
    val errorCode: String?,
) : Exception("Access gate call failed with HTTP $statusCode (${errorCode ?: "no error code"})")

/** A call that did not answer within its budget; handled exactly like a transport error. */
internal class AccessGateTimeoutException(
    operation: String,
) : Exception("Access gate $operation timed out")

internal const val ERROR_CODE_NOT_FOUND = "access_code_not_found"
internal const val ERROR_CODE_EXPIRED = "access_code_expired"
internal const val ERROR_CODE_REVOKED = "access_code_revoked"
