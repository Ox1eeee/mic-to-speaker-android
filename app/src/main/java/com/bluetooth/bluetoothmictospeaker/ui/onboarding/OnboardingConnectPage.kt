package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class PairPhase(val icon: ImageVector, val color: Color, val text: String) {
    SEARCHING(Icons.Filled.CellTower, Color.White, "Searching for speakers…"),
    FOUND(Icons.Filled.Speaker, SysCyan, "Found: Party Speaker"),
    CONNECTED(Icons.Filled.CheckCircle, SysGreen, "Connected — you're live!")
}

@Composable
internal fun ConnectPage(onContinue: () -> Unit) {
    val view = LocalView.current
    val reduceMotion = rememberReduceMotion()
    val t = rememberSeconds(paused = reduceMotion)
    var phase by remember { mutableStateOf(PairPhase.SEARCHING) }
    val beam = remember { Animatable(0f) }
    val live = phase == PairPhase.CONNECTED

    LaunchedEffect(Unit) {
        beam.animateTo(0.6f, tween(1400, easing = EaseInOutIos))
        phase = PairPhase.FOUND
        launch { beam.animateTo(1f, tween(800, easing = EaseInOutIos)) }
        delay(900)
        phase = PairPhase.CONNECTED
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoSizeText(
                text = "Connect to Any Speaker",
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                minScale = 0.8f,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Pair a Bluetooth speaker and your phone becomes a wireless mic.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
        FlexGap(24.dp)
        ConnectionScene(
            live = live,
            beamProgress = { beam.value },
            t = t,
            reduceMotion = reduceMotion,
            modifier = Modifier.fillMaxWidth().height(230.dp).padding(horizontal = 24.dp)
        )
        FlexGap(24.dp)
        StatusPill(phase)
        FlexGap(8.dp)
        ContinueButton(onClick = onContinue)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConnectionScene(
    live: Boolean,
    beamProgress: () -> Float,
    t: State<Float>,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val liveAlpha by animateFloatAsState(if (live) 1f else 0f, IosSpring(0.5f, 0.7f).spec(), label = "live")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Phone(live = live, t = t)
            SignalArcs(
                t = t,
                hiddenFraction = { liveAlpha },
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = 26.dp).size(20.dp)
            )
        }
        Beam(
            live = live,
            progress = beamProgress,
            t = t,
            modifier = Modifier.weight(1f).height(6.dp)
        )
        Box(Modifier.width(120.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (!reduceMotion) {
                SoundRings(t, Modifier.size(120.dp).graphicsLayer { alpha = liveAlpha.coerceIn(0f, 1f) })
                FloatingNotes(t, Modifier.size(120.dp, 200.dp).graphicsLayer { alpha = liveAlpha.coerceIn(0f, 1f) })
            }
            BluetoothSpeaker(
                width = 96.dp,
                accent = SysCyan,
                beatRate = 2f,
                isConnected = live,
                isPlaying = live,
                t = t
            )
        }
    }
}

// region 2.3 Phone + 2.4 Signal arcs

@Composable
private fun Phone(live: Boolean, t: State<Float>) {
    val glowAlpha by animateFloatAsState(if (live) 0.45f else 0.15f, IosSpring(0.5f, 0.7f).spec(), label = "phoneGlow")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .size(72.dp, 136.dp)
            .glow(SysCyan.copy(alpha = glowAlpha), 14.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2E2E2E), Color(0xFF121212))))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(SysCyan.copy(alpha = 0.7f), SysPurple.copy(alpha = 0.7f))),
                shape
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(22.dp, 6.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    val brush = Brush.verticalGradient(listOf(SysCyan, SysPurple))
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush, blendMode = BlendMode.SrcAtop)
                    }
                },
            tint = Color.White
        )
        LevelBars(live, t, Modifier.size(40.dp, 22.dp))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LevelBars(live: Boolean, t: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * 4) / 5
        val brush = Brush.verticalGradient(listOf(SysCyan, SysPurple))
        for (i in 0 until 5) {
            val level = if (live) 0.3f + 0.7f * abs(sin(6f * t.value + 1.3f * i)) else 0.2f
            val h = size.height * level
            drawRoundRect(
                brush = brush,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2)
            )
        }
    }
}

