#include "effect_processor.h"
#include <cmath>
#include <cstdlib>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

EffectProcessor::EffectProcessor(int sampleRate) : mSampleRate(sampleRate) {
    echoBuffer.resize(sampleRate, 0.f);
    chorusDelayBuf.resize(sampleRate, 0.f);
    shimmerBuf.resize(sampleRate, 0.f);
    monsterReverbBuf.resize(sampleRate, 0.f);
    zombieReverbBuf.resize(sampleRate, 0.f);
    ghostReverbBuf.resize(sampleRate * 2, 0.f);
    vaderReverbBuf.resize(sampleRate, 0.f);
    astronautFlangerBuf.resize(sampleRate, 0.f);
    astronautReverbBuf.resize(sampleRate * 2, 0.f);
    stadiumLateBuf.resize(sampleRate * 3, 0.f);
    choirReverbBuf.resize(sampleRate, 0.f);

    for (int i = 0; i < COMB_COUNT; i++)
        combBuffers[i].resize(combDelays[i] + 1, 0.f);
    for (int i = 0; i < ALLPASS_COUNT; i++)
        allpassBuffers[i].resize(allpassDelays[i] + 1, 0.f);
    for (int i = 0; i < STADIUM_EARLY_COUNT; i++)
        stadiumEarlyBufs[i].resize(stadiumEarlyDelays[i] + 1, 0.f);

    tempBuf.resize(8192, 0);
    tempBuf2.resize(8192, 0);
}

EffectProcessor::~EffectProcessor() = default;

void EffectProcessor::reset() {
    std::fill(echoBuffer.begin(), echoBuffer.end(), 0.f);
    echoWriteIndex = 0;
    mainPitchAccum = alienPitchAccum = princessPitchAccum = 0.0;
    ghostPitchAccum = vaderPitchAccum = oldManPitchAccum = 0.0;
    giantPitchAccum = drunkPitchAccum = genderPitchAccum = 0.0;
    choirAccum3rd = choirAccum5th = 0.0;
    robotPhase = 0.0;
    radioHpPrev = radioHpOut = radioLpOut = 0.f;
    std::fill(chorusDelayBuf.begin(), chorusDelayBuf.end(), 0.f);
    chorusWriteIdx = 0; chorusPhase = 0.0;
    std::fill(shimmerBuf.begin(), shimmerBuf.end(), 0.f);
    shimmerWriteIdx = 0;
    underwaterPhase = 0.0;
    drunkPhase = 0.0;
    std::fill(monsterReverbBuf.begin(), monsterReverbBuf.end(), 0.f);
    monsterReverbIdx = 0;
    std::fill(zombieReverbBuf.begin(), zombieReverbBuf.end(), 0.f);
    zombieReverbIdx = 0;
    for (int i = 0; i < COMB_COUNT; i++) { std::fill(combBuffers[i].begin(), combBuffers[i].end(), 0.f); combIndices[i] = 0; }
    for (int i = 0; i < ALLPASS_COUNT; i++) { std::fill(allpassBuffers[i].begin(), allpassBuffers[i].end(), 0.f); allpassIndices[i] = 0; }
    std::fill(ghostReverbBuf.begin(), ghostReverbBuf.end(), 0.f);
    ghostReverbIdx = 0;
    vaderRingPhase = 0.0;
    std::fill(vaderReverbBuf.begin(), vaderReverbBuf.end(), 0.f);
    vaderReverbIdx = 0;
    oldManTremoloPhase = 0.0;
    std::fill(astronautFlangerBuf.begin(), astronautFlangerBuf.end(), 0.f);
    astronautFlangerIdx = 0; astronautFlangerPhase = 0.0;
    std::fill(astronautReverbBuf.begin(), astronautReverbBuf.end(), 0.f);
    astronautReverbIdx = 0;
    eightBitHoldSample = 0; eightBitHoldCounter = 0;
    for (int i = 0; i < STADIUM_EARLY_COUNT; i++) { std::fill(stadiumEarlyBufs[i].begin(), stadiumEarlyBufs[i].end(), 0.f); stadiumEarlyIndices[i] = 0; }
    std::fill(stadiumLateBuf.begin(), stadiumLateBuf.end(), 0.f);
    stadiumLateIdx = 0;
    memset(vocoderEnvelopes, 0, sizeof(vocoderEnvelopes));
    vocoderCarrierPhase = 0.0;
    std::fill(choirReverbBuf.begin(), choirReverbBuf.end(), 0.f);
    choirReverbIdx = 0;
}

