package com.jarvis.android.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.android.ai.JarvisAiFacade

class DeviceActionController(
    private val context: Context,
    private val aiFacade: JarvisAiFacade
) {
    fun executeCommand(command: String): String {
        val normalized = command.trim()
        if (normalized.isEmpty()) return "I’m ready when you are."

        return when {
            normalized.contains("flashlight", ignoreCase = true) -> {
                val hasCamera = context.packageManager.hasSystemFeature("android.hardware.camera.flash")
                if (!hasCamera) "This device does not expose a flash/torch capability."
                else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    "I’m opening the relevant system settings so the torch can be managed safely and explicitly."
                }
            }
            normalized.contains("wifi", ignoreCase = true) -> {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "I’ve opened the Wi‑Fi settings screen for you."
            }
            normalized.contains("bluetooth", ignoreCase = true) -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "I’ve opened the Bluetooth settings screen for you."
            }
            normalized.contains("battery", ignoreCase = true) -> {
                val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED)
                val battery = context.registerReceiver(null, batteryIntent)
                val level = battery?.getIntExtra("level", -1) ?: -1
                val scale = battery?.getIntExtra("scale", 100) ?: 100
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else null
                val charging = battery?.getIntExtra("status", -1) == 2 || battery?.getIntExtra("status", -1) == 5
                if (percent == null) "Battery information isn’t available on this device right now."
                else "Battery is at ${percent}%${if (charging) " and charging" else ""}."
            }
            normalized.contains("volume", ignoreCase = true) -> {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "I’ve opened the sound settings so you can adjust volume safely."
            }
            normalized.contains("settings", ignoreCase = true) -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "I’ve opened the system settings screen."
            }
            else -> aiFacade.generateResponse(normalized)
        }
    }
}