@Composable
private fun SignalArcs(t: State<Float>, hiddenFraction: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val fade = 1f - hiddenFraction().coerceIn(0f, 1f)
        if (fade <= 0f) return@Canvas
        val stroke = 2.dp.toPx()
        val origin = Offset(0f, size.height / 2)
        for (k in 0 until 3) {
            val r = size.width * (0.35f + 0.3f * k)
            val p = frac(t.value - k * 0.25f)
            val alpha = 0.3f + 0.7f * sin(PI.toFloat() * p).let { it * it }
            drawArc(
                color = SysCyan.copy(alpha = alpha * fade),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(origin.x - r, origin.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

// endregion

// region 2.5 Beam

@Composable
private fun Beam(live: Boolean, progress: () -> Float, t: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(h / 2)
        drawRoundRect(Color.White.copy(alpha = 0.10f), cornerRadius = corner)

        val p = progress()
        val fillW = w * p
        if (fillW > 0f) {
            drawGlowRoundRect(SysCyan.copy(alpha = 0.7f), Offset.Zero, Size(fillW, h), h / 2, 6.dp.toPx())
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(SysCyan, SysPurple), startX = 0f, endX = w),
                size = Size(fillW, h),
                cornerRadius = corner
            )
        }

        val cy = h / 2
        if (live) {
            val d = 6.dp.toPx()
            for (i in 0 until 3) {
                val dp = frac(0.8f * t.value + i / 3f)
                val alpha = sin(PI.toFloat() * dp)
                val c = Offset(dp * (w - d) + d / 2, cy)
                drawGlowCircle(SysCyan.copy(alpha = alpha), c, d / 2, 4.dp.toPx())
                drawCircle(Color.White.copy(alpha = alpha), d / 2, c)
            }
        } else if (p > 0f && p < 1f) {
            val d = 8.dp.toPx()
            val c = Offset(max(0f, fillW - d) + d / 2, cy)
            drawGlowCircle(SysCyan, c, d / 2, 4.dp.toPx())
            drawCircle(Color.White, d / 2, c)
        }
    }
}

// endregion

// region 2.6 Sound rings + 2.7 Floating notes

@Composable
private fun SoundRings(t: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val base = size.minDimension / 2
        for (i in 0 until 3) {
            val p = frac(0.6f * t.value + i / 3f)
            drawCircle(
                color = SysCyan.copy(alpha = (1f - p) * 0.6f),
                radius = base * (0.6f + 1.1f * p),
                style = Stroke(2.dp.toPx())
            )
        }
    }
}

private val Notes = listOf("♪", "♫", "♪", "♬", "♫")

@Composable
private fun FloatingNotes(t: State<Float>, modifier: Modifier = Modifier) {
    Box(modifier) {
        Notes.forEachIndexed { i, note ->
            Text(
                text = note,
                fontSize = (18 + 4 * (i % 3)).sp,
                fontWeight = FontWeight.Bold,
                color = if (i % 2 == 0) SysCyan else SysPurple,
                modifier = Modifier
                    .offset {
                        val p = frac(0.35f * t.value + 0.2f * i)
                        val w = 120.dp.toPx()
                        val h = 200.dp.toPx()
                        val x = w * (0.2f + 0.15f * i) + 14.dp.toPx() * sin(2f * PI.toFloat() * p + i)
                        val y = h * 0.5f - p * h * 0.5f
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                    .graphicsLayer { alpha = sin(PI.toFloat() * frac(0.35f * t.value + 0.2f * i)) }
            )
        }
    }
}

// endregion

// region 2.9 Status pill

@Composable
private fun StatusPill(phase: PairPhase) {
    val spring = IosSpring(0.45f)
    val borderColor by animateColorAsState(phase.color.copy(alpha = 0.5f), spring.spec(), label = "pillBorder")
    val glowColor by animateColorAsState(
        if (phase == PairPhase.SEARCHING) Color.Transparent else phase.color.copy(alpha = 0.4f),
        spring.spec(),
        label = "pillGlow"
    )
    Row(
        modifier = Modifier
            .glow(glowColor, 10.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialFill)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (scaleIn(spring.spec()) + fadeIn(spring.spec())) togetherWith
                    (scaleOut(spring.spec()) + fadeOut(spring.spec()))
            },
            label = "pillIcon"
        ) { p ->
            Icon(p.icon, contentDescription = null, tint = p.color, modifier = Modifier.size(18.dp))
        }
        Crossfade(targetState = phase, label = "pillText") { p ->
            Text(p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
        }
    }
}

// endregion
