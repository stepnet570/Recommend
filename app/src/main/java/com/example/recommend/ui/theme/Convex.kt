package com.example.recommend.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Light top → deeper bottom: reads as a raised tile on white. */
fun convexCardGradient(): Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF2F2F2),
        Color(0xFFE4E4E4),
        Color(0xFFD8D8D8)
    ),
    start = Offset(0f, 0f),
    end = Offset(720f, 1100f)
)

fun Modifier.convexCardStyle(
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 20.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color.White.copy(alpha = 0.85f),
        ambientColor = Color.Black.copy(alpha = 0.22f)
    )
    .clip(shape)
    .background(convexCardGradient(), shape)
    .border(1.5.dp, Color(0xFFCFCFCF), shape)

/**
 * Raised convex panel — use for any block content (Column, Row, etc.).
 */
@Composable
fun ConvexCardBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.convexCardStyle(shape = shape, elevation = elevation)) {
        content()
    }
}

/**
 * Convex card with [Column] content.
 */
@Composable
fun ConvexCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ConvexCardBox(modifier, shape, elevation) {
        Column(content = content)
    }
}
