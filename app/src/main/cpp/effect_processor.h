#pragma once
#include <cstdint>
#include <cmath>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <algorithm>

// Effect IDs matching VoiceEffect sealed class order
enum EffectId {
    EFFECT_NONE = 0,
    EFFECT_ROBOT,
    EFFECT_CHIPMUNK,
    EFFECT_DEEP_VOICE,
    EFFECT_ECHO,
    EFFECT_ALIEN,
    EFFECT_RADIO,
    EFFECT_PRINCESS,
    EFFECT_HELIUM,
    EFFECT_MONSTER,
    EFFECT_TELEPHONE,
    EFFECT_UNDERWATER,
    EFFECT_MEGAPHONE,
    EFFECT_BABY,
    EFFECT_WALKIE_TALKIE,
    EFFECT_DRUNK,
    EFFECT_ZOMBIE,
    EFFECT_GIANT,
    EFFECT_CAVE,
    EFFECT_GHOST,
    EFFECT_DARTH_VADER,
    EFFECT_OLD_MAN,
    EFFECT_ASTRONAUT,
    EFFECT_EIGHT_BIT,
    EFFECT_STADIUM,
    EFFECT_GENDER_SWAP,
    EFFECT_VOCODER,
    EFFECT_CHOIR,
    EFFECT_COUNT
};

class EffectProcessor {
public:
    explicit EffectProcessor(int sampleRate = 44100);
    ~EffectProcessor();

    void process(int16_t* buffer, int32_t size, int effectId);
    void reset();

private:
    int mSampleRate;

    // Pitch shift with linear interpolation
    void applyPitchShift(const int16_t* input, int16_t* output, int32_t size,
                         float factor, double& accumulator);

    // Effect methods
    void applyRobot(int16_t* buffer, int32_t size);
    void applyEcho(const int16_t* input, int16_t* output, int32_t size, float wetMix);
    void applyRadio(int16_t* buffer, int32_t size);
    void applyAlien(int16_t* buffer, int32_t size);
    void applyPrincess(int16_t* buffer, int32_t size);
    void applyMonster(int16_t* buffer, int32_t size);
    void applyTelephone(int16_t* buffer, int32_t size);
    void applyUnderwater(int16_t* buffer, int32_t size);
    void applyMegaphone(int16_t* buffer, int32_t size);
    void applyWalkieTalkie(int16_t* buffer, int32_t size);
    void applyDrunk(int16_t* buffer, int32_t size);
    void applyZombie(int16_t* buffer, int32_t size);
    void applyGiant(int16_t* buffer, int32_t size);
    void applyCave(int16_t* buffer, int32_t size);
    void applyGhost(int16_t* buffer, int32_t size);
    void applyDarthVader(int16_t* buffer, int32_t size);
    void applyOldMan(int16_t* buffer, int32_t size);
    void applyAstronaut(int16_t* buffer, int32_t size);
    void applyEightBit(int16_t* buffer, int32_t size);
    void applyStadium(int16_t* buffer, int32_t size);
    void applyGenderSwap(int16_t* buffer, int32_t size);
    void applyVocoder(int16_t* buffer, int32_t size);
    void applyChoir(int16_t* buffer, int32_t size);

    // Helper
    static inline int16_t clampShort(float v) {
        if (v > 32767.f) return 32767;
        if (v < -32768.f) return -32768;
        return static_cast<int16_t>(v);
    }

    // State variables
    // Echo
    std::vector<float> echoBuffer;
    int echoWriteIndex = 0;
    float echoMix = 0.3f;

    // Pitch accumulators
    double mainPitchAccum = 0.0;
    double alienPitchAccum = 0.0;
    double princessPitchAccum = 0.0;
    double ghostPitchAccum = 0.0;
    double vaderPitchAccum = 0.0;
    double oldManPitchAccum = 0.0;
    double giantPitchAccum = 0.0;
    double drunkPitchAccum = 0.0;
    double genderPitchAccum = 0.0;
    double choirAccum3rd = 0.0;
    double choirAccum5th = 0.0;

    // Robot
    double robotPhase = 0.0;

    // Radio filter
    float radioHpPrev = 0.f, radioHpOut = 0.f, radioLpOut = 0.f;

    // Princess chorus
    std::vector<float> chorusDelayBuf;
    int chorusWriteIdx = 0;
    double chorusPhase = 0.0;

    // Princess shimmer
    std::vector<float> shimmerBuf;
    int shimmerWriteIdx = 0;

    // Underwater
    double underwaterPhase = 0.0;

    // Drunk
    double drunkPhase = 0.0;

    // Monster reverb
    std::vector<float> monsterReverbBuf;
    int monsterReverbIdx = 0;

    // Zombie reverb
    std::vector<float> zombieReverbBuf;
    int zombieReverbIdx = 0;

    // Cave Schroeder
    static constexpr int COMB_COUNT = 4;
    static constexpr int ALLPASS_COUNT = 2;
    int combDelays[COMB_COUNT] = {1116, 1188, 1277, 1356};
    int allpassDelays[ALLPASS_COUNT] = {225, 556};
    std::vector<float> combBuffers[COMB_COUNT];
    int combIndices[COMB_COUNT] = {};
    std::vector<float> allpassBuffers[ALLPASS_COUNT];
    int allpassIndices[ALLPASS_COUNT] = {};

    // Ghost reverb
    std::vector<float> ghostReverbBuf;
    int ghostReverbIdx = 0;

    // Vader
    double vaderRingPhase = 0.0;
    std::vector<float> vaderReverbBuf;
    int vaderReverbIdx = 0;

    // Old Man
    double oldManTremoloPhase = 0.0;

    // Astronaut
    std::vector<float> astronautFlangerBuf;
    int astronautFlangerIdx = 0;
    double astronautFlangerPhase = 0.0;
    std::vector<float> astronautReverbBuf;
    int astronautReverbIdx = 0;

    // 8-Bit
    int16_t eightBitHoldSample = 0;
    int eightBitHoldCounter = 0;

    // Stadium
    static constexpr int STADIUM_EARLY_COUNT = 3;
    int stadiumEarlyDelays[STADIUM_EARLY_COUNT] = {882, 1984, 2955};
    std::vector<float> stadiumEarlyBufs[STADIUM_EARLY_COUNT];
    int stadiumEarlyIndices[STADIUM_EARLY_COUNT] = {};
    std::vector<float> stadiumLateBuf;
    int stadiumLateIdx = 0;

    // Vocoder
    static constexpr int VOCODER_BANDS = 8;
    float vocoderEnvelopes[VOCODER_BANDS] = {};
    double vocoderCarrierPhase = 0.0;

    // Choir reverb
    std::vector<float> choirReverbBuf;
    int choirReverbIdx = 0;

    // Temp buffer for multi-stage effects
    std::vector<int16_t> tempBuf;
    std::vector<int16_t> tempBuf2;
};
