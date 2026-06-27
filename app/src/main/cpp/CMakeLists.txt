cmake_minimum_required(VERSION 3.22.1)
project("mic_to_speaker_native")

# Fetch Oboe from GitHub
include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG        1.9.0
)
FetchContent_MakeAvailable(oboe)

add_library(mic_to_speaker_native SHARED
    native_audio_engine.cpp
    effect_processor.cpp
)

target_link_libraries(mic_to_speaker_native
    oboe
    android
    log
)

target_compile_options(mic_to_speaker_native PRIVATE
    -Wall
    -O2
    -ffast-math
)

# 16 KB page size support (required by Google Play for targetSdk 35+)
target_link_options(mic_to_speaker_native PRIVATE
    -Wl,-z,max-page-size=16384
)
