package com.example.recommend.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val TrustListScheme = lightColorScheme(
    primary = Color(0xFF3BD4C0),
    onPrimary = Color(0xFF1A2A24),
    primaryContainer = Color(0xFFF0EEEB),
    onPrimaryContainer = Color(0xFF1A2A24),
    secondary = Color(0xFF7C6FE0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0EEEB),
    onSecondaryContainer = Color(0xFF1A2A24),
    tertiary = Color(0xFFD4AF37),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F7F4),
    onBackground = Color(0xFF1A2A24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2A24),
    surfaceVariant = Color(0xFFF0EEEB),
    onSurfaceVariant = Color(0xFF8A9A95),
    outline = Color(0x263BD4C0),
    outlineVariant = Color(0xFFEBE9E6),
)

@Composable
fun RecommendTheme(
    content: @Composable () -> Unit
) {
    // ── Adaptive density ──────────────────────────────────────────────────────
    // Design baseline: 375dp (matches HTML phone frame).
    // On wider screens (e.g. Pixel 7 at 393dp) scale ≈ 1.05 — elements grow
    // proportionally to fill the space, removing excess whitespace.
    // On narrower screens (e.g. 360dp) scale ≈ 0.96 — everything compresses slightly.
    // Range is clamped to [0.85, 1.25] to prevent extreme distortion.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = (screenWidthDp / DESIGN_WIDTH_DP).coerceIn(0.85f, 1.25f)

    val baseDensity = LocalDensity.current
    val adaptedDensity = Density(
        density   = baseDensity.density   * scale,
        fontScale = baseDensity.fontScale * scale
    )

    CompositionLocalProvider(
        LocalDensity   provides adaptedDensity,
        LocalAppScale  provides scale
    ) {
        MaterialTheme(
            colorScheme = TrustListScheme,
            typography  = AppTypography,
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
}
