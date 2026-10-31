package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.OnboardingTonePlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Social proof (1.10): keep in sync with the real Google Play listing.
private const val PLAY_RATING = "4.7"
private const val PLAY_USERS = "22K+"
private const val PLAY_USERS_SPOKEN = "22 thousand"

private enum class HeroVoice(
    val color: Color,
    val emoji: String,
    val label: String,
    val style: EffectStyle,
    val soundKey: String
) {
    Robot(RobotGreen, "🤖", "Robot", EffectStyle.ROBOT, "Robot"),
    Chipmunk(SysYellow, "🐿️", "Chipmunk", EffectStyle.CHIPMUNK, "Chipmunk"),
    Demon(DemonRed, "😈", "Demon", EffectStyle.DEMON, "Demon"),
    Echo(EchoBlue, "🏛️", "Echo", EffectStyle.ECHO, "Echo Chamber"),
    Helium(HeliumPink, "🎈", "Helium", EffectStyle.CHIPMUNK, "Helium"),
    Ghost(GhostBlue, "👻", "Ghost", EffectStyle.ECHO, "Ghost");

    fun next(): HeroVoice = entries[(ordinal + 1) % entries.size]
}

private val OrbitEmojis = listOf("🤖", "🦹", "🐿️", "👽", "😈", "🎵", "🏛️", "🚀", "🎈", "🦕", "👻", "📻")

private val AutoCycleSpring = IosSpring(0.4f, 0.7f)
private val TapSpring = IosSpring(0.35f, 0.65f)
private val EntranceSpring = IosSpring(0.8f, 0.75f)

@Composable
internal fun HeroPage(onContinue: () -> Unit) {
    val view = LocalView.current
    val reduceMotion = rememberReduceMotion()
    val t = rememberSeconds(paused = reduceMotion)
    val tonePlayer = remember { OnboardingTonePlayer() }
    DisposableEffect(Unit) { onDispose { tonePlayer.release() } }

    var voice by remember { mutableStateOf(HeroVoice.Robot) }
    var voiceSpring by remember { mutableStateOf(AutoCycleSpring) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    var appeared by remember { mutableStateOf(false) }
    val enter by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = EntranceSpring.spec(),
        label = "heroEnter"
    )

    LaunchedEffect(Unit) {
        appeared = true
        delay(900)
        while (isActive) {
            delay(1600)
            if (System.currentTimeMillis() - lastTapMs > 3000) {
                voiceSpring = AutoCycleSpring
                voice = voice.next()
            }
        }
    }

    val onMicTap: () -> Unit = {
        lastTapMs = System.currentTimeMillis()
        tapCount++
        val next = voice.next()
        voiceSpring = TapSpring
        voice = next
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        tonePlayer.play(next.soundKey)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroHeight = if (maxHeight < 720.dp) 210.dp else 250.dp

        VisualizerBarsBackground(
            paused = reduceMotion,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FlexGap(28.dp)
            HeroHeader(
                Modifier.graphicsLayer {
                    alpha = enter.coerceIn(0f, 1f)
                    translationY = (1f - enter) * 16.dp.toPx()
                }
            )
            FlexGap(8.dp)
            HeroBox(
                voice = voice,
                voiceSpring = voiceSpring,
                tapCount = tapCount,
                t = t,
                enter = { enter },
                heroHeight = heroHeight,
                onMicTap = onMicTap
            )
            Crossfade(targetState = tapCount > 0, modifier = Modifier.padding(top = 4.dp), label = "hint") { tapped ->
                Text(
                    text = if (tapped) "Tap again for another voice" else "Tap the mic to hear it 🔊",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            VoiceTransformRow(
                voice = voice,
                voiceSpring = voiceSpring,
                t = t,
                modifier = Modifier.padding(horizontal = 40.dp).padding(top = 12.dp)
            )
            FlexGap(12.dp)
            ValueChips(Modifier.graphicsLayer { alpha = enter.coerceIn(0f, 1f) })
            FlexGap(16.dp)
            ContinueButton(
                onClick = onContinue,
                shimmerClock = if (reduceMotion) null else t,
                modifier = Modifier.graphicsLayer { alpha = (enter * 2f).coerceIn(0f, 1f) }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// region 1.1 Header + 1.10 Social proof

@Composable
private fun HeroHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LiveMic",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.W800,
                brush = Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
            )
        )
        AutoSizeText(
            text = "Your Phone is now a Mic",
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            minScale = 0.75f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "30 voice effects, live on any Bluetooth speaker.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        SocialProofRow(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SocialProofRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Rated $PLAY_RATING stars, loved by over $PLAY_USERS_SPOKEN users"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(4) {
                Icon(Icons.Filled.Star, null, tint = SysYellow, modifier = Modifier.size(11.dp))
            }
            Icon(Icons.AutoMirrored.Filled.StarHalf, null, tint = SysYellow, modifier = Modifier.size(11.dp))
        }
        Text(PLAY_RATING, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("· Loved by $PLAY_USERS users", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
    }
}

// endregion

// region 1.2 Visualizer bars background

@Composable
private fun VisualizerBarsBackground(paused: Boolean, modifier: Modifier = Modifier) {
    val specs = remember {
        List(40) {
            Triple(
                Random.nextFloat() * 120f + 40f,
                Random.nextFloat() * 120f + 60f,
                Random.nextInt(800, 1501)
            )
        }
    }
    val heights: List<State<Float>> = if (paused) {
        remember { specs.map { mutableStateOf(it.first) } }
    } else {
        val transition = rememberInfiniteTransition(label = "bars")
        specs.mapIndexed { i, (a, b, duration) ->
            transition.animateFloat(
                initialValue = a,
                targetValue = b,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = EaseInOutIos),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 50)
                ),
                label = "bar$i"
            )
        }
    }

    Canvas(modifier.graphicsLayer { alpha = 0.7f }) {
        val barW = 4.dp.toPx()
        val gap = 6.dp.toPx()
        val total = 40 * barW + 39 * gap
        val startX = (size.width - total) / 2
        heights.forEachIndexed { i, state ->
            val h = state.value.dp.toPx()
            val top = (size.height - h) / 2
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(SysPurple.copy(alpha = 0.3f), NeonCyan.copy(alpha = 0.3f)),
                    startY = top,
                    endY = top + h
                ),
                topLeft = Offset(startX + i * (barW + gap), top),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2)
            )
        }
    }
}

