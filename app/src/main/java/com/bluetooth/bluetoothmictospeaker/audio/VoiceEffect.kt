package com.bluetooth.bluetoothmictospeaker.audio

sealed class VoiceEffect(val displayName: String, val iconName: String) {
    data object None : VoiceEffect("None", "mic")
    data object Robot : VoiceEffect("Robot", "smart_toy")
    data object Chipmunk : VoiceEffect("Chipmunk", "pets")
    data object DeepVoice : VoiceEffect("Deep", "record_voice_over")
    data object Echo : VoiceEffect("Echo", "surround_sound")
    data object Alien : VoiceEffect("Alien", "blur_on")
    data object Radio : VoiceEffect("Radio", "radio")

    companion object {
        fun all(): List<VoiceEffect> = listOf(None, Robot, Chipmunk, DeepVoice, Echo, Alien, Radio)
    }
}
