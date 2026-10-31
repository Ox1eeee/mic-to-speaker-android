package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

private enum class KaraokeEffect(
    val label: String,
    val emoji: String,
    val style: EffectStyle,
    val beatRate: Float
) {
    Autotune("Autotune", "🎵", EffectStyle.AUTOTUNE, 2.0f),
    Robot("Robot", "🤖", EffectStyle.ROBOT, 2.4f),
    Chipmunk("Chipmunk", "🐿️", EffectStyle.CHIPMUNK, 3.2f),
    Demon("Demon", "😈", EffectStyle.DEMON, 1.4f),
    Echo("Echo", "🏛️", EffectStyle.ECHO, 1.8f);

    val primary: Color get() = style.primary
    val secondary: Color get() = style.secondary
    fun next(): KaraokeEffect = entries[(ordinal + 1) % entries.size]
}

private val Lyrics = listOf(
    "Turn the music up",
    "Sing it like a star",
    "Any speaker, any room",
    "The stage is yours"
)
private const val LINE_SECONDS = 2.8f

@Composable
internal fun KaraokePage(onContinue: () -> Unit) {
    val view = LocalView.current
    val reduceMotion = rememberReduceMotion()
    val t = rememberSeconds(paused = reduceMotion)

    var effect by remember { mutableStateOf(KaraokeEffect.Autotune) }
    var lastManualTapMs by remember { mutableLongStateOf(0L) }
    var hapticReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2200)
            if (System.currentTimeMillis() - lastManualTapMs > 5000) effect = effect.next()
        }
    }
    LaunchedEffect(effect) {
        if (hapticReady) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK
                else HapticFeedbackConstants.CLOCK_TICK
            )
        } else {
            hapticReady = true
        }
    }

    val colorSpec = tween<Color>(400, easing = EaseInOutIos)
    val primary by animateColorAsState(effect.primary, colorSpec, label = "stagePrimary")
    val secondary by animateColorAsState(effect.secondary, colorSpec, label = "stageSecondary")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlexGap(32.dp)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoSizeText(
                text = "Karaoke Night",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                minScale = 0.8f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Sing live through any speaker and switch effects on the fly.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
        FlexGap(20.dp)
        Box(
            modifier = Modifier.weight(3f).fillMaxWidth().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Stage(
                effect = effect,
                primary = primary,
                secondary = secondary,
                t = t,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 320.dp).fillMaxHeight()
            )
        }
        FlexGap(16.dp)
        EffectPicker(
            selected = effect,
            onSelect = {
                lastManualTapMs = System.currentTimeMillis()
                effect = it
            }
        )
        FlexGap(20.dp)
        ContinueButton(onClick = onContinue)
        Spacer(Modifier.height(16.dp))
    }
}

// region 3.3 Stage

@Composable
private fun Stage(
    effect: KaraokeEffect,
    primary: Color,
    secondary: Color,
    t: State<Float>,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .glow(primary.copy(alpha = 0.35f), 20.dp, shape)
            .clip(shape)
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(primary.copy(alpha = 0.8f), secondary.copy(alpha = 0.5f))),
                shape
            )
    ) {
        StageBackground(primary, t)
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BluetoothSpeaker(36.dp, primary, effect.beatRate, isConnected = true, isPlaying = true, t = t)
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                EffectBadge(effect)
                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.weight(1f))
                BluetoothSpeaker(36.dp, primary, effect.beatRate, isConnected = true, isPlaying = true, t = t)
            }
            EffectWaveform(
                style = effect.style,
                primary = primary,
                secondary = secondary,
                t = t,
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 20.dp)
            )
            KaraokeLyrics(
                primary = primary,
                secondary = secondary,
                t = t,
                fullyHighlighted = reduceMotion,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StageBackground(primary: Color, t: State<Float>) {
    // Base
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(StageTop, Color.Black))))
    // Spotlight cones
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val r = 10.dp.toPx()
                    renderEffect = BlurEffect(r, r, TileMode.Decal)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val coneW = 0.55f * w
        val coneH = 1.1f * h
        val topHalf = 4.dp.toPx()
        fun cone(cx: Float) = Path().apply {
            moveTo(cx - topHalf, 0f)
            lineTo(cx + topHalf, 0f)
            lineTo(cx + coneW / 2, coneH)
            lineTo(cx - coneW / 2, coneH)
            close()
        }
        val c1 = 0.25f * w
        rotate(sin(0.7f * t.value) * 18f - 10f, pivot = Offset(c1, 0f)) {
            drawPath(
                cone(c1),
                Brush.verticalGradient(listOf(primary.copy(alpha = 0.55f), primary.copy(alpha = 0f)), 0f, coneH),
                blendMode = BlendMode.Screen
            )
        }
        val c2 = 0.75f * w
        rotate(cos(0.6f * t.value) * 18f + 10f, pivot = Offset(c2, 0f)) {
            drawPath(
                cone(c2),
                Brush.verticalGradient(listOf(SysPurple.copy(alpha = 0.5f), SysPurple.copy(alpha = 0f)), 0f, coneH),
                blendMode = BlendMode.Screen
            )
        }
    }
    // Crowd silhouette
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width / 9
        val bandTop = size.height * 0.8f
        val headR = 0.22f * w
        val crowd = Color.Black.copy(alpha = 0.9f)
        for (i in 0 until 9) {
            val cx = (i + 0.5f) * w
            val bob = 3.dp.toPx() * sin(3f * t.value + 1.7f * i)
            val headY = bandTop + headR + 4.dp.toPx() + bob + (i % 2) * 5.dp.toPx()
            drawCircle(crowd, headR, Offset(cx, headY))
            val bodyTop = headY + 0.8f * headR
            drawRoundRect(
                color = crowd,
                topLeft = Offset(cx - 0.4f * w, bodyTop),
                size = Size(0.8f * w, size.height - bodyTop + 0.3f * w),
                cornerRadius = CornerRadius(0.3f * w)
            )
        }
    }
}

