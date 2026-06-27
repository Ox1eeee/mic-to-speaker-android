package com.bluetooth.bluetoothmictospeaker.utils

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first

class RatingManager(private val preferencesManager: PreferencesManager) {

    private var hasPromptedThisSession = false

    suspend fun shouldPromptAfterOnboarding(): Boolean {
        if (hasPromptedThisSession) return false
        return true
    }

    suspend fun shouldPromptOnLaunch(): Boolean {
        if (hasPromptedThisSession) return false
        val count = preferencesManager.launchCount.first()
        return count == 2
    }

    fun requestReview(activity: Activity, forceShow: Boolean = false) {
        if (hasPromptedThisSession && !forceShow) return
        if (!forceShow) {
            hasPromptedThisSession = true
        }

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                Log.d(TAG, "Review flow request successful, launching review")
                manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { launchTask ->
                    if (launchTask.isSuccessful) {
                        Log.d(TAG, "Review flow completed successfully")
                    } else {
                        Log.e(TAG, "Review flow launch failed: ${launchTask.exception?.message}")
                    }
                }
            } else {
                Log.e(TAG, "Review request failed: ${task.exception?.message}")
            }
        }
    }

    companion object {
        private const val TAG = "RatingManager"
    }
}
