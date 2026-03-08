package com.bluetooth.bluetoothmictospeaker.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bluetooth.bluetoothmictospeaker.MainActivity
import com.bluetooth.bluetoothmictospeaker.R

class AudioForegroundService : Service() {

    private var audioEngine: AudioEngine? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioEngine = AudioEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                audioEngine?.setup()
                audioEngine?.start()
            }
            ACTION_STOP -> {
                audioEngine?.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioEngine?.release()
        audioEngine = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mic to Speaker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when mic-to-speaker audio is active"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mic Speaker Active")
            .setContentText("Voice effect is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "mic_speaker_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.bluetooth.bluetoothmictospeaker.START"
        const val ACTION_STOP = "com.bluetooth.bluetoothmictospeaker.STOP"
    }
}
