package com.flabbergast.wandkit.ui.compose.feedbackForm

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.flabbergast.wandkit.core.components.formPage.FormPageComponent
import com.flabbergast.wandkit.core.components.formPage.model.FormPageUiState
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun FormPageView(
    component: FormPageComponent,
) {
    val state by component.viewState.subscribeAsState()

    val page = state.page ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = component::dismissForm,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WandKitColors.onSurface,
                ),
            ) {
                Text("Close")
            }
        }

        page.imageUrl?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WandKitColors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Image",
                    style = WandKitTypography.bodyMedium,
                    color = WandKitColors.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = page.title,
                style = WandKitTypography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            page.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = WandKitTypography.bodyLarge,
                    color = WandKitColors.onSurfaceVariant,
                )
            }
        }

        FormPageContent(
            page = page,
            component = component,
        )

        Button(
            onClick = component::submitPage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = WandKitColors.primary,
                contentColor = WandKitColors.onPrimary,
            ),
        ) {
            Text(page.nextButtonLabel ?: defaultButtonLabel(page.content))
        }
    }
}

@Composable
private fun FormPageContent(
    page: FormPageUiState,
    component: FormPageComponent,
) {
    when (val content = page.content) {
        is FormPageUiState.Content.End -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WandKitColors.surfaceVariant,
                contentColor = WandKitColors.onSurfaceVariant,
            ) {
                Text(
                    text = page.subtitle ?: "Thanks for sharing your feedback.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    style = WandKitTypography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is FormPageUiState.Content.MultiChoice -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content.choices.forEach { choice ->
                    ChoiceRow(
                        label = choice.label,
                        selected = choice.isSelected,
                        onClick = { component.updateMultiChoice(choice.id) },
                    )
                }
            }
        }

        is FormPageUiState.Content.Stars -> {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                (1..content.totalStars).forEach { rating ->
                    WandKitFilterChip(
                        selected = content.selectedStars == rating,
                        onClick = { component.updateStars(rating) },
                        label = { Text(rating.toString()) },
                    )
                }
            }
        }

        is FormPageUiState.Content.Text -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WandKitOutlinedTextField(
                    value = content.text,
                    onValueChange = { component.updateText(it.take(content.maxLength)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text(content.placeholder) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Text(
                    text = "${content.text.length}/${content.maxLength}",
                    modifier = Modifier.fillMaxWidth(),
                    style = WandKitTypography.bodySmall,
                    color = WandKitColors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }

        is FormPageUiState.Content.Thumbs -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChoiceCard(
                    label = "Thumbs up",
                    selected = content.isThumbsUp == true,
                    modifier = Modifier.weight(1f),
                    onClick = { component.updateThumbs(true) },
                )
                ChoiceCard(
                    label = "Thumbs down",
                    selected = content.isThumbsUp == false,
                    modifier = Modifier.weight(1f),
                    onClick = { component.updateThumbs(false) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ChoiceCard(
        label = label,
        selected = selected,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    )
}

@Composable
private fun ChoiceCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        WandKitColors.secondaryContainer
    } else {
        WandKitColors.surfaceVariant
    }
    val contentColor = if (selected) {
        WandKitColors.onSecondaryContainer
    } else {
        WandKitColors.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (selected) contentColor else contentColor.copy(alpha = 0.25f)),
            )
            Text(
                text = label,
                style = WandKitTypography.titleMedium,
                color = contentColor,
            )
        }
    }
}

private fun defaultButtonLabel(content: FormPageUiState.Content): String = when (content) {
    is FormPageUiState.Content.End -> "Done"
    else -> "Continue"
}

@Composable
private fun WandKitFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = WandKitColors.surfaceVariant,
            labelColor = WandKitColors.onSurfaceVariant,
            selectedContainerColor = WandKitColors.secondaryContainer,
            selectedLabelColor = WandKitColors.onSecondaryContainer,
        ),
    )
}

@Composable
private fun WandKitOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        minLines = minLines,
        maxLines = maxLines,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WandKitColors.primary,
            unfocusedBorderColor = WandKitColors.outline,
            focusedTextColor = WandKitColors.onSurface,
            unfocusedTextColor = WandKitColors.onSurface,
            cursorColor = WandKitColors.primary,
            focusedPlaceholderColor = WandKitColors.onSurfaceVariant,
            unfocusedPlaceholderColor = WandKitColors.onSurfaceVariant,
        ),
    )
}
