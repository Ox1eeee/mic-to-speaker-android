package com.bluetooth.bluetoothmictospeaker.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bluetooth.bluetoothmictospeaker.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioUiState(
    val isRunning: Boolean = false,
    val currentEffect: VoiceEffect = VoiceEffect.None,
    val volume: Float = 0.8f,
    val echoMix: Float = 0.3f,
    val amplitude: Float = 0f,
    val currentRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val availableRoutes: List<AudioOutputRoute> = listOf(AudioOutputRoute.SPEAKER),
    val hasPermission: Boolean = false
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    val audioEngine = AudioEngine()
    val audioRouter = AudioRouter(application)
    private val preferencesManager = PreferencesManager(application)

    init {
        audioEngine.onAmplitudeUpdate = { amp ->
            _uiState.value = _uiState.value.copy(amplitude = amp)
        }

        audioRouter.onRouteChanged = { route ->
            _uiState.value = _uiState.value.copy(currentRoute = route)
        }

        audioRouter.onAvailableRoutesChanged = { routes ->
            _uiState.value = _uiState.value.copy(availableRoutes = routes)
        }

        // Load initial routes
        val routes = audioRouter.getAvailableRoutes()
        val currentRoute = audioRouter.getCurrentRoute()
        _uiState.value = _uiState.value.copy(
            availableRoutes = routes,
            currentRoute = currentRoute
        )

        audioRouter.startListeningForDeviceChanges()

        // Load saved effect preference
        viewModelScope.launch {
            preferencesManager.lastSelectedEffect.collect { index ->
                val effects = VoiceEffect.all()
                if (index in effects.indices) {
                    setEffect(effects[index])
                }
            }
        }
    }

    fun setPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    private var isToggling = false

    fun toggleAudio() {
        // Debounce rapid toggling to prevent crashes
        if (isToggling) return
        isToggling = true

        viewModelScope.launch {
            try {
                val state = _uiState.value
                if (state.isRunning) {
                    audioEngine.stop()
                    _uiState.value = _uiState.value.copy(isRunning = false, amplitude = 0f)
                } else {
                    audioRouter.requestAudioFocus()
                    // Release old resources and re-setup fresh
                    audioEngine.release()
                    if (audioEngine.setup()) {
                        audioEngine.volume = _uiState.value.volume
                        audioEngine.currentEffect = _uiState.value.currentEffect
                        audioEngine.effectProcessor.echoMix = _uiState.value.echoMix
                        audioEngine.start()
                        _uiState.value = _uiState.value.copy(isRunning = true)
                    }
                }
            } finally {
                // Small delay before allowing next toggle to let resources settle
                kotlinx.coroutines.delay(300)
                isToggling = false
            }
        }
    }

    fun setEffect(effect: VoiceEffect) {
        audioEngine.currentEffect = effect
        audioEngine.effectProcessor.reset()
        _uiState.value = _uiState.value.copy(currentEffect = effect)

        // Save preference
        val index = VoiceEffect.all().indexOf(effect)
        viewModelScope.launch {
            preferencesManager.setLastSelectedEffect(index)
        }
    }

    fun setVolume(volume: Float) {
        audioEngine.volume = volume
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun setEchoMix(mix: Float) {
        audioEngine.effectProcessor.echoMix = mix
        _uiState.value = _uiState.value.copy(echoMix = mix)
    }

    fun setAudioRoute(route: AudioOutputRoute) {
        audioRouter.setRoute(route)
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }

    fun cycleRoute() {
        val routes = _uiState.value.availableRoutes
        if (routes.size <= 1) return
        val currentIndex = routes.indexOf(_uiState.value.currentRoute)
        val nextIndex = (currentIndex + 1) % routes.size
        setAudioRoute(routes[nextIndex])
    }

    override fun onCleared() {
        audioEngine.release()
        audioRouter.stopListeningForDeviceChanges()
        super.onCleared()
    }
}
