package com.bluetooth.bluetoothmictospeaker.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {

    private const val TAG = "InterstitialAdManager"

    // Test ad unit ID — replace with your real ad unit ID before release
    private const val AD_UNIT_ID = "ca-app-pub-9136368944341050/6379232434"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var actionCount = 0
    private const val SHOW_AD_EVERY_N_ACTIONS = 3

    fun loadAd(context: Context) {
        if (interstitialAd != null || isLoading) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showAdIfReady(activity: Activity, isPro: Boolean = false, onAdDismissed: () -> Unit = {}) {
        if (isPro) {
            onAdDismissed()
            return
        }
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    loadAd(activity)
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed")
                }
            }
            ad.show(activity)
        } else {
            loadAd(activity)
            onAdDismissed()
        }
    }

    fun trackActionAndShowAd(activity: Activity, isPro: Boolean = false, onAdDismissed: () -> Unit = {}) {
        if (isPro) {
            onAdDismissed()
            return
        }
        actionCount++
        if (actionCount >= SHOW_AD_EVERY_N_ACTIONS) {
            actionCount = 0
            showAdIfReady(activity, isPro = false, onAdDismissed = onAdDismissed)
        } else {
            onAdDismissed()
        }
    }
}
