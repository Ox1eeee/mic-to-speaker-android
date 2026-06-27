package com.bluetooth.bluetoothmictospeaker.audio

sealed class VoiceEffect(val displayName: String, val iconName: String, val isFree: Boolean) {
    data object None : VoiceEffect("None", "mic", true)
    data object Robot : VoiceEffect("Robot", "smart_toy", true)
    data object Chipmunk : VoiceEffect("Chipmunk", "pets", true)
    data object DeepVoice : VoiceEffect("Deep", "record_voice_over", true)
    data object Echo : VoiceEffect("Echo", "surround_sound", true)
    data object Alien : VoiceEffect("Alien", "blur_on", true)
    data object Radio : VoiceEffect("Radio", "radio", true)
    data object Princess : VoiceEffect("Princess", "auto_awesome", true)
    data object Autotune : VoiceEffect("Autotune", "music_note", true)
    data object Helium : VoiceEffect("Helium", "bubble_chart", true)
    data object Monster : VoiceEffect("Monster", "whatshot", true)
    data object Telephone : VoiceEffect("Telephone", "phone_in_talk", true)
    data object Underwater : VoiceEffect("Underwater", "water_drop", true)
    data object Megaphone : VoiceEffect("Megaphone", "campaign", true)
    data object Baby : VoiceEffect("Baby", "child_care", true)
    data object WalkieTalkie : VoiceEffect("Walkie", "settings_input_antenna", true)
    data object Drunk : VoiceEffect("Drunk", "local_bar", true)
    data object Zombie : VoiceEffect("Zombie", "skull", true)
    data object Giant : VoiceEffect("Giant", "landscape", true)
    data object Cave : VoiceEffect("Cave", "temple_buddhist", true)
    data object Ghost : VoiceEffect("Ghost", "nightlight", true)
    data object DarthVader : VoiceEffect("Vader", "shield", true)
    data object OldMan : VoiceEffect("Old Man", "elderly", true)
    data object Astronaut : VoiceEffect("Space", "rocket_launch", true)
    data object EightBit : VoiceEffect("8-Bit", "videogame_asset", true)
    data object Stadium : VoiceEffect("Stadium", "stadium", true)

    companion object {
        fun all(): List<VoiceEffect> = listOf(
            None, Robot, Chipmunk, DeepVoice, Echo, Alien, Radio, Princess, Autotune,
            Helium, Monster, Telephone, Underwater, Megaphone, Baby, WalkieTalkie, Drunk, Zombie, Giant,
            Cave, Ghost, DarthVader, OldMan, Astronaut, EightBit, Stadium
        )
    }
}
