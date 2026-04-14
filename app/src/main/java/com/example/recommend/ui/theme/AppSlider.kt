package com.example.recommend.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Слайдер с градиентным активным треком (AppLime → AppTeal).
 * Thumb — AppTeal. Неактивный трек — SurfaceMuted.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    Box(modifier = modifier) {
        // Градиентный трек под слайдером
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
        ) {
            val trackHeight = size.height
            val radius = CornerRadius(trackHeight / 2, trackHeight / 2)

            // Неактивный трек (весь)
            drawRoundRect(
                color = SurfaceMuted,
                size = size,
                cornerRadius = radius
            )

            // Активный трек — градиент от старта до позиции thumb
            val activeWidth = size.width * fraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    brush = PrimaryGradient,
                    topLeft = Offset.Zero,
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = radius
                )
            }

            // Thumb circle
            val thumbRadius = 10.dp.toPx()
            val thumbX = (size.width * fraction).coerceIn(thumbRadius, size.width - thumbRadius)
            drawCircle(
                color = AppTeal,
                radius = thumbRadius,
                center = Offset(thumbX, size.height / 2)
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius * 0.5f,
                center = Offset(thumbX, size.height / 2)
            )
        }

        // Прозрачный Material Slider для обработки touch-событий
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent
            )
        )
    }
}
