package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import android.graphics.BlurMaskFilter
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

// region Colours (instructions 0.4)

internal val NeonCyan = Color(0xFF00F2FF)
internal val NeonPurple = Color(0xFF8A2BE3)
internal val Accent = Color(0xFF5B13EC)
internal val SysCyan = Color(0xFF64D2FF)
internal val SysPurple = Color(0xFFBF5AF2)
internal val SysYellow = Color(0xFFFFD60A)
internal val SysOrange = Color(0xFFFF9F0A)
internal val SysGreen = Color(0xFF30D158)
internal val SysTeal = Color(0xFF40C8E0)
internal val SysIndigo = Color(0xFF5E5CE6)
internal val SysRed = Color(0xFFFF453A)
internal val RobotGreen = Color(0xFF33FF99)
internal val DemonRed = Color(0xFFFF3340)
internal val DemonDeep = Color(0xFF730099)
internal val DemonText2 = Color(0xFF66004D)
internal val EchoBlue = Color(0xFF598CFF)
internal val HeliumPink = Color(0xFFFF73CC)
internal val GhostBlue = Color(0xFFCCD9FF)
internal val StageTop = Color(0xFF120829)
internal val MaterialFill = Color.White.copy(alpha = 0.10f)

internal val OnboardingBackground = Brush.verticalGradient(listOf(Color(0xFF2A1A4A), Color(0xFF161022)))

// endregion

// region Animation helpers (instructions 0.7)

internal val EaseInOutIos = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
internal val EaseOutIos = CubicBezierEasing(0f, 0f, 0.58f, 1f)

internal data class IosSpring(val response: Float, val damping: Float = 0.825f) {
    fun <T> spec(): SpringSpec<T> {
        val w = (2 * PI / response).toFloat()
        return spring(dampingRatio = damping, stiffness = w * w)
    }
}

internal fun frac(x: Float): Float = x - floor(x)

internal fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f

@Composable
internal fun rememberSeconds(paused: Boolean = false): State<Float> = produceState(0f, paused) {
    if (paused) {
        value = 0f
        return@produceState
    }
    val start = withFrameNanos { it }
    while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
}

@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
internal fun ColumnScope.FlexGap(min: Dp) {
    Spacer(Modifier.height(min))
    Spacer(Modifier.weight(1f))
}

// endregion

// region Glow (iOS coloured shadow)

private fun blurPaint(color: Color, blur: Float) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(blur.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
    }

/** Soft coloured halo behind the element's [shape], like an iOS `.shadow(color:radius:)`. */
internal fun Modifier.glow(
    color: Color,
    radius: Dp,
    shape: Shape = CircleShape,
    offsetY: Dp = 0.dp
): Modifier = if (color.alpha <= 0f) this else drawWithCache {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        .asAndroidPath()
    val paint = blurPaint(color, radius.toPx())
    val dy = offsetY.toPx()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(0f, dy)
            canvas.nativeCanvas.drawPath(path, paint)
            canvas.nativeCanvas.restore()
        }
    }
}

internal fun DrawScope.drawGlowCircle(color: Color, center: Offset, radius: Float, blur: Float) {
    if (color.alpha <= 0f) return
    val paint = blurPaint(color, blur)
    drawIntoCanvas { it.nativeCanvas.drawCircle(center.x, center.y, radius, paint) }
}

internal fun DrawScope.drawGlowRoundRect(color: Color, topLeft: Offset, size: Size, corner: Float, blur: Float) {
    if (color.alpha <= 0f) return
    val paint = blurPaint(color, blur)
    drawIntoCanvas {
        it.nativeCanvas.drawRoundRect(
            topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height, corner, corner, paint
        )
    }
}

// endregion

/** Single-line text that steps its font size down (to [minScale]) until it fits the width. */
@Composable
internal fun AutoSizeText(
    text: String,
    style: TextStyle,
    minScale: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    textAlign: TextAlign = TextAlign.Center
) {
    var scale by remember(text) { mutableFloatStateOf(1f) }
    var ready by remember(text) { mutableStateOf(false) }
    Text(
        text = text,
        color = color,
        style = style.copy(fontSize = style.fontSize * scale),
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && scale > minScale) {
                scale = (scale - 0.04f).coerceAtLeast(minScale)
            } else {
                ready = true
            }
        }
    )
}

