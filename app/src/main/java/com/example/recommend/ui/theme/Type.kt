package com.example.recommend.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.recommend.R

val HeadingFontFamily = FontFamily(
    Font(R.font.syne_variable, FontWeight.Normal),
    Font(R.font.syne_variable, FontWeight.Medium),
    Font(R.font.syne_variable, FontWeight.SemiBold),
    Font(R.font.syne_variable, FontWeight.Bold),
    Font(R.font.syne_variable, FontWeight.Black)
)

val BodyFontFamily = FontFamily(
    Font(R.font.dm_sans_variable, FontWeight.Normal),
    Font(R.font.dm_sans_variable, FontWeight.Medium),
    Font(R.font.dm_sans_variable, FontWeight.SemiBold),
    Font(R.font.dm_sans_variable, FontWeight.Bold)
)

object AppTextStyles {
    val Heading1 = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
        color = DarkPastelAnthracite
    )

    val Heading2 = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.35).sp,
        color = DarkPastelAnthracite
    )

    val BodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = DarkPastelAnthracite
    )

    val BodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp,
        color = DarkPastelAnthracite
    )
}

val AppTypography = Typography(
    displayLarge = AppTextStyles.Heading1,
    headlineLarge = AppTextStyles.Heading1,
    headlineMedium = AppTextStyles.Heading2,
    headlineSmall = AppTextStyles.Heading2.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = AppTextStyles.Heading2,
    titleMedium = AppTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = AppTextStyles.BodySmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = AppTextStyles.BodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = AppTextStyles.BodyMedium,
    bodySmall = AppTextStyles.BodySmall,
    labelLarge = AppTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = AppTextStyles.BodySmall,
    labelSmall = AppTextStyles.BodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp)
)
