package com.bluetooth.bluetoothmictospeaker.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.AudioOutputRoute
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurple
import com.bluetooth.bluetoothmictospeaker.ui.theme.ActiveGreen
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkBackground
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurfaceVariant
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassBorder
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassWhite
import com.bluetooth.bluetoothmictospeaker.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    audioViewModel: AudioViewModel,
    onBack: () -> Unit,
    onRateApp: () -> Unit,
    onUpgrade: () -> Unit = {},
    isPro: Boolean = false,
    modifier: Modifier = Modifier
) {
    val audioState by audioViewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Subtle ambient glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentPurple.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.15f),
                    radius = size.width * 0.5f
                ),
                center = Offset(size.width * 0.3f, size.height * 0.15f),
                radius = size.width * 0.5f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GlassWhite)
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Audio Output Section
            SectionHeader(title = "Audio Output")

            Spacer(modifier = Modifier.height(12.dp))

            val routes = audioState.availableRoutes
            routes.forEach { route ->
                val isRouteFree = route == AudioOutputRoute.SPEAKER
                val isLocked = !isRouteFree && !isPro
                AudioRouteItem(
                    route = route,
                    isSelected = route == audioState.currentRoute,
                    isLocked = isLocked,
                    onClick = {
                        if (isLocked) {
                            onUpgrade()
                        } else {
                            audioViewModel.setAudioRoute(route)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!isPro) {
                // Premium Section
                SectionHeader(title = "Premium")

                Spacer(modifier = Modifier.height(12.dp))

                SettingsItem(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Upgrade to Pro",
                    subtitle = "Unlock all voice effects & features",
                    accentColor = AccentCyan,
                    onClick = onUpgrade
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // General Section
            SectionHeader(title = "General")

            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Star,
                title = "Rate App",
                subtitle = "Love the app? Leave a review!",
                accentColor = AccentCyan,
                onClick = onRateApp
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Mic Speaker v1.0",
                accentColor = AccentPurple,
                onClick = { }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary.copy(alpha = 0.6f),
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun AudioRouteItem(
    route: AudioOutputRoute,
    isSelected: Boolean,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val icon = when (route) {
        AudioOutputRoute.SPEAKER -> Icons.Default.VolumeUp
        AudioOutputRoute.EARPIECE -> Icons.Default.PhoneAndroid
        AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
        AudioOutputRoute.WIRED_HEADSET -> Icons.Default.Headphones
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Brush.linearGradient(
                    listOf(AccentPurple.copy(alpha = 0.2f), AccentCyan.copy(alpha = 0.08f))
                )
                else Brush.linearGradient(listOf(GlassWhite, GlassWhite))
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(
                    listOf(AccentCyan.copy(alpha = 0.7f), AccentPurple.copy(alpha = 0.3f))
                )
                else Brush.linearGradient(listOf(GlassBorder, GlassBorder)),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AccentCyan.copy(alpha = 0.15f)
                            else DarkSurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = route.displayName,
                        tint = if (isSelected) AccentCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = route.displayName,
                    fontSize = 15.sp,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pro",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            } else if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ActiveGreen)
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}