void EffectProcessor::process(int16_t* buffer, int32_t size, int effectId) {
    if (size > (int32_t)tempBuf.size()) {
        tempBuf.resize(size, 0);
        tempBuf2.resize(size, 0);
    }

    switch (effectId) {
        case EFFECT_NONE: break;
        case EFFECT_ROBOT: applyRobot(buffer, size); break;
        case EFFECT_CHIPMUNK: { memcpy(tempBuf.data(), buffer, size * 2); applyPitchShift(tempBuf.data(), buffer, size, 2.0f, mainPitchAccum); break; }
        case EFFECT_DEEP_VOICE: { memcpy(tempBuf.data(), buffer, size * 2); applyPitchShift(tempBuf.data(), buffer, size, 0.6f, mainPitchAccum); break; }
        case EFFECT_ECHO: { memcpy(tempBuf.data(), buffer, size * 2); applyEcho(tempBuf.data(), buffer, size, 0.8f); break; }
        case EFFECT_ALIEN: applyAlien(buffer, size); break;
        case EFFECT_RADIO: applyRadio(buffer, size); break;
        case EFFECT_PRINCESS: applyPrincess(buffer, size); break;
        case EFFECT_HELIUM: { memcpy(tempBuf.data(), buffer, size * 2); applyPitchShift(tempBuf.data(), buffer, size, 1.8f, mainPitchAccum); break; }
        case EFFECT_MONSTER: applyMonster(buffer, size); break;
        case EFFECT_TELEPHONE: applyTelephone(buffer, size); break;
        case EFFECT_UNDERWATER: applyUnderwater(buffer, size); break;
        case EFFECT_MEGAPHONE: applyMegaphone(buffer, size); break;
        case EFFECT_BABY: { memcpy(tempBuf.data(), buffer, size * 2); applyPitchShift(tempBuf.data(), buffer, size, 1.5f, mainPitchAccum); break; }
        case EFFECT_WALKIE_TALKIE: applyWalkieTalkie(buffer, size); break;
        case EFFECT_DRUNK: applyDrunk(buffer, size); break;
        case EFFECT_ZOMBIE: applyZombie(buffer, size); break;
        case EFFECT_GIANT: applyGiant(buffer, size); break;
        case EFFECT_CAVE: applyCave(buffer, size); break;
        case EFFECT_GHOST: applyGhost(buffer, size); break;
        case EFFECT_DARTH_VADER: applyDarthVader(buffer, size); break;
        case EFFECT_OLD_MAN: applyOldMan(buffer, size); break;
        case EFFECT_ASTRONAUT: applyAstronaut(buffer, size); break;
        case EFFECT_EIGHT_BIT: applyEightBit(buffer, size); break;
        case EFFECT_STADIUM: applyStadium(buffer, size); break;
        case EFFECT_GENDER_SWAP: applyGenderSwap(buffer, size); break;
        case EFFECT_VOCODER: applyVocoder(buffer, size); break;
        case EFFECT_CHOIR: applyChoir(buffer, size); break;
        default: break;
    }
}

