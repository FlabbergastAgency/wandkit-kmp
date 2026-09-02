package com.flabbergast.wandkit.ui.compose.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.WandKitThemeDefaults
import com.flabbergast.wandkit.ui.compose.WandKitThemeProvider
import com.flabbergast.wandkit.ui.compose.WandKitTypography

@Composable
internal fun WandKitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: WandKitButtonColors = WandKitButtonColors.Primary,
    textStyle: TextStyle = WandKitTypography.modalSubtitle.copy(fontWeight = FontWeight.SemiBold),
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.backgroundColor,
            contentColor = colors.textColor,
            disabledContainerColor = colors.backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = colors.textColor.copy(alpha = 0.5f),
        ),
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = colors.textColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = textStyle,
            )
        }
    }
}

internal data class WandKitButtonColors(
    val textColor: Color,
    val backgroundColor: Color,
) {
    internal companion object {
        val Primary: WandKitButtonColors
            @Composable
            get() = WandKitButtonColors(
                textColor = WandKitColors.modalPrimaryButtonContent,
                backgroundColor = WandKitColors.modalPrimaryButtonBackground,
            )

        val Secondary: WandKitButtonColors
            @Composable
            get() = WandKitButtonColors(
                textColor = WandKitColors.label,
                backgroundColor = WandKitColors.modalSecondaryButtonBackground,
            )
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonPreviewLight() {
    WandKitThemeProvider {
        Column {
            WandKitButton(
                text = "Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            WandKitButton(
                text = "Secondary",
                onClick = {},
                colors = WandKitButtonColors.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun ButtonPreviewDark() {
    WandKitThemeProvider(theme = WandKitThemeDefaults.dark()) {
        Column {
            WandKitButton(
                text = "Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            WandKitButton(
                text = "Secondary",
                onClick = {},
                colors = WandKitButtonColors.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
