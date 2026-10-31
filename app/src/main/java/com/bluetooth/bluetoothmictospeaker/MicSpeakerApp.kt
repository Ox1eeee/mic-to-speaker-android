package com.bluetooth.bluetoothmictospeaker

import android.app.Application
import com.bluetooth.bluetoothmictospeaker.ads.InterstitialAdManager
import com.bluetooth.bluetoothmictospeaker.billing.RevenueCatManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MicSpeakerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RevenueCatManager.configure(this)
        // MobileAds.initialize is heavy; run it off the main thread so it doesn't delay the first screen.
        // The completion listener is delivered on the main thread, where ads must be loaded.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MicSpeakerApp) {
                InterstitialAdManager.loadAd(this@MicSpeakerApp)
            }
        }
    }
}
