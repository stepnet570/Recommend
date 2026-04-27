package com.example.recommend.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design baseline width (matches HTML phone frame: 375px).
 * Scale = actualScreenWidthDp / DESIGN_WIDTH_DP.
 * Example: Pixel 7 (393dp) → scale ≈ 1.048 (elements are ~5% larger → less air).
 */
const val DESIGN_WIDTH_DP = 375f

/** Provided by [RecommendTheme]. Use [LocalAppScale.current] to read. */
val LocalAppScale = compositionLocalOf { 1f }

// ── Convenient scaled-dp / scaled-sp extensions ──────────────────────────────
// Usage: padding(horizontal = 20.sdp), fontSize = 14.ssp

val Int.sdp: Dp
    @Composable get() = (this * LocalAppScale.current).dp

val Float.sdp: Dp
    @Composable get() = (this * LocalAppScale.current).dp

val Int.ssp: TextUnit
    @Composable get() = (this * LocalAppScale.current).sp

val Float.ssp: TextUnit
    @Composable get() = (this * LocalAppScale.current).sp
