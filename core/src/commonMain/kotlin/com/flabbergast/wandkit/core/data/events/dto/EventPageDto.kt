package com.flabbergast.wandkit.core.data.events.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class EventPageDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: EventPageTypeDto,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("next_button_label")
    val nextButtonLabel: String? = null,
    @SerialName("required")
    val required: Boolean = false,
    @SerialName("options")
    val options: List<EventOptionDto>? = null,
    @SerialName("allow_multiple")
    val allowMultiple: Boolean? = null,
    @SerialName("max_length")
    val maxLength: Int? = null,
    @SerialName("placeholder")
    val placeholder: String? = null,
    @SerialName("suggested_display_name")
    val suggestedDisplayName: String? = null,
    /**
     * Only decoded as a presence marker: whether this page is the form's
     * `post_creation` page (see `SDKFormPagePostCreation` in the backend's
     * openapi.yaml). The SDK doesn't render posts UI, so the contents are
     * never inspected - just whether the key is present at all, which is
     * what [com.flabbergast.wandkit.core.domain.forms.models.FeedbackFormPage.hasPostCreation]
     * needs to gate the `display_name` page's visibility. Typed as
     * [JsonElement] (rather than a concrete shape) so any future change to
     * that nested object still decodes leniently.
     */
    @SerialName("post_creation")
    val postCreation: JsonElement? = null,
    @SerialName("next")
    val next: List<EventNextRuleDto> = emptyList(),
)
