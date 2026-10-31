package com.bluetooth.bluetoothmictospeaker.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.BluetoothAudio
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.AudioOutputRoute
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.audio.VoiceEffect
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.Accent
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.OnboardingBackground
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.SysYellow
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.glow
import com.bluetooth.bluetoothmictospeaker.utils.PermissionHelper

private val CardFill = Color.White.copy(alpha = 0.28f)
private val ChipFill = Color.White.copy(alpha = 0.30f)
private val ProGradient = Brush.horizontalGradient(listOf(Color(0xFF40C4FF), Color(0xFFC86DD7)))

private fun VoiceEffect.emoji(): String = when (this) {
    VoiceEffect.None -> "🎙️"
    VoiceEffect.Robot -> "🤖"
    VoiceEffect.Chipmunk -> "🐿️"
    VoiceEffect.DeepVoice -> "🔊"
    VoiceEffect.Echo -> "🏛️"
    VoiceEffect.Alien -> "👽"
    VoiceEffect.Radio -> "📻"
    VoiceEffect.Princess -> "👸"
    VoiceEffect.Helium -> "🎈"
    VoiceEffect.Monster -> "👹"
    VoiceEffect.Telephone -> "☎️"
    VoiceEffect.Underwater -> "🌊"
    VoiceEffect.Megaphone -> "📢"
    VoiceEffect.Baby -> "👶"
    VoiceEffect.WalkieTalkie -> "📡"
    VoiceEffect.Drunk -> "🍺"
    VoiceEffect.Zombie -> "🧟"
    VoiceEffect.Giant -> "🗿"
    VoiceEffect.Cave -> "🦇"
    VoiceEffect.Ghost -> "👻"
    VoiceEffect.DarthVader -> "🦹"
    VoiceEffect.OldMan -> "👴"
    VoiceEffect.Astronaut -> "🚀"
    VoiceEffect.EightBit -> "👾"
    VoiceEffect.Stadium -> "🏟️"
    VoiceEffect.GenderSwap -> "🔄"
    VoiceEffect.Vocoder -> "🎛️"
    VoiceEffect.Choir -> "🎶"
}

private fun openBluetoothSettings(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
}

