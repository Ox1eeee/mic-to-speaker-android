package com.bluetooth.bluetoothmictospeaker.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.first

/**
 * Decides when to show the Google Play in-app review card.
 *
 * Strategy:
 *  - Ask right after a positive moment: the user stops the mic after a real session
 *    (>= [MIN_SESSION_MS]) and has had at least [MIN_SUCCESSFUL_SESSIONS] of them, or right after
 *    a successful purchase/restore.
 *  - Never at launch or mid-session, at most once per app process, and at most once every
 *    [COOLDOWN_MS] (Play enforces its own quota, so earlier retries would just waste the moment).
 *  - No "Do you like the app?" pre-prompt: Play policy forbids asking anything before the review card.
 */
class RatingManager(context: Context, private val preferencesManager: PreferencesManager) {

    private val appContext = context.applicationContext
    private val reviewManager = ReviewManagerFactory.create(appContext)
    private var cachedReviewInfo: ReviewInfo? = null
    private var promptedThisProcess = false

    /** Call when a mic session starts; pre-fetches the review info so the card appears instantly later. */
    suspend fun onSessionStarted() {
        if (cachedReviewInfo != null || !isEligible()) return
        cachedReviewInfo = runCatching { reviewManager.requestReview() }
            .onFailure { Log.w(TAG, "Review prefetch failed: ${it.message}") }
            .getOrNull()
    }

    /** Call when a mic session ends. [allowPrompt] = false when another screen (paywall, settings) is on top. */
    suspend fun onSessionEnded(activity: Activity, durationMs: Long, allowPrompt: Boolean) {
        if (durationMs < MIN_SESSION_MS) return
        val sessions = preferencesManager.incrementSuccessfulSessions()
        if (allowPrompt && sessions >= MIN_SUCCESSFUL_SESSIONS) maybeLaunch(activity, trigger = "session_end")
    }

    /** Call after a successful purchase or restore. */
    suspend fun onPurchaseCompleted(activity: Activity) {
        maybeLaunch(activity, trigger = "purchase")
    }

    /** Settings → "Rate app": always open the Play Store listing (the in-app card may be quota-limited). */
    fun openStoreListing(activity: Activity) {
        val pkg = activity.packageName
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .setPackage("com.android.vending")
            )
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            )
        }
        logEvent("review_store_opened", "settings")
    }

    private suspend fun isEligible(): Boolean {
        if (promptedThisProcess) return false
        if (!preferencesManager.onboardingCompleted.first()) return false
        val last = preferencesManager.lastReviewPromptMs.first()
        return last == 0L || System.currentTimeMillis() - last >= COOLDOWN_MS
    }

    private suspend fun maybeLaunch(activity: Activity, trigger: String) {
        if (!isEligible() || activity.isFinishing) return
        promptedThisProcess = true
        try {
            val info = cachedReviewInfo ?: reviewManager.requestReview()
            cachedReviewInfo = null
            preferencesManager.setLastReviewPrompt(System.currentTimeMillis())
            logEvent("review_prompt_requested", trigger)
            reviewManager.launchReview(activity, info)
        } catch (e: Exception) {
            Log.e(TAG, "In-app review failed: ${e.message}")
        }
    }

    private fun logEvent(name: String, trigger: String) {
        FirebaseAnalytics.getInstance(appContext).logEvent(name, bundleOf("trigger" to trigger))
    }

    companion object {
        private const val TAG = "RatingManager"
        private const val MIN_SESSION_MS = 30_000L
        private const val MIN_SUCCESSFUL_SESSIONS = 2
        private const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000
    }
}
