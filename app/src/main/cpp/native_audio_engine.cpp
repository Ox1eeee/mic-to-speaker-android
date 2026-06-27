#include <jni.h>
#include <oboe/Oboe.h>
#include <android/log.h>
#include <atomic>
#include <mutex>
#include <cstring>
#include <cmath>
#include "effect_processor.h"

#define LOG_TAG "NativeAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

class NativeAudioEngine : public oboe::AudioStreamDataCallback,
                          public oboe::AudioStreamErrorCallback {
public:
    NativeAudioEngine() = default;
    ~NativeAudioEngine() { stop(); }

    bool start() {
        std::lock_guard<std::mutex> lock(mLock);
        if (mIsRunning) return true;

        mEffectProcessor.reset();

        // Build input (recording) stream
        oboe::AudioStreamBuilder inputBuilder;
        inputBuilder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(mSampleRate)
            ->setInputPreset(oboe::InputPreset::VoiceCommunication);

        oboe::Result result = inputBuilder.openStream(mInputStream);
        if (result != oboe::Result::OK) {
            LOGE("Failed to open input stream: %s", oboe::convertToText(result));
            return false;
        }

        // Get the actual sample rate and buffer size from the input stream
        mSampleRate = mInputStream->getSampleRate();
        int32_t inputFramesPerBurst = mInputStream->getFramesPerBurst();
        LOGI("Input stream opened: sampleRate=%d, framesPerBurst=%d", mSampleRate, inputFramesPerBurst);

        // Recreate effect processor with actual sample rate
        mEffectProcessor = EffectProcessor(mSampleRate);

        // Build output (playback) stream — callback-driven
        oboe::AudioStreamBuilder outputBuilder;
        outputBuilder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::I16)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(mSampleRate)
            ->setDataCallback(this)
            ->setErrorCallback(this)
            ->setUsage(oboe::Usage::Media)
            ->setContentType(oboe::ContentType::Music);

        result = outputBuilder.openStream(mOutputStream);
        if (result != oboe::Result::OK) {
            LOGE("Failed to open output stream: %s", oboe::convertToText(result));
            mInputStream->close();
            mInputStream.reset();
            return false;
        }

        int32_t outputFramesPerBurst = mOutputStream->getFramesPerBurst();
        LOGI("Output stream opened: sampleRate=%d, framesPerBurst=%d",
             mOutputStream->getSampleRate(), outputFramesPerBurst);

        // Allocate intermediate buffer
        int32_t bufferSize = std::max(inputFramesPerBurst, outputFramesPerBurst) * 4;
        mIntermediateBuffer.resize(bufferSize, 0);

        // Start both streams
        result = mInputStream->requestStart();
        if (result != oboe::Result::OK) {
            LOGE("Failed to start input stream: %s", oboe::convertToText(result));
            cleanup();
            return false;
        }

        result = mOutputStream->requestStart();
        if (result != oboe::Result::OK) {
            LOGE("Failed to start output stream: %s", oboe::convertToText(result));
            cleanup();
            return false;
        }

        mIsRunning = true;
        LOGI("Audio engine started successfully");
        return true;
    }

    void stop() {
        std::lock_guard<std::mutex> lock(mLock);
        if (!mIsRunning) return;
        mIsRunning = false;
        cleanup();
        LOGI("Audio engine stopped");
    }

    void setVolume(float volume) { mVolume.store(volume); }
    void setEffect(int effectId) { mEffectId.store(effectId); }
    void setEchoMix(float mix) { mEchoMix.store(mix); }
    bool isRunning() const { return mIsRunning; }
    float getAmplitude() const { return mAmplitude.load(); }
    int getSampleRate() const { return mSampleRate; }

    void resetEffect() {
        std::lock_guard<std::mutex> lock(mProcessLock);
        mEffectProcessor.reset();
    }

    // Oboe output callback — reads from input stream, processes, writes to output
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* outputStream,
            void* audioData,
            int32_t numFrames) override {

        if (!mIsRunning || !mInputStream) {
            memset(audioData, 0, numFrames * sizeof(int16_t));
            return oboe::DataCallbackResult::Continue;
        }

        auto* outputBuffer = static_cast<int16_t*>(audioData);

        // Read from input stream (non-blocking)
        auto inputResult = mInputStream->read(
            mIntermediateBuffer.data(), numFrames, 0);

        if (inputResult.value() <= 0) {
            memset(outputBuffer, 0, numFrames * sizeof(int16_t));
            return oboe::DataCallbackResult::Continue;
        }

        int32_t framesRead = inputResult.value();

        // Calculate amplitude for visualization
        int maxAmp = 0;
        for (int32_t i = 0; i < framesRead; i++) {
            int a = abs(mIntermediateBuffer[i]);
            if (a > maxAmp) maxAmp = a;
        }
        mAmplitude.store((float)maxAmp / 32768.f);

        // Apply effect
        {
            std::lock_guard<std::mutex> lock(mProcessLock);
            mEffectProcessor.process(mIntermediateBuffer.data(), framesRead, mEffectId.load());
        }

        // Apply volume
        float vol = mVolume.load();
        if (vol != 1.0f) {
            for (int32_t i = 0; i < framesRead; i++) {
                float s = mIntermediateBuffer[i] * vol;
                mIntermediateBuffer[i] = (s > 32767.f) ? 32767 :
                                         (s < -32768.f) ? -32768 :
                                         static_cast<int16_t>(s);
            }
        }

        // Copy to output, zero-pad if needed
        memcpy(outputBuffer, mIntermediateBuffer.data(), framesRead * sizeof(int16_t));
        if (framesRead < numFrames) {
            memset(outputBuffer + framesRead, 0, (numFrames - framesRead) * sizeof(int16_t));
        }

        return oboe::DataCallbackResult::Continue;
    }

    // Error callback — restart on disconnect
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override {
        LOGW("Stream error after close: %s. Restarting...", oboe::convertToText(error));
        if (mIsRunning) {
            mIsRunning = false;
            cleanup();
            start();
        }
    }

