package com.bluetooth.bluetoothmictospeaker.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

enum class AudioOutputRoute(val displayName: String) {
    SPEAKER("Speaker"),
    EARPIECE("Earpiece"),
    BLUETOOTH("Bluetooth"),
    WIRED_HEADSET("Wired Headset");
}

class AudioRouter(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var deviceChangeReceiver: BroadcastReceiver? = null

    var onRouteChanged: ((AudioOutputRoute) -> Unit)? = null
    var onAvailableRoutesChanged: ((List<AudioOutputRoute>) -> Unit)? = null

    fun getAvailableRoutes(): List<AudioOutputRoute> {
        val routes = mutableListOf(AudioOutputRoute.SPEAKER)

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    if (!routes.contains(AudioOutputRoute.BLUETOOTH)) {
                        routes.add(AudioOutputRoute.BLUETOOTH)
                    }
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    if (!routes.contains(AudioOutputRoute.WIRED_HEADSET)) {
                        routes.add(AudioOutputRoute.WIRED_HEADSET)
                    }
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    if (!routes.contains(AudioOutputRoute.EARPIECE)) {
                        routes.add(AudioOutputRoute.EARPIECE)
                    }
                }
            }
        }
        return routes
    }

    fun getCurrentRoute(): AudioOutputRoute {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            // Check active output devices
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
                        return AudioOutputRoute.BLUETOOTH
                    }
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    if (audioManager.isWiredHeadsetOn) {
                        return AudioOutputRoute.WIRED_HEADSET
                    }
                }
            }
        }
        if (audioManager.isSpeakerphoneOn) {
            return AudioOutputRoute.SPEAKER
        }
        return AudioOutputRoute.SPEAKER
    }

    @Suppress("DEPRECATION")
    fun setRoute(route: AudioOutputRoute) {
        when (route) {
            AudioOutputRoute.SPEAKER -> {
                audioManager.isSpeakerphoneOn = true
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            AudioOutputRoute.EARPIECE -> {
                audioManager.isSpeakerphoneOn = false
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            AudioOutputRoute.BLUETOOTH -> {
                audioManager.isSpeakerphoneOn = false
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
            AudioOutputRoute.WIRED_HEADSET -> {
                audioManager.isSpeakerphoneOn = false
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                // Wired headset is automatically used when connected
            }
        }
        Log.d(TAG, "Audio route set to: ${route.displayName}")
        onRouteChanged?.invoke(route)
    }

    fun requestAudioFocus(): Boolean {
        val focusRequest = android.media.AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN
        )
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "Audio focus lost")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        Log.d(TAG, "Audio focus lost transiently")
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(TAG, "Audio focus gained")
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun startListeningForDeviceChanges() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }

        deviceChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val routes = getAvailableRoutes()
                onAvailableRoutesChanged?.invoke(routes)
                val current = getCurrentRoute()
                onRouteChanged?.invoke(current)
                Log.d(TAG, "Device change detected. Current route: ${current.displayName}")
            }
        }

        context.registerReceiver(deviceChangeReceiver, filter)
        Log.d(TAG, "Started listening for device changes")
    }

    fun stopListeningForDeviceChanges() {
        deviceChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver already unregistered")
            }
        }
        deviceChangeReceiver = null
    }

    fun isBluetoothAvailable(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    companion object {
        private const val TAG = "AudioRouter"
    }
}
