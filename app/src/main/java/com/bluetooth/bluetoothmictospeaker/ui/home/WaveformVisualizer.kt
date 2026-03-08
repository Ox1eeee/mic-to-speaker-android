package com.bluetooth.bluetoothmictospeaker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurple

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 20
) {
    val animatedBars = remember {
        List(barCount) { Animatable(0f) }
    }

    LaunchedEffect(amplitude, isActive) {
        if (!isActive) {
            animatedBars.forEach { bar ->
                bar.animateTo(
                    targetValue = 0.05f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            return@LaunchedEffect
        }

        animatedBars.forEachIndexed { index, bar ->
            val variation = when {
                index % 3 == 0 -> 1.0f
                index % 3 == 1 -> 0.7f
                else -> 0.85f
            }
            val distance = kotlin.math.abs(index - barCount / 2f) / (barCount / 2f)
            val centerBoost = 1f - distance * 0.5f
            val target = (amplitude * variation * centerBoost).coerceIn(0.05f, 1f)

            bar.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 2f)
        val gap = barWidth

        animatedBars.forEachIndexed { index, bar ->
            val barHeight = totalHeight * bar.value
            val x = index * (barWidth + gap) + gap / 2

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(AccentCyan, AccentPurple),
                startY = (totalHeight - barHeight) / 2,
                endY = (totalHeight + barHeight) / 2
            )

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, (totalHeight - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
