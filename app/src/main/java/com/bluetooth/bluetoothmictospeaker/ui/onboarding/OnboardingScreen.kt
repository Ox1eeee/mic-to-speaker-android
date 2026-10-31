package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 3-page onboarding matching the iOS app (see instructions.md).
 * No swiping, no page dots: only the Continue button moves forward.
 * Only the current page is composed, so leaving a page stops its timers and sounds.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = page > 0) { page-- }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingBackground)
    ) {
        when (page) {
            0 -> HeroPage(onContinue = { page = 1 })
            1 -> ConnectPage(onContinue = { page = 2 })
            else -> KaraokePage(onContinue = onComplete)
        }
    }
}
