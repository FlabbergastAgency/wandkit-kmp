package com.flabbergast.wandkit.core.data.networking

import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger

internal actual fun platformHttpLogger() = Logger.DEFAULT