// ============== Pitch Shift ==============
void EffectProcessor::applyPitchShift(const int16_t* input, int16_t* output, int32_t size,
                                       float factor, double& accumulator) {
    for (int i = 0; i < size; i++) {
        int srcInt = (int)accumulator;
        float frac = (float)(accumulator - srcInt);
        if (srcInt >= 0 && srcInt < size - 1) {
            float s = input[srcInt] * (1.f - frac) + input[srcInt + 1] * frac;
            output[i] = clampShort(s);
        } else if (srcInt >= 0 && srcInt < size) {
            output[i] = input[srcInt];
        } else {
            output[i] = 0;
        }
        accumulator += factor;
        if (accumulator >= size) accumulator -= size;
    }
}

// ============== Robot ==============
void EffectProcessor::applyRobot(int16_t* buffer, int32_t size) {
    double phaseInc = 2.0 * M_PI * 100.0 / mSampleRate;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        float mod = (float)sin(robotPhase);
        float processed = s * mod * 1.5f;
        processed = std::clamp(processed, -24000.f, 24000.f);
        robotPhase += phaseInc;
        if (robotPhase > 2.0 * M_PI) robotPhase -= 2.0 * M_PI;
        buffer[i] = clampShort(processed);
    }
}

// ============== Echo ==============
void EffectProcessor::applyEcho(const int16_t* input, int16_t* output, int32_t size, float wetMix) {
    int delaySamples = mSampleRate * 250 / 1000;
    float feedback = 0.4f;
    float effectiveWet = wetMix * echoMix / 0.3f;
    effectiveWet = std::clamp(effectiveWet, 0.f, 0.95f);

    for (int i = 0; i < size; i++) {
        float dry = input[i];
        int readIdx = (echoWriteIndex - delaySamples + (int)echoBuffer.size()) % (int)echoBuffer.size();
        float delayed = echoBuffer[readIdx];
        float mixed = dry + delayed * effectiveWet;
        echoBuffer[echoWriteIndex] = dry + delayed * feedback;
        echoWriteIndex = (echoWriteIndex + 1) % (int)echoBuffer.size();
        output[i] = clampShort(mixed);
    }
}

// ============== Radio ==============
void EffectProcessor::applyRadio(int16_t* buffer, int32_t size) {
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 300.f);
    float alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 3400.f);
    float alphaLp = dt / (rcLp + dt);

    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        radioHpOut = alphaHp * (radioHpOut + s - radioHpPrev);
        radioHpPrev = s;
        radioLpOut += alphaLp * (radioHpOut - radioLpOut);
        float gained = radioLpOut * 2.5f;
        float processed = gained / (1.f + fabsf(gained / 20000.f));
        processed = std::clamp(processed, -30000.f, 30000.f);
        buffer[i] = clampShort(processed);
    }
}

// ============== Alien ==============
void EffectProcessor::applyAlien(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 1.3f, alienPitchAccum);

    double alienFreq = 150.0;
    double phaseInc = 2.0 * M_PI * alienFreq / mSampleRate;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        float mod = (float)sin(robotPhase);
        buffer[i] = clampShort(s * (0.6f + 0.4f * mod));
        robotPhase += phaseInc;
        if (robotPhase > 2.0 * M_PI) robotPhase -= 2.0 * M_PI;
    }
    memcpy(tempBuf.data(), buffer, size * 2);
    applyEcho(tempBuf.data(), buffer, size, 0.3f);
}

