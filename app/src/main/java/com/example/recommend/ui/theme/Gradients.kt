package com.example.recommend.ui.theme
import androidx.compose.ui.graphics.Brush

// Violet → Teal: premium gradient, used on FAB, primary buttons, TrustScoreRing
val PrimaryGradient = Brush.horizontalGradient(listOf(AppViolet, AppTeal))
val PrimaryGradientVert = Brush.verticalGradient(listOf(AppViolet, AppTeal))
val PrimaryGradientLinear = Brush.linearGradient(listOf(AppViolet, AppTeal))
val DisabledGradient = Brush.linearGradient(listOf(AppDisabled, AppDisabled))