// endregion

// region Hero box: orbit, mic, burst, bubble

@Composable
private fun HeroBox(
    voice: HeroVoice,
    voiceSpring: IosSpring,
    tapCount: Int,
    t: State<Float>,
    enter: () -> Float,
    heroHeight: Dp,
    onMicTap: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(heroHeight),
        contentAlignment = Alignment.Center
    ) {
        val s = heroHeight / 250.dp
        val radius = min(maxWidth, heroHeight) / 2 - 22.dp
        val micOffset = 22.dp * s

        Orbit(
            voice = voice,
            voiceSpring = voiceSpring,
            radius = radius,
            t = t,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val e = enter()
                scaleX = lerp(0.3f, 1f, e)
                scaleY = lerp(0.3f, 1f, e)
                rotationZ = -120f * (1f - e)
                alpha = e.coerceIn(0f, 1f)
            }
        )
        MicButton(
            voice = voice,
            voiceSpring = voiceSpring,
            t = t,
            onTap = onMicTap,
            modifier = Modifier.offset(y = micOffset).graphicsLayer {
                val e = enter()
                scaleX = lerp(0.4f, 1f, e)
                scaleY = lerp(0.4f, 1f, e)
                alpha = e.coerceIn(0f, 1f)
            }
        )
        if (tapCount > 0) {
            key(tapCount) {
                ParticleBurst(voice.color, Modifier.offset(y = micOffset).size(190.dp))
            }
        }
        SpeechBubble(
            voice = voice,
            t = t,
            modifier = Modifier.offset(y = (-58).dp * s).graphicsLayer { alpha = enter().coerceIn(0f, 1f) }
        )
    }
}

