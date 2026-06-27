package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.audio.AudioPreviewManager
import com.bluetooth.bluetoothmictospeaker.audio.VoiceEffect
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurple
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentPurpleLight
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkBackground
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurface
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkSurfaceVariant
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassBorder
import com.bluetooth.bluetoothmictospeaker.ui.theme.GlassWhite
import com.bluetooth.bluetoothmictospeaker.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val accentPrimary: Color,
    val accentSecondary: Color
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Mic,
        title = "Mic Speaker",
        subtitle = "Real-Time Voice FX",
        description = "Transform your voice instantly with stunning effects.\nRoute audio to any speaker with zero latency.",
        accentPrimary = AccentCyan,
        accentSecondary = AccentPurple
    ),
    OnboardingPage(
        icon = Icons.Default.GraphicEq,
        title = "How It Works",
        subtitle = "Simple as 1-2-3",
        description = "Mic captures your voice\nEffects are applied in real-time\nTransformed audio plays through your speaker",
        accentPrimary = AccentPurpleLight,
        accentSecondary = AccentCyan
    ),
    OnboardingPage(
        icon = Icons.Default.MusicNote,
        title = "Voice Effects",
        subtitle = "7 Unique Sounds",
        description = "Robot, Chipmunk, Deep Voice, Echo, Alien, Radio and more.\nTap below to hear a live preview!",
        accentPrimary = AccentCyan,
        accentSecondary = AccentPurpleLight
    ),
    OnboardingPage(
        icon = Icons.Default.BluetoothAudio,
        title = "Any Speaker",
        subtitle = "Bluetooth & Wired",
        description = "Seamlessly connects to Bluetooth speakers, earphones,\nwired headsets and built-in speakers.",
        accentPrimary = AccentPurpleLight,
        accentSecondary = AccentPurple
    ),
    OnboardingPage(
        icon = Icons.Default.Favorite,
        title = "Help Us Grow",
        subtitle = "Solo Developer",
        description = "I'm a solo developer building this app — your review\nwould mean a lot and helps others find us.",
        accentPrimary = Color(0xFFFFB300),
        accentSecondary = Color(0xFFFF6D00)
    ),
    OnboardingPage(
        icon = Icons.Default.Security,
        title = "Almost There",
        subtitle = "Quick Permissions",
        description = "We need mic access to hear your voice and Bluetooth\nto connect wirelessly. Nothing is ever recorded.",
        accentPrimary = AccentCyan,
        accentSecondary = AccentPurple
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestReview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val previewManager = remember { AudioPreviewManager(context) }
    val isLastPage = pagerState.currentPage == pages.size - 1
    val currentPage = pages[pagerState.currentPage]

    DisposableEffect(Unit) {
        onDispose { previewManager.release() }
    }

    // Animated gradient orbs in the background
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnim")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "orbRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Ambient glow orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height * 0.35f
            val rad = Math.toRadians(orbOffset.toDouble())

            // Primary orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentPage.accentPrimary.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(
                        cx + (cos(rad) * size.width * 0.15f).toFloat(),
                        cy + (sin(rad) * size.height * 0.06f).toFloat()
                    ),
                    radius = size.width * 0.55f
                ),
                center = Offset(
                    cx + (cos(rad) * size.width * 0.15f).toFloat(),
                    cy + (sin(rad) * size.height * 0.06f).toFloat()
                ),
                radius = size.width * 0.55f
            )

            // Secondary orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentPage.accentSecondary.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(
                        cx - (cos(rad + 2.0) * size.width * 0.2f).toFloat(),
                        cy + size.height * 0.18f + (sin(rad + 1.5) * size.height * 0.04f).toFloat()
                    ),
                    radius = size.width * 0.45f
                ),
                center = Offset(
                    cx - (cos(rad + 2.0) * size.width * 0.2f).toFloat(),
                    cy + size.height * 0.18f + (sin(rad + 1.5) * size.height * 0.04f).toFloat()
                ),
                radius = size.width * 0.45f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pager (swipe disabled — navigation via button only)
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    2 -> EffectPreviewPage(
                        pageData = pages[page],
                        previewManager = previewManager
                    )
                    4 -> RatingReviewPage(pageData = pages[page])
                    else -> OnboardingPageContent(page = pages[page])
                }
            }

            // Page indicators - pill style
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateFloatAsState(
                        targetValue = if (isSelected) 28f else 8f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = width.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(
                                    listOf(currentPage.accentPrimary, currentPage.accentSecondary)
                                )
                                else Brush.horizontalGradient(
                                    listOf(DarkSurfaceVariant, DarkSurfaceVariant)
                                )
                            )
                    )
                }
            }

            // CTA button with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AccentPurple, currentPage.accentPrimary)
                        )
                    )
                    .clickable {
                        if (isLastPage) {
                            onRequestPermissions()
                            onComplete()
                        } else {
                            // Trigger in-app review when leaving the rating page
                            if (pagerState.currentPage == 4) {
                                onRequestReview()
                            }
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val iconScale = remember { Animatable(0.3f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, delayMillis = 200, easing = EaseInOut)
        )
    }

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringPulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow rings
        Box(contentAlignment = Alignment.Center) {
            // Outer pulsing ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(ringScale)
                    .alpha(ringAlpha)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(page.accentPrimary, Color.Transparent),
                                radius = size.minDimension / 2
                            ),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )

            // Middle ring
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(iconScale.value)
                    .drawBehind {
                        drawCircle(
                            color = page.accentPrimary.copy(alpha = 0.12f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
            )

            // Icon circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                page.accentPrimary.copy(alpha = 0.25f),
                                page.accentSecondary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                page.accentPrimary.copy(alpha = 0.6f),
                                page.accentSecondary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.accentPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Subtitle chip
        Box(
            modifier = Modifier
                .alpha(contentAlpha.value)
                .clip(RoundedCornerShape(20.dp))
                .background(page.accentPrimary.copy(alpha = 0.1f))
                .border(
                    1.dp,
                    page.accentPrimary.copy(alpha = 0.25f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = page.subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = page.accentPrimary,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.title,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.alpha(contentAlpha.value)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .alpha(contentAlpha.value)
        )
    }
}

@Composable
private fun RatingReviewPage(pageData: OnboardingPage) {
    val cardScale = remember { Animatable(0.3f) }
    val contentAlpha = remember { Animatable(0f) }
    val starAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, delayMillis = 200, easing = EaseInOut)
        )
    }
    LaunchedEffect(Unit) {
        delay(600)
        starAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = EaseInOut)
        )
    }

    // Floating animation for the card
    val infiniteTransition = rememberInfiniteTransition(label = "ratingFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardFloat"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = pageData.title,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.alpha(contentAlpha.value)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pageData.description,
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .alpha(contentAlpha.value)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Smiley card with amber/gold theme
        Box(
            modifier = Modifier
                .scale(cardScale.value)
                .offset(y = floatOffset.dp)
                .size(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFCA28),
                            Color(0xFFFFB300)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFD54F),
                            Color(0xFFFF8F00)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App icon text
                Text(
                    text = "MIC",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFE65100),
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Smiley face using text
                Text(
                    text = "\u2022 \u2022",
                    fontSize = 28.sp,
                    color = Color(0xFF5D4037),
                    letterSpacing = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Smile curve
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 20.dp)
                        .drawBehind {
                            drawArc(
                                color = Color(0xFF5D4037),
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Star rating display (animated)
        Row(
            modifier = Modifier.alpha(starAlpha.value),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .size(36.dp)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tap Continue to rate on Play Store",
            fontSize = 13.sp,
            color = TextSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(starAlpha.value)
        )
    }
}

@Composable
private fun EffectPreviewPage(
    pageData: OnboardingPage,
    previewManager: AudioPreviewManager
) {
    var selectedPreview by remember { mutableStateOf<VoiceEffect?>(null) }
    var visible by remember { mutableStateOf(false) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        visible = true
        contentAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseInOut))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "effectPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "effectRing"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with glow
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(ringScale)
                    .alpha(0.15f)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(pageData.accentPrimary, Color.Transparent),
                                radius = size.minDimension / 2
                            ),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                pageData.accentPrimary.copy(alpha = 0.25f),
                                pageData.accentSecondary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                pageData.accentPrimary.copy(alpha = 0.6f),
                                pageData.accentSecondary.copy(alpha = 0.2f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = pageData.icon,
                    contentDescription = null,
                    tint = pageData.accentPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .alpha(contentAlpha.value)
                .clip(RoundedCornerShape(20.dp))
                .background(pageData.accentPrimary.copy(alpha = 0.1f))
                .border(1.dp, pageData.accentPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = pageData.subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pageData.accentPrimary,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = pageData.title,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.alpha(contentAlpha.value)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = pageData.description,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .alpha(contentAlpha.value)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Effect preview grid
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 3 }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val effects = VoiceEffect.all().take(4)
                effects.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { effect ->
                            EffectPreviewChip(
                                effect = effect,
                                isPlaying = selectedPreview == effect,
                                accentColor = pageData.accentPrimary,
                                onClick = {
                                    if (selectedPreview == effect) {
                                        previewManager.stopPreview()
                                        selectedPreview = null
                                    } else {
                                        selectedPreview = effect
                                        previewManager.playEffectPreview(effect)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectPreviewChip(
    effect: VoiceEffect,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )

    Box(
        modifier = modifier
            .scale(chipScale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isPlaying) Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.2f), AccentPurple.copy(alpha = 0.12f))
                )
                else Brush.linearGradient(
                    listOf(GlassWhite, GlassWhite)
                )
            )
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                brush = if (isPlaying) Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.8f), AccentPurple.copy(alpha = 0.4f))
                )
                else Brush.linearGradient(
                    listOf(GlassBorder, GlassBorder)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isPlaying) accentColor else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = effect.displayName,
                color = if (isPlaying) Color.White else TextSecondary,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