// ============== Princess ==============
void EffectProcessor::applyPrincess(int16_t* buffer, int32_t size) {
    float pitchFactor = powf(2.f, 6.f / 12.f);
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), tempBuf2.data(), size, pitchFactor, princessPitchAccum);

    // Blend 80% pitched + 20% dry
    for (int i = 0; i < size; i++)
        buffer[i] = clampShort(tempBuf2[i] * 0.8f + tempBuf[i] * 0.2f);

    // Chorus
    float chorusRate = 1.5f, wetM = 0.3f, dryM = 0.8f;
    int baseDelay = 40, depth = 25;
    for (int i = 0; i < size; i++) {
        chorusDelayBuf[chorusWriteIdx] = buffer[i];
        int mod = (int)(sin(chorusPhase) * depth);
        int rIdx = (chorusWriteIdx - baseDelay - mod + (int)chorusDelayBuf.size()) % (int)chorusDelayBuf.size();
        float delayed = chorusDelayBuf[rIdx];
        buffer[i] = clampShort(buffer[i] * dryM + delayed * wetM);
        chorusWriteIdx = (chorusWriteIdx + 1) % (int)chorusDelayBuf.size();
        chorusPhase += 2.0 * M_PI * chorusRate / mSampleRate;
        if (chorusPhase > 2.0 * M_PI) chorusPhase -= 2.0 * M_PI;
    }

    // Shimmer reverb
    int delaySmp = mSampleRate * 80 / 1000;
    for (int i = 0; i < size; i++) {
        float dry = buffer[i];
        int rIdx = (shimmerWriteIdx - delaySmp + (int)shimmerBuf.size()) % (int)shimmerBuf.size();
        float delayed = shimmerBuf[rIdx];
        float mixed = dry + delayed * 0.25f;
        shimmerBuf[shimmerWriteIdx] = mixed;
        shimmerWriteIdx = (shimmerWriteIdx + 1) % (int)shimmerBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== Monster ==============
void EffectProcessor::applyMonster(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 0.5f, mainPitchAccum);

    int delaySmp = (int)(mSampleRate * 0.08f);
    for (int i = 0; i < size; i++) {
        float s = buffer[i] * 2.f / 32768.f;
        float distorted = (s / (1.f + fabsf(s))) * 32768.f;
        int rIdx = (monsterReverbIdx - delaySmp + (int)monsterReverbBuf.size()) % (int)monsterReverbBuf.size();
        float delayed = monsterReverbBuf[rIdx];
        float mixed = distorted + delayed * 0.3f;
        monsterReverbBuf[monsterReverbIdx] = mixed;
        monsterReverbIdx = (monsterReverbIdx + 1) % (int)monsterReverbBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== Telephone ==============
void EffectProcessor::applyTelephone(int16_t* buffer, int32_t size) {
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 300.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 3400.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        lpO += alphaLp * (hpO - lpO);
        float gained = lpO * 2.f;
        float sat = gained / (1.f + fabsf(gained / 25000.f));
        float crushed = (float)((int)(sat) / 64 * 64);
        buffer[i] = clampShort(crushed);
    }
}

// ============== Underwater ==============
void EffectProcessor::applyUnderwater(int16_t* buffer, int32_t size) {
    double lfoPhaseInc = 2.0 * M_PI * 0.5 / mSampleRate;
    float lpO = 0;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        float lfo = (float)sin(underwaterPhase);
        float cutoff = 500.f + lfo * 200.f;
        float rc = 1.f / (2.f * (float)M_PI * cutoff);
        float dt = 1.f / mSampleRate;
        float alpha = dt / (rc + dt);
        lpO += alpha * (s - lpO);
        underwaterPhase += lfoPhaseInc;
        if (underwaterPhase > 2.0 * M_PI) underwaterPhase -= 2.0 * M_PI;
        buffer[i] = clampShort(lpO * 0.7f);
    }
}

// ============== Megaphone ==============
void EffectProcessor::applyMegaphone(int16_t* buffer, int32_t size) {
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 500.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 4000.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        lpO += alphaLp * (hpO - lpO);
        float clipped = std::clamp(lpO * 3.f, -22000.f, 22000.f);
        buffer[i] = clampShort(clipped);
    }
}

// ============== Walkie-Talkie ==============
void EffectProcessor::applyWalkieTalkie(int16_t* buffer, int32_t size) {
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 1000.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 3000.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        lpO += alphaLp * (hpO - lpO);
        float noise = ((float)rand() / RAND_MAX - 0.5f) * 800.f;
        float gained = (lpO + noise) * 2.f;
        float dist = gained / (1.f + fabsf(gained / 20000.f));
        if (i % 2 != 0) dist = buffer[i - 1]; // hold
        buffer[i] = clampShort(dist);
    }
}