@Composable
private fun Orbit(
    voice: HeroVoice,
    voiceSpring: IosSpring,
    radius: Dp,
    t: State<Float>,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = radius.toPx()
            rotate(degrees = Math.toDegrees(t.value * 0.25).toFloat()) {
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(SysCyan.copy(alpha = 0.35f), SysPurple.copy(alpha = 0.35f)),
                        startY = center.y - r,
                        endY = center.y + r
                    ),
                    radius = r,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
        }
        OrbitEmojis.forEachIndexed { i, emoji ->
            val active = emoji == voice.emoji
            val scale by animateFloatAsState(if (active) 1.3f else 0.9f, voiceSpring.spec(), label = "orbitScale")
            val fill by animateColorAsState(
                if (active) voice.color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f),
                voiceSpring.spec(),
                label = "orbitFill"
            )
            val border by animateColorAsState(
                if (active) voice.color else Color.White.copy(alpha = 0.12f),
                voiceSpring.spec(),
                label = "orbitBorder"
            )
            val glowColor by animateColorAsState(
                voice.color.copy(alpha = if (active) 0.8f else 0f),
                voiceSpring.spec(),
                label = "orbitGlow"
            )
            Box(
                modifier = Modifier
                    .offset {
                        val a = i / 12f * 2f * PI.toFloat() + t.value * 0.25f
                        val r = radius.toPx()
                        IntOffset((cos(a) * r).roundToInt(), (sin(a) * r).roundToInt())
                    }
                    .size(38.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .glow(glowColor, 10.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(if (active) 2.dp else 1.dp, border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun MicButton(
    voice: HeroVoice,
    voiceSpring: IosSpring,
    t: State<Float>,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(voice.color, voiceSpring.spec(), label = "micColor")
    val interaction = remember { MutableInteractionSource() }

    Box(modifier.size(150.dp), contentAlignment = Alignment.Center) {
        // Glow
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pulse = (sin(2.2f * t.value) + 1f) / 2f
                    scaleX = 1f + 0.12f * pulse
                    scaleY = 1f + 0.12f * pulse
                }
                .blur(10.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        0.4f to color.copy(alpha = 0.45f),
                        0.7f to SysPurple.copy(alpha = 0.25f),
                        1f to Color.Transparent
                    ),
                    CircleShape
                )
        )
        // Core
        Box(
            modifier = Modifier
                .size(96.dp)
                .glow(color.copy(alpha = 0.6f), 18.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), SysPurple.copy(alpha = 0.3f))))
                .border(2.dp, Brush.linearGradient(listOf(color, SysPurple)), CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onTap)
                .semantics {
                    contentDescription = "Tap to transform your voice"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        // Voice badge
        AnimatedContent(
            targetState = voice,
            modifier = Modifier.offset(36.dp, 34.dp),
            transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
            label = "voiceBadge"
        ) { v ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.5.dp, v.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(v.emoji, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ParticleBurst(color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(700, easing = EaseOutIos)) }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val dist = 85.dp.toPx() * p
        val dotR = 3.5.dp.toPx() * (1f - 0.6f * p)
        repeat(14) { i ->
            val a = i / 14f * 2f * PI.toFloat()
            drawCircle(
                color = if (i % 2 == 0) color else Color.White,
                radius = dotR,
                center = center + Offset(cos(a) * dist, sin(a) * dist),
                alpha = 1f - p
            )
        }
    }
}

private object BubbleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tail = with(density) { 8.dp.toPx() }
        val corner = with(density) { 16.dp.toPx() }
        val bodyBottom = size.height - tail
        val body = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, bodyBottom, CornerRadius(corner)))
        }
        val triangle = Path().apply {
            moveTo(size.width / 2 - tail, bodyBottom - 1f)
            lineTo(size.width / 2, size.height)
            lineTo(size.width / 2 + tail, bodyBottom - 1f)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, triangle))
    }
}

@Composable
private fun SpeechBubble(voice: HeroVoice, t: State<Float>, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = voice,
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
        transitionSpec = {
            val origin = TransformOrigin(0.5f, 1f)
            (scaleIn(initialScale = 0.6f, transformOrigin = origin) + fadeIn()) togetherWith
                (scaleOut(targetScale = 0.6f, transformOrigin = origin) + fadeOut())
        },
        label = "bubble"
    ) { v ->
        Box(
            modifier = Modifier
                .height(60.dp)
                .widthIn(min = 130.dp)
                .glow(v.color.copy(alpha = 0.5f), 10.dp, BubbleShape)
                .background(v.color.copy(alpha = 0.12f), BubbleShape)
                .background(Color.White.copy(alpha = 0.04f), BubbleShape)
                .border(1.5.dp, v.color.copy(alpha = 0.85f), BubbleShape)
                .padding(start = 18.dp, end = 18.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            StyledHello(v, t)
        }
    }
}

