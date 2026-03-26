package com.example.recommend.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val MonochromeScheme = lightColorScheme(
    primary = RichPastelCoral,
    onPrimary = Color.White,
    primaryContainer = SurfaceMuted,
    onPrimaryContainer = DarkPastelAnthracite,
    secondary = MutedPastelTeal,
    onSecondary = Color.White,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = DarkPastelAnthracite,
    tertiary = MutedPastelGold,
    onTertiary = Color.White,
    tertiaryContainer = SurfaceMuted,
    onTertiaryContainer = DarkPastelAnthracite,
    background = Color.White,
    onBackground = DarkPastelAnthracite,
    surface = SurfacePastel,
    onSurface = DarkPastelAnthracite,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = DarkPastelAnthracite.copy(alpha = 0.78f),
    outline = MutedPastelTeal.copy(alpha = 0.45f),
    outlineVariant = SurfaceMuted
)

@Composable
fun RecommendTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonochromeScheme,
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
