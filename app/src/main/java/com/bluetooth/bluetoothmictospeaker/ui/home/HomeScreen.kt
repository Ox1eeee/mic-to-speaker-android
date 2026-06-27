package com.bluetooth.bluetoothmictospeaker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.audio.VoiceEffect
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurple
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurpleLight
import com.bluetooth.bluetoothmictospeaker.ui.theme.ActiveGreen
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkBackground
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurfaceVariant
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassBorder
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassWhite
import com.bluetooth.bluetoothmictospeaker.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    viewModel: AudioViewModel,
    onSettingsClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    isPro: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Ambient background animation
    val infiniteTransition = rememberInfiniteTransition(label = "homeBg")
    val orbAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "orbAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Ambient glow canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height * 0.3f
            val rad = Math.toRadians(orbAngle.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentPurple.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(
                        cx + (cos(rad) * size.width * 0.12f).toFloat(),
                        cy + (sin(rad) * size.height * 0.04f).toFloat()
                    ),
                    radius = size.width * 0.5f
                ),
                center = Offset(
                    cx + (cos(rad) * size.width * 0.12f).toFloat(),
                    cy + (sin(rad) * size.height * 0.04f).toFloat()
                ),
                radius = size.width * 0.5f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(
                        cx - (cos(rad + 2.0) * size.width * 0.15f).toFloat(),
                        cy + size.height * 0.25f
                    ),
                    radius = size.width * 0.4f
                ),
                center = Offset(
                    cx - (cos(rad + 2.0) * size.width * 0.15f).toFloat(),
                    cy + size.height * 0.25f
                ),
                radius = size.width * 0.4f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar — title + settings only
            TopBar(onSettingsClick = onSettingsClick)

            Spacer(modifier = Modifier.height(24.dp))

            // Waveform
            WaveformVisualizer(
                amplitude = state.amplitude,
                isActive = state.isRunning,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Power button with glow rings
            PowerButton(
                isRunning = state.isRunning,
                onClick = { viewModel.toggleAudio() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (state.isRunning) "Listening" else "Tap to Start",
                color = if (state.isRunning) ActiveGreen.copy(alpha = 0.9f) else TextSecondary.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Effect section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EFFECTS",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            EffectChipRow(
                effects = VoiceEffect.all(),
                selectedEffect = state.currentEffect,
                isPro = isPro,
                onEffectSelected = { effect ->
                    if (effect.isFree || isPro) {
                        viewModel.setEffect(effect)
                    } else {
                        onUpgradeClick()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Volume slider
            SliderControl(
                label = "Volume",
                value = state.volume,
                onValueChange = { viewModel.setVolume(it) },
                accentColor = AccentCyan
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Echo slider (Pro only)
            SliderControl(
                label = "Echo Mix",
                value = state.echoMix,
                onValueChange = { viewModel.setEchoMix(it) },
                accentColor = AccentPurpleLight,
                isLocked = !isPro,
                onLockedClick = onUpgradeClick
            )
        }
    }
}

@Composable
private fun TopBar(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mic Speaker",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(1.dp, GlassBorder, CircleShape)
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PowerButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "powerScale"
    )

    // Pulsing ring when active
    val infiniteTransition = rememberInfiniteTransition(label = "powerPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = EaseInOut), RepeatMode.Reverse
        ), label = "powerRing"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = EaseInOut), RepeatMode.Reverse
        ), label = "powerRingAlpha"
    )

    val activeColor = ActiveGreen
    val inactiveGradient = Brush.linearGradient(listOf(AccentPurple, AccentCyan))

    Box(contentAlignment = Alignment.Center) {
        // Outer pulsing ring (only when active)
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(ringScale)
                    .alpha(ringAlpha)
                    .drawBehind {
                        drawCircle(
                            color = activeColor,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )
        }

        // Middle ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .drawBehind {
                    drawCircle(
                        color = if (isRunning) activeColor.copy(alpha = 0.1f)
                        else AccentPurple.copy(alpha = 0.08f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
        )

        // Button
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isRunning) Brush.radialGradient(
                        listOf(activeColor, activeColor.copy(alpha = 0.7f))
                    ) else inactiveGradient
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isRunning) "Stop" else "Start",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun EffectChipRow(
    effects: List<VoiceEffect>,
    selectedEffect: VoiceEffect,
    isPro: Boolean,
    onEffectSelected: (VoiceEffect) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(effects) { effect ->
            EffectChip(
                effect = effect,
                isSelected = effect == selectedEffect,
                isLocked = !effect.isFree && !isPro,
                onClick = { onEffectSelected(effect) }
            )
        }
    }
}

@Composable
private fun EffectChip(
    effect: VoiceEffect,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val chipScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )

    Box(
        modifier = Modifier
            .scale(chipScale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Brush.linearGradient(
                    listOf(AccentPurple.copy(alpha = 0.3f), AccentCyan.copy(alpha = 0.12f))
                )
                else Brush.linearGradient(listOf(GlassWhite, GlassWhite))
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(
                    listOf(AccentCyan.copy(alpha = 0.8f), AccentPurple.copy(alpha = 0.4f))
                )
                else Brush.linearGradient(listOf(GlassBorder, GlassBorder)),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = effect.displayName,
                color = if (isLocked) TextSecondary.copy(alpha = 0.5f)
                    else if (isSelected) Color.White else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            if (isLocked) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pro",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    isLocked: Boolean = false,
    onLockedClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLocked) Modifier.clickable(onClick = onLockedClick)
                else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                color = if (isLocked) TextSecondary.copy(alpha = 0.5f)
                    else TextSecondary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (isLocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pro",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(value * 100).toInt()}%",
                color = if (isLocked) TextSecondary.copy(alpha = 0.4f)
                    else accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = if (isLocked) {{}} else onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLocked,
            colors = SliderDefaults.colors(
                thumbColor = if (isLocked) TextSecondary.copy(alpha = 0.3f) else accentColor,
                activeTrackColor = if (isLocked) TextSecondary.copy(alpha = 0.2f)
                    else accentColor.copy(alpha = 0.6f),
                inactiveTrackColor = DarkSurfaceVariant,
                disabledThumbColor = TextSecondary.copy(alpha = 0.3f),
                disabledActiveTrackColor = TextSecondary.copy(alpha = 0.2f),
                disabledInactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}
