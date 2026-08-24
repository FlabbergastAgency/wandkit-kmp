package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventRequestSdkDto(
    @SerialName("platform")
    val platform: String,
    @SerialName("version")
    val version: String,
    /**
     * The feedback-form page types this SDK build can render. The server
     * splices out any page of another type (re-wiring `next` rules around
     * it) so this client never meets a page type it cannot decode - see
     * `SDKClientInfo.supported_page_types` in the backend's openapi.yaml.
     * Defaults to empty only so call sites that don't care (e.g. tests) stay
     * terse; production always supplies [EVENT_REQUEST_SUPPORTED_PAGE_TYPES]
     * via [com.flabbergast.wandkit.core.data.events.mappers.toEventRequestSdk].
     */
    @SerialName("supported_page_types")
    val supportedPageTypes: List<String> = emptyList(),
)

/**
 * The feedback-form page types this SDK build can render, in the wire's
 * `type` spelling. Deliberately excludes `push_permission`: KMP has no
 * permission-prompt UI, so listing it would make the server splice in pages
 * this client cannot render.
 */
internal val EVENT_REQUEST_SUPPORTED_PAGE_TYPES: List<String> = listOf(
    "thumbs",
    "stars",
    "multi_choice",
    "text",
    "end",
    "display_name",
)
