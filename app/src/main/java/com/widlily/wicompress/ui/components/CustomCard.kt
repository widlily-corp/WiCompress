package com.widlily.wicompress.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.widlily.wicompress.ui.theme.Shapes

@Composable
fun CustomCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardShape = Shapes.medium // 24dp rounded corners
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), cardShape)
            .then(clickModifier)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart,
        content = content
    )
}

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val cardShape = Shapes.medium
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(gradient)
            .border(BorderStroke(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.1f)), cardShape)
            .then(clickModifier)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart,
        content = content
    )
}
