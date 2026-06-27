package com.bluetooth.bluetoothmictospeaker.audio

sealed class VoiceEffect(val displayName: String, val iconName: String, val isFree: Boolean) {
    data object None : VoiceEffect("None", "mic", true)
    data object Robot : VoiceEffect("Robot", "smart_toy", true)
    data object Chipmunk : VoiceEffect("Chipmunk", "pets", false)
    data object DeepVoice : VoiceEffect("Deep", "record_voice_over", false)
    data object Echo : VoiceEffect("Echo", "surround_sound", false)
    data object Alien : VoiceEffect("Alien", "blur_on", false)
    data object Radio : VoiceEffect("Radio", "radio", false)
    data object Princess : VoiceEffect("Princess", "auto_awesome", true)
    data object Autotune : VoiceEffect("Autotune", "music_note", true)

    companion object {
        fun all(): List<VoiceEffect> = listOf(None, Robot, Chipmunk, DeepVoice, Echo, Alien, Radio, Princess, Autotune)
    }
}
