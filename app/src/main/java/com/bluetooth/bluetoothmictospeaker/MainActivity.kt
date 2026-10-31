package com.bluetooth.bluetoothmictospeaker

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.bluetooth.bluetoothmictospeaker.ads.InterstitialAdManager
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.billing.RevenueCatManager
import com.bluetooth.bluetoothmictospeaker.billing.SubscriptionViewModel
import com.bluetooth.bluetoothmictospeaker.ui.home.HomeScreen
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.OnboardingScreen
import com.bluetooth.bluetoothmictospeaker.ui.onboarding.OnboardingViewModel
import com.bluetooth.bluetoothmictospeaker.ui.paywall.PaywallScreen
import com.bluetooth.bluetoothmictospeaker.ui.settings.SettingsScreen
import com.bluetooth.bluetoothmictospeaker.ui.theme.BluetoothMicToSpeakerTheme
import com.bluetooth.bluetoothmictospeaker.utils.PermissionHelper
import com.bluetooth.bluetoothmictospeaker.utils.PreferencesManager
import com.bluetooth.bluetoothmictospeaker.utils.RatingManager
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bluetooth.bluetoothmictospeaker.audio.MicSessionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val FREE_LAUNCH_LIMIT = 3
        private const val REVIEW_PROMPT_DELAY_MS = 600L
    }

    private val audioViewModel: AudioViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()

    private var showSettings by mutableStateOf(false)
    private var showPaywall by mutableStateOf(false)
    private var hasShownPaywallThisSession by mutableStateOf(false)
    private var launchCount by mutableIntStateOf(0)

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var ratingManager: RatingManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        audioViewModel.setPermissionGranted(PermissionHelper.hasAllPermissions(this))
        if (PermissionHelper.hasAudioPermission(this)) audioViewModel.toggleAudio()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(this)
        ratingManager = RatingManager(this, preferencesManager)

        val hasPermission = PermissionHelper.hasAllPermissions(this)
        audioViewModel.setPermissionGranted(hasPermission)

        // Track launch count
        lifecycleScope.launch {
            preferencesManager.incrementLaunchCount()
            launchCount = preferencesManager.launchCount.first()
        }

        // In-app review: ask after a real mic session ends (see RatingManager for the strategy)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioViewModel.sessionEvents.collect { event ->
                    when (event) {
                        MicSessionEvent.Started -> ratingManager.onSessionStarted()
                        is MicSessionEvent.Ended -> {
                            delay(REVIEW_PROMPT_DELAY_MS)
                            ratingManager.onSessionEnded(
                                activity = this@MainActivity,
                                durationMs = event.durationMs,
                                allowPrompt = !showPaywall && !showSettings
                            )
                        }
                    }
                }
            }
        }

        // Keep the system splash on screen until we know whether to show onboarding or home
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (onboardingViewModel.onboardingCompleted.value == null) return false
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })

        setContent {
            BluetoothMicToSpeakerTheme {
                val onboardingState by onboardingViewModel.onboardingCompleted.collectAsState()
                val isPro by RevenueCatManager.isPro.collectAsState()

                // Auto-show paywall for free users after onboarding or on app launch
                LaunchedEffect(onboardingState, isPro) {
                    if (onboardingState == true && !isPro && !hasShownPaywallThisSession) {
                        subscriptionViewModel.loadOfferings()
                        showPaywall = true
                        hasShownPaywallThisSession = true
                    }
                }

                // After a successful purchase/restore: close the paywall, then ask for a review
                val purchaseSuccess by remember {
                    subscriptionViewModel.uiState.map { it.purchaseSuccess }.distinctUntilChanged()
                }.collectAsState(initial = false)
                LaunchedEffect(purchaseSuccess) {
                    if (purchaseSuccess) {
                        subscriptionViewModel.consumePurchaseSuccess()
                        showPaywall = false
                        delay(REVIEW_PROMPT_DELAY_MS)
                        ratingManager.onPurchaseCompleted(this@MainActivity)
                    }
                }

                if (onboardingState == false) {
                    OnboardingScreen(
                        onComplete = { onboardingViewModel.completeOnboarding() }
                    )
                } else if (onboardingState == true) Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        showPaywall -> {
                            PaywallScreen(
                                subscriptionViewModel = subscriptionViewModel,
                                dismissable = launchCount <= FREE_LAUNCH_LIMIT,
                                onDismiss = {
                                    InterstitialAdManager.trackActionAndShowAd(this@MainActivity, isPro = isPro) {
                                        showPaywall = false
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        showSettings -> {
                            SettingsScreen(
                                audioViewModel = audioViewModel,
                                onBack = {
                                    InterstitialAdManager.trackActionAndShowAd(this@MainActivity, isPro = isPro) {
                                        showSettings = false
                                    }
                                },
                                onRateApp = {
                                    ratingManager.openStoreListing(this@MainActivity)
                                },
                                onUpgrade = {
                                    subscriptionViewModel.loadOfferings()
                                    showPaywall = true
                                },
                                isPro = isPro,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        else -> {
                            HomeScreen(
                                viewModel = audioViewModel,
                                onSettingsClick = {
                                    InterstitialAdManager.trackActionAndShowAd(this@MainActivity, isPro = isPro) {
                                        showSettings = true
                                    }
                                },
                                onUpgradeClick = {
                                    subscriptionViewModel.loadOfferings()
                                    showPaywall = true
                                },
                                onRequestPermissions = {
                                    permissionLauncher.launch(
                                        PermissionHelper.getRequiredPermissions().toTypedArray()
                                    )
                                },
                                isPro = isPro
                            )
                        }
                    }
                }
            }
        }
    }
}