package com.flabbergast.wandkit.core.data.forms.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormDeviceDto
import com.flabbergast.wandkit.core.platform.DeviceContext

/**
 * The wire contract wants a lowercase "android"/"ios", not the "Android"/"iOS"
 * [AppConfiguration.platformName] already sends on events - kept separate here
 * so that unrelated field is not touched.
 */
internal fun AppConfiguration.toSubmitFormDeviceDto(deviceContext: DeviceContext) = SubmitFormDeviceDto(
    platform = platformName.lowercase(),
    osVersion = deviceContext.osVersion,
    appVersion = deviceContext.appVersion,
    deviceModel = deviceContext.deviceModel,
    locale = deviceContext.locale,
)
