package com.flabbergast.wandkit.core.data.events.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.events.dto.EventRequestDeviceDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestSdkDto
import com.flabbergast.wandkit.core.data.events.dto.EventRequestUserDto
import com.flabbergast.wandkit.core.domain.events.IdentifyInfo
import com.flabbergast.wandkit.core.platform.DeviceContext

internal fun IdentifyInfo.toEventRequestUser() = EventRequestUserDto(
    externalUserId = userId,
    deviceId = deviceId,
)

internal fun AppConfiguration.toEventRequestSdk() = EventRequestSdkDto(
    platform = platformName,
    version = libraryVersion,
)

/**
 * The wire contract wants a lowercase "android"/"ios", not the "Android"/"iOS"
 * [AppConfiguration.platformName] already sends in [toEventRequestSdk] - kept
 * separate here so that unrelated field is not touched. Mirrors
 * [com.flabbergast.wandkit.core.data.forms.mappers.toSubmitFormDeviceDto].
 */
internal fun AppConfiguration.toEventRequestDevice(deviceContext: DeviceContext) = EventRequestDeviceDto(
    platform = platformName.lowercase(),
    osVersion = deviceContext.osVersion,
    appVersion = deviceContext.appVersion,
    deviceModel = deviceContext.deviceModel,
    locale = deviceContext.locale,
)