// ============== Drunk ==============
void EffectProcessor::applyDrunk(int16_t* buffer, int32_t size) {
    double lfoPhaseInc = 2.0 * M_PI * 0.3 / mSampleRate;
    for (int i = 0; i < size; i++) {
        float lfo = (float)sin(drunkPhase);
        float factor = 1.f + lfo * 0.05f;
        int srcInt = (int)drunkPitchAccum;
        float frac = (float)(drunkPitchAccum - srcInt);
        if (srcInt >= 0 && srcInt < size - 1) {
            float s = buffer[srcInt] * (1.f - frac) + buffer[srcInt + 1] * frac;
            tempBuf[i] = clampShort(s);
        } else if (srcInt >= 0 && srcInt < size) {
            tempBuf[i] = buffer[srcInt];
        } else {
            tempBuf[i] = 0;
        }
        drunkPitchAccum += factor;
        if (drunkPitchAccum >= size) drunkPitchAccum -= size;
        drunkPhase += lfoPhaseInc;
        if (drunkPhase > 2.0 * M_PI) drunkPhase -= 2.0 * M_PI;
    }
    memcpy(buffer, tempBuf.data(), size * 2);
}

// ============== Zombie ==============
void EffectProcessor::applyZombie(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 0.7f, mainPitchAccum);

    int delaySmp = (int)(mSampleRate * 0.15f);
    for (int i = 0; i < size; i++) {
        float s = buffer[i] * 1.8f;
        float dist = (s > 0) ? std::min(s, 18000.f) : std::max(s, -25000.f);
        int rIdx = (zombieReverbIdx - delaySmp + (int)zombieReverbBuf.size()) % (int)zombieReverbBuf.size();
        float delayed = zombieReverbBuf[rIdx];
        float mixed = dist + delayed * 0.4f;
        zombieReverbBuf[zombieReverbIdx] = mixed;
        zombieReverbIdx = (zombieReverbIdx + 1) % (int)zombieReverbBuf.size();
        buffer[i] = clampShort(mixed * 0.85f);
    }
}

// ============== Giant ==============
void EffectProcessor::applyGiant(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 0.4f, giantPitchAccum);

    int delaySmp = (int)(mSampleRate * 0.25f);
    float lpO = 0;
    float dt = 1.f / mSampleRate;
    float rcLp = 1.f / (2.f * (float)M_PI * 300.f);
    float alphaLp = dt / (rcLp + dt);
    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        lpO += alphaLp * (s - lpO);
        float boosted = s + lpO * 0.5f;
        int rIdx = (monsterReverbIdx - delaySmp + (int)monsterReverbBuf.size()) % (int)monsterReverbBuf.size();
        float delayed = monsterReverbBuf[rIdx];
        float mixed = boosted + delayed * 0.35f;
        monsterReverbBuf[monsterReverbIdx] = mixed;
        monsterReverbIdx = (monsterReverbIdx + 1) % (int)monsterReverbBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== Cave ==============
void EffectProcessor::applyCave(int16_t* buffer, int32_t size) {
    float feedback = 0.84f, wetMix = 0.6f, dryMix = 0.4f;
    for (int i = 0; i < size; i++) {
        float input = buffer[i] / 32768.f;
        float combSum = 0;
        for (int c = 0; c < COMB_COUNT; c++) {
            auto& buf = combBuffers[c];
            int rIdx = (combIndices[c] - combDelays[c] + (int)buf.size()) % (int)buf.size();
            float delayed = buf[rIdx];
            buf[combIndices[c]] = input + feedback * delayed;
            combIndices[c] = (combIndices[c] + 1) % (int)buf.size();
            combSum += delayed;
        }
        combSum /= 4.f;
        float apOut = combSum;
        for (int a = 0; a < ALLPASS_COUNT; a++) {
            auto& buf = allpassBuffers[a];
            int rIdx = (allpassIndices[a] - allpassDelays[a] + (int)buf.size()) % (int)buf.size();
            float delayed = buf[rIdx];
            float apIn = apOut;
            apOut = -0.5f * apIn + delayed;
            buf[allpassIndices[a]] = apIn + 0.5f * delayed;
            allpassIndices[a] = (allpassIndices[a] + 1) % (int)buf.size();
        }
        buffer[i] = clampShort((input * dryMix + apOut * wetMix) * 32768.f);
    }
}

