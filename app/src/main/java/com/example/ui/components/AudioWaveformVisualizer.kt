package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSpacing = width / barCount
        val barWidth = (totalSpacing * 0.55f).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount
            val isPassed = normalizedX <= progress

            // Amplitude based on sine wave pattern + animation when playing
            val baseAmp = 0.25f + 0.65f * kotlin.math.abs(sin((i * 0.45f) + if (isPlaying) phase else 0f))
            val barHeight = (height * baseAmp).coerceIn(4f, height)

            val x = i * totalSpacing + (totalSpacing - barWidth) / 2
            val y = (height - barHeight) / 2

            val color = if (isPassed) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
