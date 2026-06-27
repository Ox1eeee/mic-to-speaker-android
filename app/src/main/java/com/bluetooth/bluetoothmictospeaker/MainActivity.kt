package com.bluetooth.bluetoothmictospeaker

import android.os.Bundle
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val audioViewModel: AudioViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()

    private var showSettings by mutableStateOf(false)
    private var showPaywall by mutableStateOf(false)
    private var hasShownPaywallThisSession by mutableStateOf(false)

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var ratingManager: RatingManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        audioViewModel.setPermissionGranted(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(this)
        ratingManager = RatingManager(preferencesManager)

        val hasPermission = PermissionHelper.hasAllPermissions(this)
        audioViewModel.setPermissionGranted(hasPermission)

        // Track launch count
        lifecycleScope.launch {
            preferencesManager.incrementLaunchCount()
            if (ratingManager.shouldPromptOnLaunch()) {
                ratingManager.requestReview(this@MainActivity)
            }
        }

        setContent {
            BluetoothMicToSpeakerTheme {
                val onboardingCompleted by onboardingViewModel.onboardingCompleted.collectAsState()
                val isPro by RevenueCatManager.isPro.collectAsState()

                // Auto-show paywall for free users after onboarding or on app launch
                LaunchedEffect(onboardingCompleted, isPro) {
                    if (onboardingCompleted && !isPro && !hasShownPaywallThisSession) {
                        subscriptionViewModel.loadOfferings()
                        showPaywall = true
                        hasShownPaywallThisSession = true
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        !onboardingCompleted -> {
                            OnboardingScreen(
                                onComplete = {
                                    onboardingViewModel.completeOnboarding()
                                },
                                onRequestPermissions = {
                                    permissionLauncher.launch(
                                        PermissionHelper.getRequiredPermissions().toTypedArray()
                                    )
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        showPaywall -> {
                            PaywallScreen(
                                subscriptionViewModel = subscriptionViewModel,
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
                                    ratingManager.requestReview(this@MainActivity, forceShow = true)
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
                                isPro = isPro,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}