// ============== Ghost ==============
void EffectProcessor::applyGhost(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), tempBuf2.data(), size, 2.0f, ghostPitchAccum);

    int delaySmp = (int)(mSampleRate * 0.8f);
    for (int i = 0; i < size; i++) {
        float dry = buffer[i] * 0.3f;
        float pitched = tempBuf2[i] * 0.2f;
        float envelope = fabsf(buffer[i]) / 32768.f;
        float noise = ((float)rand() / RAND_MAX - 0.5f) * 32768.f * 0.15f * envelope;
        float combined = dry + pitched + noise;
        int rIdx = (ghostReverbIdx - delaySmp + (int)ghostReverbBuf.size()) % (int)ghostReverbBuf.size();
        float delayed = ghostReverbBuf[rIdx];
        float mixed = combined + delayed * 0.5f;
        ghostReverbBuf[ghostReverbIdx] = mixed;
        ghostReverbIdx = (ghostReverbIdx + 1) % (int)ghostReverbBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== Darth Vader ==============
void EffectProcessor::applyDarthVader(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 0.75f, vaderPitchAccum);

    double ringPhaseInc = 2.0 * M_PI * 30.0 / mSampleRate;
    int helmDelay = (int)(mSampleRate * 0.05f);
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 100.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 200.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;

    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        float ringMod = (float)sin(vaderRingPhase);
        float modulated = s * (0.8f + 0.2f * ringMod);
        vaderRingPhase += ringPhaseInc;
        if (vaderRingPhase > 2.0 * M_PI) vaderRingPhase -= 2.0 * M_PI;
        hpO = alphaHp * (hpO + modulated - hpP); hpP = modulated;
        lpO += alphaLp * (hpO - lpO);
        float resonance = modulated + lpO * 0.4f;
        int rIdx = (vaderReverbIdx - helmDelay + (int)vaderReverbBuf.size()) % (int)vaderReverbBuf.size();
        float delayed = vaderReverbBuf[rIdx];
        float mixed = resonance + delayed * 0.2f;
        vaderReverbBuf[vaderReverbIdx] = mixed;
        vaderReverbIdx = (vaderReverbIdx + 1) % (int)vaderReverbBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== Old Man ==============
void EffectProcessor::applyOldMan(int16_t* buffer, int32_t size) {
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, 0.85f, oldManPitchAccum);

    double tremPhaseInc = 2.0 * M_PI * 5.5 / mSampleRate;
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 150.f), alphaHp = rcHp / (rcHp + dt);
    float hpP = 0, hpO = 0;

    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        float tremMod = 1.f - 0.15f + 0.15f * (float)sin(oldManTremoloPhase);
        oldManTremoloPhase += tremPhaseInc;
        if (oldManTremoloPhase > 2.0 * M_PI) oldManTremoloPhase -= 2.0 * M_PI;
        float tremoloed = hpO * tremMod;
        float crackle = ((float)rand() / RAND_MAX < 0.002f) ? ((float)rand() / RAND_MAX - 0.5f) * 2000.f : 0.f;
        buffer[i] = clampShort(tremoloed + crackle);
    }
}

