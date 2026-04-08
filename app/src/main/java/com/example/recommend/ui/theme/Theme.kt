package com.example.recommend.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val TrustListScheme = lightColorScheme(
    primary = Color(0xFF3BD4C0),
    onPrimary = Color(0xFF1A2A24),
    primaryContainer = Color(0xFFE8F5F0),
    onPrimaryContainer = Color(0xFF1A2A24),
    secondary = Color(0xFF6B8C80),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F5F0),
    onSecondaryContainer = Color(0xFF1A2A24),
    tertiary = Color(0xFFD4AF37),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A2A24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2A24),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF6B8C80),
    outline = Color(0x263BD4C0),
    outlineVariant = Color(0xFFE8F0EC),
)

@Composable
fun RecommendTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TrustListScheme,
        typography = AppTypography,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                content()
            }
        }
    )
}