// endregion

// region 1.7 Styled "Hello"

@Composable
private fun StyledHello(voice: HeroVoice, t: State<Float>) {
    val c = voice.color
    val density = LocalDensity.current
    when (voice) {
        HeroVoice.Robot -> Box {
            val style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            val glitch: Density.() -> Float = { if (sin(23f * t.value) > 0.85f) 2.5.dp.toPx() else 0f }
            Text("HELLO.", style = style, color = SysCyan.copy(alpha = 0.7f),
                modifier = Modifier.graphicsLayer { translationX = -glitch() })
            Text("HELLO.", style = style, color = SysRed.copy(alpha = 0.6f),
                modifier = Modifier.graphicsLayer { translationX = glitch() })
            Text("HELLO.", style = style, color = c)
        }
        HeroVoice.Chipmunk -> Row {
            "hello!!".forEachIndexed { i, ch ->
                Text(
                    ch.toString(), fontSize = 22.sp, fontWeight = FontWeight.W800, color = c,
                    modifier = Modifier.graphicsLayer { translationY = sin(12f * t.value + 0.8f * i) * 4.dp.toPx() }
                )
            }
        }
        HeroVoice.Demon -> Text(
            text = "HELLO",
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.W900,
                brush = Brush.verticalGradient(listOf(DemonRed, DemonText2)),
                shadow = Shadow(DemonRed.copy(alpha = 0.8f), blurRadius = with(density) { 6.dp.toPx() })
            ),
            modifier = Modifier.graphicsLayer {
                translationX = sin(37f * t.value) * 1.5.dp.toPx()
                translationY = cos(29f * t.value) * 1.dp.toPx()
            }
        )
        HeroVoice.Echo -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hello", Modifier.alignByBaseline(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c)
            Text("hello", Modifier.alignByBaseline(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.copy(alpha = 0.55f))
            Text("hello", Modifier.alignByBaseline(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.copy(alpha = 0.25f))
        }
        HeroVoice.Helium -> Row {
            "hello~".forEachIndexed { i, ch ->
                Text(
                    ch.toString(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c,
                    modifier = Modifier.graphicsLayer {
                        translationY = -abs(sin(3f * t.value + 0.6f * i)) * 7.dp.toPx()
                        rotationZ = sin(2f * t.value + i) * 8f
                    }
                )
            }
        }
        HeroVoice.Ghost -> Text(
            text = "hellooo…",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = c,
            modifier = Modifier.graphicsLayer {
                val w = (sin(2.5f * t.value) + 1f) / 2f
                alpha = 0.55f + 0.45f * w
                translationY = sin(1.5f * t.value) * 3.dp.toPx()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val r = (0.3f + 0.7f * w).dp.toPx()
                    renderEffect = BlurEffect(r, r, TileMode.Decal)
                }
            }
        )
    }
}

// endregion

// region 1.8 Voice transform row + 1.9 Value chips

@Composable
private fun VoiceTransformRow(
    voice: HeroVoice,
    voiceSpring: IosSpring,
    t: State<Float>,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(voice.color, voiceSpring.spec(), label = "waveColor")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlainWave(t, Modifier.fillMaxWidth().height(24.dp))
            Text("Your voice", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.6f))
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EffectWaveform(voice.style, color, voice.style.secondary, t, Modifier.fillMaxWidth().height(24.dp))
            Crossfade(targetState = voice, label = "voiceLabel") { v ->
                Text("${v.emoji} ${v.label}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = v.color)
            }
        }
    }
}

@Composable
private fun PlainWave(t: State<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * 19) / 20
        val minH = 3.dp.toPx()
        for (i in 0 until 20) {
            val x = i / 19f
            val level = 0.25f + 0.25f * abs(sin(7f * x + 2f * t.value))
            val h = maxOf(minH, size.height * level)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2)
            )
        }
    }
}

@Composable
private fun ValueChips(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ValueChip(Icons.Filled.AutoAwesome, "30 Effects")
        ValueChip(Icons.Filled.Speaker, "Any Speaker")
        ValueChip(Icons.Filled.Bolt, "Zero Setup")
    }
}

@Composable
private fun ValueChip(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialFill)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SysCyan, modifier = Modifier.size(12.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, softWrap = false)
    }
}

// endregion
