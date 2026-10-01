package com.flabbergast.wandkit.core.platform

import java.security.MessageDigest

internal actual fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
