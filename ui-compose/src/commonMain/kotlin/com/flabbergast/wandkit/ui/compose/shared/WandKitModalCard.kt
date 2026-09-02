package com.flabbergast.wandkit.ui.compose.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.flabbergast.wandkit.ui.compose.Res
import com.flabbergast.wandkit.ui.compose.WandKitColors
import com.flabbergast.wandkit.ui.compose.ic_close
import org.jetbrains.compose.resources.painterResource

internal val WandKitModalShape = RoundedCornerShape(44.dp)
internal val WandKitModalContentFillShape = RoundedCornerShape(24.dp)

@Composable
internal fun WandKitModalScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WandKitColors.modalScrim)
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = onDismiss,
            ),
    )
}

@Composable
internal fun WandKitModalCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .widthIn(max = 362.dp)
            .clip(WandKitModalShape),
        shape = WandKitModalShape,
        color = WandKitColors.modalBackground,
        contentColor = WandKitColors.label,
    ) {
        Box(content = content)
    }
}

@Composable
internal fun WandKitModalCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(WandKitColors.modalCloseButtonBackground, CircleShape)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Close",
            tint = WandKitColors.label,
            modifier = Modifier.size(24.dp),
        )
    }
}
