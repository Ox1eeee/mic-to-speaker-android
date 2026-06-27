package com.bluetooth.bluetoothmictospeaker.audio

sealed class VoiceEffect(val displayName: String, val iconName: String, val isFree: Boolean) {
    data object None : VoiceEffect("None", "mic", true)
    data object Robot : VoiceEffect("Robot", "smart_toy", true)
    data object Chipmunk : VoiceEffect("Chipmunk", "pets", false)
    data object DeepVoice : VoiceEffect("Deep", "record_voice_over", false)
    data object Echo : VoiceEffect("Echo", "surround_sound", false)
    data object Alien : VoiceEffect("Alien", "blur_on", false)
    data object Radio : VoiceEffect("Radio", "radio", false)
    data object Princess : VoiceEffect("Princess", "auto_awesome", false)
    data object Helium : VoiceEffect("Helium", "bubble_chart", false)
    data object Monster : VoiceEffect("Monster", "whatshot", false)
    data object Telephone : VoiceEffect("Telephone", "phone_in_talk", false)
    data object Underwater : VoiceEffect("Underwater", "water_drop", false)
    data object Megaphone : VoiceEffect("Megaphone", "campaign", false)
    data object Baby : VoiceEffect("Baby", "child_care", false)
    data object WalkieTalkie : VoiceEffect("Walkie", "settings_input_antenna", false)
    data object Drunk : VoiceEffect("Drunk", "local_bar", false)
    data object Zombie : VoiceEffect("Zombie", "skull", false)
    data object Giant : VoiceEffect("Giant", "landscape", false)
    data object Cave : VoiceEffect("Cave", "temple_buddhist", false)
    data object Ghost : VoiceEffect("Ghost", "nightlight", false)
    data object DarthVader : VoiceEffect("Vader", "shield", false)
    data object OldMan : VoiceEffect("Old Man", "elderly", false)
    data object Astronaut : VoiceEffect("Space", "rocket_launch", false)
    data object EightBit : VoiceEffect("8-Bit", "videogame_asset", false)
    data object Stadium : VoiceEffect("Stadium", "stadium", false)
    data object GenderSwap : VoiceEffect("Gender", "swap_horiz", false)
    data object Vocoder : VoiceEffect("Vocoder", "graphic_eq", false)
    data object Choir : VoiceEffect("Choir", "groups", false)

    companion object {
        fun all(): List<VoiceEffect> = listOf(
            None, Robot, Chipmunk, DeepVoice, Echo, Alien, Radio, Princess,
            Helium, Monster, Telephone, Underwater, Megaphone, Baby, WalkieTalkie, Drunk, Zombie, Giant,
            Cave, Ghost, DarthVader, OldMan, Astronaut, EightBit, Stadium,
            GenderSwap, Vocoder, Choir
        )

        fun nativeId(effect: VoiceEffect): Int = all().indexOf(effect)
    }
}
