package com.bluetooth.bluetoothmictospeaker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.AudioOutputRoute
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.audio.VoiceEffect
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurple
import com.bluetooth.bluetoothmictospeaker.ui.theme.ActiveGreen
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkBackground
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurface
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurfaceVariant
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassBorder
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassWhite
import com.bluetooth.bluetoothmictospeaker.ui.theme.InactiveGray
import com.bluetooth.bluetoothmictospeaker.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: AudioViewModel,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            TopBar(
                currentRoute = state.currentRoute,
                onRouteClick = { viewModel.cycleRoute() },
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Waveform
            WaveformVisualizer(
                amplitude = state.amplitude,
                isActive = state.isRunning,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Power button
            PowerButton(
                isRunning = state.isRunning,
                onClick = { viewModel.toggleAudio() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (state.isRunning) "Tap to Stop" else "Tap to Start",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Effect chips
            Text(
                text = "Voice Effects",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            EffectChipRow(
                effects = VoiceEffect.all(),
                selectedEffect = state.currentEffect,
                onEffectSelected = { viewModel.setEffect(it) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Volume slider
            SliderControl(
                label = "Volume",
                value = state.volume,
                onValueChange = { viewModel.setVolume(it) },
                icon = Icons.Default.VolumeUp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Echo slider
            SliderControl(
                label = "Echo",
                value = state.echoMix,
                onValueChange = { viewModel.setEchoMix(it) },
                icon = Icons.Default.Speaker
            )
        }
    }
}

@Composable
private fun TopBar(
    currentRoute: AudioOutputRoute,
    onRouteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mic Speaker",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRouteClick) {
                Icon(
                    imageVector = routeIcon(currentRoute),
                    contentDescription = currentRoute.displayName,
                    tint = AccentCyan
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PowerButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "powerScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isRunning) ActiveGreen else AccentPurple,
        label = "powerColor"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isRunning) "Stop" else "Start",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun EffectChipRow(
    effects: List<VoiceEffect>,
    selectedEffect: VoiceEffect,
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
                onClick = { onEffectSelected(effect) }
            )
        }
    }
}

@Composable
private fun EffectChip(
    effect: VoiceEffect,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AccentPurple.copy(alpha = 0.3f) else GlassWhite,
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentCyan else GlassBorder,
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = effect.displayName,
            color = if (isSelected) AccentCyan else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(value * 100).toInt()}%",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentPurple,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
    }
}

private fun routeIcon(route: AudioOutputRoute): ImageVector {
    return when (route) {
        AudioOutputRoute.SPEAKER -> Icons.Default.VolumeUp
        AudioOutputRoute.EARPIECE -> Icons.Default.PhoneAndroid
        AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
        AudioOutputRoute.WIRED_HEADSET -> Icons.Default.Headphones
    }
}
