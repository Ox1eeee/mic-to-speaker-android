package com.bluetooth.bluetoothmictospeaker

import android.app.Application
import com.bluetooth.bluetoothmictospeaker.ads.InterstitialAdManager
import com.bluetooth.bluetoothmictospeaker.billing.RevenueCatManager
import com.google.android.gms.ads.MobileAds

class MicSpeakerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        RevenueCatManager.configure(this)
        MobileAds.initialize(this)
        InterstitialAdManager.loadAd(this)
    }
}