private:
    void cleanup() {
        if (mInputStream) {
            mInputStream->requestStop();
            mInputStream->close();
            mInputStream.reset();
        }
        if (mOutputStream) {
            mOutputStream->requestStop();
            mOutputStream->close();
            mOutputStream.reset();
        }
    }

    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::shared_ptr<oboe::AudioStream> mOutputStream;

    std::mutex mLock;
    std::mutex mProcessLock;
    std::atomic<bool> mIsRunning{false};
    std::atomic<float> mVolume{1.0f};
    std::atomic<int> mEffectId{EFFECT_NONE};
    std::atomic<float> mEchoMix{0.3f};
    std::atomic<float> mAmplitude{0.f};

    int mSampleRate = 44100;
    EffectProcessor mEffectProcessor{44100};
    std::vector<int16_t> mIntermediateBuffer;
};

// ============== JNI Bridge ==============

static NativeAudioEngine* gEngine = nullptr;

extern "C" {

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeCreate(
        JNIEnv*, jobject) {
    if (!gEngine) {
        gEngine = new NativeAudioEngine();
        LOGI("Native audio engine created");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeStart(
        JNIEnv*, jobject) {
    if (gEngine) return gEngine->start();
    return false;
}

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeStop(
        JNIEnv*, jobject) {
    if (gEngine) gEngine->stop();
}

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeDestroy(
        JNIEnv*, jobject) {
    if (gEngine) {
        gEngine->stop();
        delete gEngine;
        gEngine = nullptr;
        LOGI("Native audio engine destroyed");
    }
}

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeSetVolume(
        JNIEnv*, jobject, jfloat volume) {
    if (gEngine) gEngine->setVolume(volume);
}

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeSetEffect(
        JNIEnv*, jobject, jint effectId) {
    if (gEngine) {
        gEngine->resetEffect();
        gEngine->setEffect(effectId);
    }
}

JNIEXPORT void JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeSetEchoMix(
        JNIEnv*, jobject, jfloat mix) {
    if (gEngine) gEngine->setEchoMix(mix);
}

JNIEXPORT jboolean JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeIsRunning(
        JNIEnv*, jobject) {
    if (gEngine) return gEngine->isRunning();
    return false;
}

JNIEXPORT jfloat JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeGetAmplitude(
        JNIEnv*, jobject) {
    if (gEngine) return gEngine->getAmplitude();
    return 0.f;
}

JNIEXPORT jint JNICALL
Java_com_bluetooth_bluetoothmictospeaker_audio_NativeAudioBridge_nativeGetSampleRate(
        JNIEnv*, jobject) {
    if (gEngine) return gEngine->getSampleRate();
    return 44100;
}

} // extern "C"
