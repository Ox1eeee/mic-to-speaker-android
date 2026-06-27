package com.bluetooth.bluetoothmictospeaker.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bluetooth.bluetoothmictospeaker.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _onboardingCompleted = MutableStateFlow(true) // default true to avoid flash
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    init {
        viewModelScope.launch {
            _onboardingCompleted.value = preferencesManager.onboardingCompleted.first()
        }
    }

    fun setPage(page: Int) {
        _currentPage.value = page
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            _onboardingCompleted.value = true
        }
    }
}
