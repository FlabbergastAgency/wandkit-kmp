package com.flabbergast.wandkit.core.data.referrals.mappers

import kotlin.time.Instant

internal fun parseReferralInstant(value: String) = runCatching { Instant.parse(value) }.getOrNull()

internal fun parseReferralInstantOrNull(value: String?): Instant? = value?.let(::parseReferralInstant)
