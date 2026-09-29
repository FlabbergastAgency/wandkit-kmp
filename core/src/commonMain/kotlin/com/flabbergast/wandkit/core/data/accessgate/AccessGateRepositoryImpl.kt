package com.flabbergast.wandkit.core.data.accessgate

import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateClaimResponseDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateCodeStatusDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusRequestDto
import com.flabbergast.wandkit.core.data.accessgate.dto.AccessGateStatusResponseDto
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.networking.WandKitHttpException
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateApiException
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateClaim
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateCodeCheck
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateCodeStatus
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateRepository
import com.flabbergast.wandkit.core.domain.accessgate.AccessGateStatus
import com.flabbergast.wandkit.core.domain.install.InstallIdentity
import kotlinx.coroutines.CancellationException

internal fun createAccessGateRepository(
    accessGateApi: WandKitApi<AccessGateApi>,
    installIdentity: InstallIdentity,
    platform: String,
    sdkVersion: String,
): AccessGateRepository = AccessGateRepositoryImpl(
    accessGateApi = accessGateApi,
    installIdentity = installIdentity,
    platform = platform,
    sdkVersion = sdkVersion,
)

private class AccessGateRepositoryImpl(
    private val accessGateApi: WandKitApi<AccessGateApi>,
    private val installIdentity: InstallIdentity,
    private val platform: String,
    private val sdkVersion: String,
) : AccessGateRepository {
    override suspend fun status(code: String?): Result<AccessGateStatus> =
        accessGateApi.invokeReadingErrorBody {
            status(AccessGateStatusRequestDto(installId = installIdentity.installId, code = code))
        }
            .toAccessGateResult()
            .map { it.data.toAccessGateStatus() }

    override suspend fun claim(code: String): Result<AccessGateClaim> =
        accessGateApi.invokeReadingErrorBody {
            claim(
                AccessGateClaimRequestDto(
                    installId = installIdentity.installId,
                    code = code,
                    platform = platform,
                    sdkVersion = sdkVersion,
                ),
            )
        }
            .toAccessGateResult()
            .map { it.data.toAccessGateClaim() }
}

/**
 * [WandKitApi] runs the call inside `runCatching`, which also catches a
 * cancelled coroutine's [CancellationException]; rethrow it so cancellation
 * (a reset, a timeout) keeps propagating instead of reading as "offline".
 */
private fun <T> Result<T>.toAccessGateResult(): Result<T> {
    val error = exceptionOrNull() ?: return this
    if (error is CancellationException) throw error
    if (error is WandKitHttpException) {
        return Result.failure(AccessGateApiException(error.statusCode, error.errorBody?.code))
    }
    return this
}

internal fun AccessGateStatusResponseDto.toAccessGateStatus(): AccessGateStatus = AccessGateStatus(
    enabled = enabled,
    title = title.nonBlankOrNull(),
    message = message.nonBlankOrNull(),
    helpUrl = helpUrl.nonBlankOrNull(),
    code = code?.toAccessGateCodeCheck(),
)

internal fun AccessGateCodeStatusDto.toAccessGateCodeCheck(): AccessGateCodeCheck = AccessGateCodeCheck(
    status = when (status) {
        "active" -> AccessGateCodeStatus.ACTIVE
        "expired" -> AccessGateCodeStatus.EXPIRED
        "revoked" -> AccessGateCodeStatus.REVOKED
        // "not_found" and anything this SDK does not know yet fail closed alike.
        else -> AccessGateCodeStatus.NOT_FOUND
    },
    claimCount = claimCount,
)

private fun AccessGateClaimResponseDto.toAccessGateClaim(): AccessGateClaim = AccessGateClaim(
    code = code,
    claimCount = claimCount,
)

private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