/** Primary "Continue" button (instructions 0.8). Pass [shimmerClock] for the Screen 1 shimmer (1.11). */
@Composable
internal fun ContinueButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shimmerClock: State<Float>? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "continuePress")

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (pressed) 0.9f else 1f
            }
            .glow(Accent.copy(alpha = 0.35f), 18.dp, CircleShape, offsetY = 10.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
            .drawWithContent {
                drawContent()
                val clock = shimmerClock ?: return@drawWithContent
                val w = size.width
                val bandW = 0.35f * w
                val phase = frac(clock.value / 3f)
                val x = -bandW + min(1f, phase / 0.4f) * 1.35f * w
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        startX = x,
                        endX = x + bandW
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(bandW, size.height)
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// region Effect waveform (instructions 1.13)

internal enum class EffectStyle(val primary: Color, val secondary: Color) {
    AUTOTUNE(SysCyan, SysPurple),
    ROBOT(RobotGreen, SysTeal),
    CHIPMUNK(SysYellow, SysOrange),
    DEMON(DemonRed, DemonDeep),
    ECHO(EchoBlue, SysIndigo);

    fun level(x: Double, t: Double): Double = when (this) {
        AUTOTUNE -> {
            val snap = floor(t * 4) / 4
            val v = 0.5 + 0.5 * sin(x * 12 + snap * 5)
            0.25 + 0.75 * v * (0.7 + 0.3 * sin(t * 6))
        }
        ROBOT -> {
            val tq = floor(t * 6) / 6
            val v = abs(sin(x * 9 + tq * 4)) * 0.7 + abs(sin(x * 23 - tq * 3)) * 0.3
            round(v * 4) / 4 * 0.85 + 0.15
        }
        CHIPMUNK -> {
            val v = abs(sin(x * 30 + t * 18)) * 0.6 + abs(sin(x * 57 - t * 25)) * 0.4
            0.15 + 0.7 * v
        }
        DEMON -> 0.3 + 0.7 * abs(sin(x * 4 + t * 2.2))
        ECHO -> {
            val p = (t * 0.9) % 1.6
            val v = exp(-abs(x - p) * 7) + 0.55 * exp(-abs(x - p + 0.3) * 7) +
                0.3 * exp(-abs(x - p + 0.6) * 7)
            0.12 + 0.85 * min(1.0, v)
        }
    }
}

@Composable
internal fun EffectWaveform(
    style: EffectStyle,
    primary: Color,
    secondary: Color,
    t: State<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * 31) / 32
        val time = t.value.toDouble()
        val minH = 4.dp.toPx()
        for (i in 0 until 32) {
            val x = i / 31.0
            val envelope = 0.35 + 0.65 * sin(PI * x)
            val h = max(minH, (size.height * style.level(x, time) * envelope).toFloat())
            val left = i * (barW + gap)
            val top = (size.height - h) / 2
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(primary, secondary), startY = top, endY = top + h),
                topLeft = Offset(left, top),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2)
            )
        }
    }
}

// endregion

/** Bluetooth speaker drawing (instructions 2.8). Shared by Screen 2 and Screen 3. */
@Composable
internal fun BluetoothSpeaker(
    width: Dp,
    accent: Color,
    beatRate: Float,
    isConnected: Boolean,
    isPlaying: Boolean,
    t: State<Float>,
    modifier: Modifier = Modifier
) {
    val beat: () -> Float = {
        if (isPlaying) max(0f, sin(PI.toFloat() * beatRate * t.value)).pow(6) else 0f
    }
    Canvas(
        modifier
            .size(width, width * 1.55f)
            .graphicsLayer {
                val s = 1f + 0.03f * beat()
                scaleX = s
                scaleY = s
            }
            .glow(if (isPlaying) accent.copy(alpha = 0.35f) else Color.Transparent, 18.dp, RoundedCornerShape(width * 0.2f))
    ) {
        val b = beat()
        val w = size.width
        val corner = CornerRadius(0.2f * w)

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF333333), Color(0xFF121212))),
            cornerRadius = corner
        )
        val borderW = 1.5.dp.toPx()
        drawRoundRect(
            color = accent.copy(alpha = if (isPlaying) 0.7f else 0.25f),
            topLeft = Offset(borderW / 2, borderW / 2),
            size = Size(w - borderW, size.height - borderW),
            cornerRadius = corner,
            style = Stroke(borderW)
        )

        val spacing = 0.08f * w
        val tweeter = 0.2f * w
        val wf = 0.72f * w
        val led = max(4.dp.toPx(), 0.07f * w)
        var y = (size.height - (tweeter + wf + led + spacing * 2)) / 2
        val cx = w / 2

        // Tweeter
        val tc = Offset(cx, y + tweeter / 2)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF595959), Color(0xFF1A1A1A)), center = tc, radius = 0.1f * w),
            radius = tweeter / 2,
            center = tc
        )
        drawCircle(Color.White.copy(alpha = 0.15f), tweeter / 2, tc, style = Stroke(1.dp.toPx()))
        y += tweeter + spacing

        // Woofer
        val wc = Offset(cx, y + wf / 2)
        drawCircle(Color(0xFF0D0D0D), wf / 2, wc)
        val ringW = 2.dp.toPx()
        drawCircle(accent.copy(alpha = (0.35f + 0.6f * b).coerceIn(0f, 1f)), wf / 2 - ringW / 2, wc, style = Stroke(ringW))
        val coneScale = 1f + 0.12f * b
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF4D4D4D), Color(0xFF141414)), center = wc, radius = 0.4f * wf * coneScale
            ),
            radius = 0.39f * wf * coneScale,
            center = wc
        )
        val capR = 0.13f * wf * (1f + 0.25f * b)
        drawGlowCircle(accent.copy(alpha = b.coerceIn(0f, 1f)), wc, capR, 8.dp.toPx())
        drawCircle(
            brush = Brush.linearGradient(
                listOf(accent.copy(alpha = 0.9f), SysPurple.copy(alpha = 0.8f)),
                start = Offset(wc.x - capR, wc.y - capR),
                end = Offset(wc.x + capR, wc.y + capR)
            ),
            radius = capR,
            center = wc
        )
        y += wf + spacing

        // LED
        val lc = Offset(cx, y + led / 2)
        if (isConnected) {
            drawGlowCircle(accent, lc, led / 2, 4.dp.toPx())
            drawCircle(accent, led / 2, lc)
        } else {
            drawCircle(Color.White.copy(alpha = 0.25f), led / 2, lc)
        }
    }
}
