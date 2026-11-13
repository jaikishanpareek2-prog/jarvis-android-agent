package com.jarvis.android.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.android.R
import com.jarvis.android.voice.WakeWordListener

class VoiceAssistantService : Service() {
    private var wakeWordListener: WakeWordListener? = null
    private val wakeIntentAction = "com.jarvis.android.WAKE_ACTIVATED"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jarvis_service_channel",
                "Jarvis Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A microphone foreground service crashes the app if started without the permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification: Notification = NotificationCompat.Builder(this, "jarvis_service_channel")
            .setContentTitle("JARVIS is listening")
            .setContentText("Your assistant is ready for voice activation.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        wakeWordListener = WakeWordListener(
            context = this,
            onWake = {
                sendBroadcast(Intent(wakeIntentAction).apply {
                    setPackage(packageName)
                    putExtra("wake_activated", true)
                })
            },
            onError = { /* no-op for now; keep service safe */ }
        )
        wakeWordListener?.stop()
        wakeWordListener?.start()

        // Not sticky: Android 12+ blocks restarting a microphone foreground service from the background.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wakeWordListener?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