@Composable
fun HomeScreen(
    viewModel: AudioViewModel,
    onSettingsClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    isPro: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopBar(
                isPro = isPro,
                onSettingsClick = onSettingsClick,
                onUpgradeClick = onUpgradeClick,
                onOutputClick = { openBluetoothSettings(context) },
                onHelpClick = { showHelp = true }
            )

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                MicButton(
                    isRunning = state.isRunning,
                    amplitude = state.amplitude,
                    diameter = min(196.dp, maxHeight * 0.72f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (!state.isRunning && !PermissionHelper.hasAudioPermission(context)) {
                            onRequestPermissions()
                        } else {
                            viewModel.toggleAudio()
                        }
                    }
                )
            }

            EffectChipRow(
                selectedEffect = state.currentEffect,
                isPro = isPro,
                onEffectSelected = { effect ->
                    if (effect.isFree || isPro) viewModel.setEffect(effect) else onUpgradeClick()
                }
            )

            Spacer(Modifier.height(16.dp))

            AudioControlsCard(
                expanded = controlsExpanded,
                onToggle = { controlsExpanded = !controlsExpanded },
                volume = state.volume,
                onVolumeChange = viewModel::setVolume,
                echo = state.echoMix,
                onEchoChange = viewModel::setEchoMix,
                echoLocked = !isPro,
                onUpgradeClick = onUpgradeClick,
                isRunning = state.isRunning,
                route = state.currentRoute,
                onFixRoute = { openBluetoothSettings(context) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}

// region Top bar

@Composable
private fun TopBar(
    isPro: Boolean,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    onOutputClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, "Settings", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            if (!isPro) ProPill(onClick = onUpgradeClick)
        }

        Text(
            text = "LiveMic",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOutputClick) {
                Icon(Icons.Outlined.BluetoothAudio, "Audio output", tint = Color.White, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = onHelpClick) {
                Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Help", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun ProPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .glow(Color(0xFF40C4FF).copy(alpha = 0.55f), 12.dp, CircleShape)
            .clip(CircleShape)
            .background(ProGradient)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text("PRO", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// endregion

// region Mic button

@Composable
private fun MicButton(
    isRunning: Boolean,
    amplitude: Float,
    diameter: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "micPress")

    val fill by animateColorAsState(
        if (isRunning) Accent else Color.White.copy(alpha = 0.14f),
        tween(350),
        label = "micFill"
    )
    val border by animateColorAsState(
        Color.White.copy(alpha = if (isRunning) 0.55f else 0.30f),
        tween(350),
        label = "micBorder"
    )
    val haloAlpha by animateFloatAsState(if (isRunning) 1f else 0f, tween(450), label = "halo")
    val level by animateFloatAsState(amplitude.coerceIn(0f, 1f), tween(120), label = "level")

    val breathing = rememberInfiniteTransition(label = "breath")
    val breath by breathing.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "breathValue"
    )

    Box(contentAlignment = Alignment.Center) {
        // Soft purple halo while live
        Canvas(
            Modifier
                .size(diameter * 1.5f)
                .graphicsLayer {
                    alpha = haloAlpha
                    val s = 1f + 0.04f * breath + 0.12f * level
                    scaleX = s
                    scaleY = s
                }
        ) {
            drawCircle(
                Brush.radialGradient(
                    0.55f to Accent.copy(alpha = 0.75f),
                    0.72f to Accent.copy(alpha = 0.30f),
                    1f to Color.Transparent
                )
            )
        }

        Box(
            modifier = Modifier
                .size(diameter)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .glow(Accent.copy(alpha = 0.6f * haloAlpha), 24.dp)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, border, CircleShape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = if (isRunning) "Turn mic off" else "Turn mic on" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(diameter * 0.36f)
            )
        }
    }
}

// endregion

// region Effect chips

@Composable
private fun EffectChipRow(
    selectedEffect: VoiceEffect,
    isPro: Boolean,
    onEffectSelected: (VoiceEffect) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        items(VoiceEffect.all()) { effect ->
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
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val fill by animateColorAsState(
        if (isSelected) Accent.copy(alpha = 0.22f) else ChipFill,
        tween(200),
        label = "chipFill"
    )
    Row(
        modifier = Modifier
            .height(40.dp)
            .glow(if (isSelected) Accent.copy(alpha = 0.6f) else Color.Transparent, 10.dp, CircleShape)
            .clip(CircleShape)
            .background(fill)
            .then(
                if (isSelected) Modifier.border(1.5.dp, Color(0xFF7C4DFF), CircleShape) else Modifier
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(effect.emoji(), fontSize = 16.sp)
        Text(effect.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (isLocked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "Pro",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// endregion

// region Audio controls card

@Composable
private fun AudioControlsCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    echo: Float,
    onEchoChange: (Float) -> Unit,
    echoLocked: Boolean,
    onUpgradeClick: () -> Unit,
    isRunning: Boolean,
    route: AudioOutputRoute,
    onFixRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardFill)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audio Controls", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White,
                modifier = Modifier.size(26.dp).rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ControlLabel("Volume")
                LiveMicSlider(value = volume, onValueChange = onVolumeChange)

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = if (echoLocked) Modifier.clickable(onClick = onUpgradeClick) else Modifier
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ControlLabel("Echo", locked = echoLocked)
                        LiveMicSlider(value = echo, onValueChange = onEchoChange, enabled = !echoLocked)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Mic ${if (isRunning) "On" else "Off"} — Output: ${route.displayName}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (route != AudioOutputRoute.BLUETOOTH) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onFixRoute)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = SysYellow, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Not routed to Bluetooth — tap here or the speaker icon to connect one",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlLabel(text: String, locked: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        if (locked) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.Lock, "Pro", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveMicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        thumb = {
            Box(
                Modifier
                    .size(width = 44.dp, height = 28.dp)
                    .shadow(4.dp, CircleShape)
                    .background(if (enabled) Color.White else Color.White.copy(alpha = 0.6f), CircleShape)
            )
        },
        track = { sliderState -> LiveMicTrack(sliderState, enabled) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveMicTrack(state: SliderState, enabled: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val range = state.valueRange
        val fraction = ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.18f), cornerRadius = corner)
        drawRoundRect(
            color = if (enabled) Accent else Color.White.copy(alpha = 0.35f),
            size = Size(size.width * fraction, size.height),
            cornerRadius = corner
        )
    }
}

// endregion

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        title = { Text("How to use LiveMic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Connect a Bluetooth speaker. Tap the speaker icon at the top to open Bluetooth settings.")
                Text("2. Tap the big mic button to go live.")
                Text("3. Pick a voice effect and adjust volume and echo in Audio Controls.")
                Text("Tip: keep your phone a few steps away from the speaker to avoid feedback.")
            }
        }
    )
}