// ============== Astronaut ==============
void EffectProcessor::applyAstronaut(int16_t* buffer, int32_t size) {
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 500.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 5000.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;
    double flangerPhaseInc = 2.0 * M_PI * 0.1 / mSampleRate;
    int flangerBase = 88, flangerDepth = 44;
    int reverbDelay = (int)(mSampleRate * 0.6f);

    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        lpO += alphaLp * (hpO - lpO);
        astronautFlangerBuf[astronautFlangerIdx] = lpO;
        int mod = (int)(sin(astronautFlangerPhase) * flangerDepth);
        int fRIdx = (astronautFlangerIdx - flangerBase - mod + (int)astronautFlangerBuf.size()) % (int)astronautFlangerBuf.size();
        float flanged = (lpO + astronautFlangerBuf[fRIdx]) * 0.5f;
        astronautFlangerIdx = (astronautFlangerIdx + 1) % (int)astronautFlangerBuf.size();
        astronautFlangerPhase += flangerPhaseInc;
        if (astronautFlangerPhase > 2.0 * M_PI) astronautFlangerPhase -= 2.0 * M_PI;
        float staticNoise = ((float)rand() / RAND_MAX < 0.005f) ? ((float)rand() / RAND_MAX - 0.5f) * 3000.f : 0.f;
        float withStatic = flanged + staticNoise;
        int rIdx = (astronautReverbIdx - reverbDelay + (int)astronautReverbBuf.size()) % (int)astronautReverbBuf.size();
        float delayed = astronautReverbBuf[rIdx];
        float mixed = withStatic + delayed * 0.45f;
        astronautReverbBuf[astronautReverbIdx] = mixed;
        astronautReverbIdx = (astronautReverbIdx + 1) % (int)astronautReverbBuf.size();
        buffer[i] = clampShort(mixed);
    }
}

// ============== 8-Bit ==============
void EffectProcessor::applyEightBit(int16_t* buffer, int32_t size) {
    int holdLen = 6, step = 65536 / 8;
    for (int i = 0; i < size; i++) {
        eightBitHoldCounter++;
        if (eightBitHoldCounter >= holdLen) {
            eightBitHoldCounter = 0;
            int s = buffer[i] + 32768;
            int q = (s / step) * step - 32768;
            eightBitHoldSample = clampShort((float)q);
        }
        buffer[i] = eightBitHoldSample;
    }
}

// ============== Stadium ==============
void EffectProcessor::applyStadium(int16_t* buffer, int32_t size) {
    int preDelay = (int)(mSampleRate * 0.08f);
    float lateFb = 0.92f, wetMix = 0.5f, dryMix = 0.5f;
    for (int i = 0; i < size; i++) {
        float input = buffer[i];
        float earlySum = 0;
        for (int e = 0; e < STADIUM_EARLY_COUNT; e++) {
            auto& buf = stadiumEarlyBufs[e];
            int rIdx = (stadiumEarlyIndices[e] - stadiumEarlyDelays[e] + (int)buf.size()) % (int)buf.size();
            earlySum += buf[rIdx] * 0.3f;
            buf[stadiumEarlyIndices[e]] = input;
            stadiumEarlyIndices[e] = (stadiumEarlyIndices[e] + 1) % (int)buf.size();
        }
        int lateRIdx = (stadiumLateIdx - preDelay - (int)(mSampleRate * 0.5f) + (int)stadiumLateBuf.size()) % (int)stadiumLateBuf.size();
        float lateDelayed = stadiumLateBuf[lateRIdx];
        float lateMixed = input + earlySum * 0.5f + lateDelayed * lateFb;
        stadiumLateBuf[stadiumLateIdx] = lateMixed * 0.97f;
        stadiumLateIdx = (stadiumLateIdx + 1) % (int)stadiumLateBuf.size();
        float mixed = input * dryMix + (earlySum + lateDelayed * 0.3f) * wetMix;
        buffer[i] = clampShort(mixed);
    }
}

