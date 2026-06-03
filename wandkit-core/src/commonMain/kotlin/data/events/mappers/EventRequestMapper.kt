package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EventRequestSdkDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo

internal fun IdentifyInfo.toEventRequestUser() = EventRequestUserDto(
    externalUserId = userId,
    deviceId = deviceId,
)

internal fun AppConfiguration.toEventRequestSdk() = EventRequestSdkDto(
    platform = platformName,
    version = libraryVersion,
)