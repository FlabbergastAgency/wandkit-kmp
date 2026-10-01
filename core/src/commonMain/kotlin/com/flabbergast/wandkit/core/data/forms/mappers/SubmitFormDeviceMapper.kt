package com.flabbergast.wandkit.core.data.forms.mappers

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.forms.dto.SubmitFormDeviceDto
import com.flabbergast.wandkit.core.platform.DeviceContext

internal fun AppConfiguration.toSubmitFormDeviceDto(deviceContext: DeviceContext) = SubmitFormDeviceDto(
    platform = platform,
    osVersion = deviceContext.osVersion,
    appVersion = deviceContext.appVersion,
    deviceModel = deviceContext.deviceModel,
    locale = deviceContext.locale,
    appIdentifier = deviceContext.appIdentifier,
)
