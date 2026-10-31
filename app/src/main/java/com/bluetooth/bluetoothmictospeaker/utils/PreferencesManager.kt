package com.bluetooth.bluetoothmictospeaker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mic_speaker_prefs")

class PreferencesManager(private val context: Context) {

    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val launchCountKey = intPreferencesKey("launch_count")
    private val lastSelectedEffectKey = intPreferencesKey("last_selected_effect")
    private val successfulSessionsKey = intPreferencesKey("successful_sessions")
    private val lastReviewPromptKey = longPreferencesKey("last_review_prompt_ms")

    val lastReviewPromptMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[lastReviewPromptKey] ?: 0L
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }

    val launchCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[launchCountKey] ?: 0
    }

    val lastSelectedEffect: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[lastSelectedEffectKey] ?: 0
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }

    suspend fun incrementLaunchCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[launchCountKey] ?: 0
            prefs[launchCountKey] = current + 1
        }
    }

    /** Increments and returns the number of successful mic sessions. */
    suspend fun incrementSuccessfulSessions(): Int {
        var count = 0
        context.dataStore.edit { prefs ->
            count = (prefs[successfulSessionsKey] ?: 0) + 1
            prefs[successfulSessionsKey] = count
        }
        return count
    }

    suspend fun setLastReviewPrompt(timeMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[lastReviewPromptKey] = timeMs
        }
    }

    suspend fun setLastSelectedEffect(effectIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[lastSelectedEffectKey] = effectIndex
        }
    }
}