// endregion

// region 3.6 Effect badge

@Composable
private fun EffectBadge(effect: KaraokeEffect) {
    AnimatedContent(
        targetState = effect,
        transitionSpec = {
            (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.4f) + fadeOut())
        },
        label = "effectBadge"
    ) { e ->
        Row(
            modifier = Modifier
                .glow(e.primary.copy(alpha = 0.6f), 10.dp, CircleShape)
                .clip(CircleShape)
                .background(e.primary.copy(alpha = 0.25f))
                .border(1.5.dp, e.primary.copy(alpha = 0.8f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(e.emoji, fontSize = 28.sp)
            Text(e.label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
        }
    }
}

// endregion

// region 3.7 Karaoke lyrics

@Composable
private fun KaraokeLyrics(
    primary: Color,
    secondary: Color,
    t: State<Float>,
    fullyHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val index by remember { derivedStateOf { floor(t.value / LINE_SECONDS).toInt() % Lyrics.size } }
    val line = Lyrics[index]
    val next = Lyrics[(index + 1) % Lyrics.size]
    val words = line.split(" ")
    val progress: () -> Float = {
        if (fullyHighlighted) 1f else min(1f, (t.value % LINE_SECONDS) / (LINE_SECONDS * 0.85f))
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val baseStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.W800)
            val maxPx = constraints.maxWidth.toFloat()
            val scale = remember(line, maxPx) {
                val gapPx = with(density) { 6.dp.toPx() } * (words.size - 1)
                val wordsPx = words.sumOf { measurer.measure(it, baseStyle).size.width }
                ((maxPx) / (wordsPx + gapPx)).coerceIn(0.6f, 1f)
            }
            val style = baseStyle.copy(fontSize = baseStyle.fontSize * scale)
            val glowBlur = with(density) { 6.dp.toPx() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
                words.forEachIndexed { i, word ->
                    Box {
                        Text(
                            text = word,
                            maxLines = 1,
                            softWrap = false,
                            style = style.copy(
                                color = Color.White.copy(alpha = 0.4f),
                                shadow = Shadow(primary.copy(alpha = 0.5f), blurRadius = glowBlur)
                            )
                        )
                        Text(
                            text = word,
                            maxLines = 1,
                            softWrap = false,
                            style = style.copy(brush = Brush.horizontalGradient(listOf(primary, secondary))),
                            modifier = Modifier.drawWithContent {
                                val fill = (progress() * words.size - i).coerceIn(0f, 1f)
                                if (fill > 0f) clipRect(right = size.width * fill) { this@drawWithContent.drawContent() }
                            }
                        )
                    }
                }
            }
        }
        Text(
            text = next,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.35f),
            maxLines = 1
        )
    }
}

// endregion

// region 3.8 Effect picker

@Composable
private fun EffectPicker(selected: KaraokeEffect, onSelect: (KaraokeEffect) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KaraokeEffect.entries.forEach { e ->
            val isSelected = e == selected
            val spring = IosSpring(0.4f, 0.7f)
            val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, spring.spec(), label = "pickScale")
            val fill by animateColorAsState(
                if (isSelected) e.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                spring.spec(),
                label = "pickFill"
            )
            val border by animateColorAsState(
                if (isSelected) e.primary else Color.White.copy(alpha = 0.15f),
                spring.spec(),
                label = "pickBorder"
            )
            val glowColor by animateColorAsState(
                e.primary.copy(alpha = if (isSelected) 0.7f else 0f),
                spring.spec(),
                label = "pickGlow"
            )
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(e) }
                    .semantics {
                        contentDescription = "${e.label} effect"
                        role = Role.Button
                        this.selected = isSelected
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .glow(glowColor, 8.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .border(if (isSelected) 2.dp else 1.dp, border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(e.emoji, fontSize = 24.sp)
                }
                Text(
                    text = e.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = if (isSelected) 1f else 0.55f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// endregion