// ============== Gender Swap (Male→Female: +5 semitones) ==============
void EffectProcessor::applyGenderSwap(int16_t* buffer, int32_t size) {
    float factor = powf(2.f, 5.f / 12.f);  // +5 semitones
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), buffer, size, factor, genderPitchAccum);

    // Slight formant compensation: boost 1-3kHz range
    float dt = 1.f / mSampleRate;
    float rcHp = 1.f / (2.f * (float)M_PI * 1000.f), alphaHp = rcHp / (rcHp + dt);
    float rcLp = 1.f / (2.f * (float)M_PI * 3000.f), alphaLp = dt / (rcLp + dt);
    float hpP = 0, hpO = 0, lpO = 0;

    for (int i = 0; i < size; i++) {
        float s = buffer[i];
        hpO = alphaHp * (hpO + s - hpP); hpP = s;
        lpO += alphaLp * (hpO - lpO);
        // Add formant region boost back
        buffer[i] = clampShort(s + lpO * 0.3f);
    }
}

// ============== Vocoder (8-band simplified) ==============
void EffectProcessor::applyVocoder(int16_t* buffer, int32_t size) {
    // Band center frequencies (Hz)
    float bandCenters[VOCODER_BANDS] = {200, 400, 800, 1200, 2000, 3200, 5000, 8000};
    float bandWidth = 0.7f;  // Q factor

    float dt = 1.f / mSampleRate;
    float envelopeAlpha = dt / (1.f / (2.f * (float)M_PI * 50.f) + dt);  // 50Hz envelope follower

    for (int i = 0; i < size; i++) {
        float input = buffer[i] / 32768.f;

        // Generate sawtooth carrier at 150Hz (robotic)
        float carrier = (float)(fmod(vocoderCarrierPhase, 1.0) * 2.0 - 1.0);
        vocoderCarrierPhase += 150.0 / mSampleRate;
        if (vocoderCarrierPhase > 1.0) vocoderCarrierPhase -= 1.0;

        float outputSample = 0;
        for (int b = 0; b < VOCODER_BANDS; b++) {
            // Simple band energy estimation
            float rc = 1.f / (2.f * (float)M_PI * bandCenters[b] * bandWidth);
            float alpha = dt / (rc + dt);

            // Extract envelope
            float bandEnergy = fabsf(input);  // Simplified
            vocoderEnvelopes[b] += envelopeAlpha * (bandEnergy - vocoderEnvelopes[b]);

            // Modulate carrier with envelope
            outputSample += carrier * vocoderEnvelopes[b] / VOCODER_BANDS;
        }

        buffer[i] = clampShort(outputSample * 32768.f * 2.f);
    }
}

// ============== Choir/Harmony (+major 3rd, +perfect 5th) ==============
void EffectProcessor::applyChoir(int16_t* buffer, int32_t size) {
    float factor3rd = powf(2.f, 4.f / 12.f);   // +4 semitones (major 3rd)
    float factor5th = powf(2.f, 7.f / 12.f);   // +7 semitones (perfect 5th)

    // Create harmony voices
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), tempBuf2.data(), size, factor3rd, choirAccum3rd);

    // Need another temp for 5th — reuse tempBuf for input
    std::vector<int16_t> voice5th(size);
    memcpy(tempBuf.data(), buffer, size * 2);
    applyPitchShift(tempBuf.data(), voice5th.data(), size, factor5th, choirAccum5th);

    // Mix: original 50% + 3rd 30% + 5th 30%
    int reverbDelay = (int)(mSampleRate * 0.1f);
    float feedback = 0.25f;

    for (int i = 0; i < size; i++) {
        float original = buffer[i] * 0.50f;
        float third = tempBuf2[i] * 0.30f;
        float fifth = voice5th[i] * 0.30f;
        float mixed = original + third + fifth;

        // Slight reverb on the mix
        int rIdx = (choirReverbIdx - reverbDelay + (int)choirReverbBuf.size()) % (int)choirReverbBuf.size();
        float delayed = choirReverbBuf[rIdx];
        float withReverb = mixed + delayed * feedback;
        choirReverbBuf[choirReverbIdx] = withReverb;
        choirReverbIdx = (choirReverbIdx + 1) % (int)choirReverbBuf.size();

        buffer[i] = clampShort(withReverb);
    }
}
