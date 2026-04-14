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
    primary = Color(0xFF3BD4C0),          // Teal
    onPrimary = Color(0xFF1A2A24),
    primaryContainer = Color(0xFFF0EEEB), // warm neutral
    onPrimaryContainer = Color(0xFF1A2A24),
    secondary = Color(0xFF7C6FE0),         // Violet — new accent
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0EEEB),
    onSecondaryContainer = Color(0xFF1A2A24),
    tertiary = Color(0xFFD4AF37),          // Gold stays for monetization
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F7F4),        // warm off-white
    onBackground = Color(0xFF1A2A24),
    surface = Color(0xFFFFFFFF),           // white cards
    onSurface = Color(0xFF1A2A24),
    surfaceVariant = Color(0xFFF0EEEB),    // warm neutral sections
    onSurfaceVariant = Color(0xFF8A9A95),
    outline = Color(0x263BD4C0),
    outlineVariant = Color(0xFFEBE9E6),
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
                    .background(AppBackground)
            ) {
                content()
            }
        }
    )
}
