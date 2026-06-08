package com.flabbergast.wandkit.ui.compose.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
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
    textStyle: TextStyle = WandKitTypography.labelMedium,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.backgroundColor,
            contentColor = colors.textColor,
        ),
        modifier = modifier.clip(CircleShape),
    ) {
        Text(
            text = text,
            style = textStyle,
        )
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
                textColor = WandKitColors.systemGray,
                backgroundColor = WandKitColors.link,
            )

        val Secondary: WandKitButtonColors
            @Composable
            get() = WandKitButtonColors(
                textColor = WandKitColors.label,
                backgroundColor = WandKitColors.quaternaryLabel,
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
            )
            WandKitButton(
                text = "Secondary",
                onClick = {},
                colors = WandKitButtonColors.Secondary,
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
            )
            WandKitButton(
                text = "Secondary",
                onClick = {},
                colors = WandKitButtonColors.Secondary,
            )
        }
    }
}