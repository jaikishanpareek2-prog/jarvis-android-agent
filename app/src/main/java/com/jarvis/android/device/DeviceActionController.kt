package com.jarvis.android.device

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.jarvis.android.ai.JarvisAiFacade

class DeviceActionController(
    private val context: Context,
    private val aiFacade: JarvisAiFacade
) {
    fun executeCommand(command: String): String {
        val normalized = command.trim()
        if (normalized.isEmpty()) return "I’m ready when you are."

        return when {
            normalized.contains("wifi", ignoreCase = true) -> openSettings(
                Settings.ACTION_WIFI_SETTINGS, "I’ve opened Wi-Fi settings. Android requires you to make the change there."
            )
            normalized.contains("bluetooth", ignoreCase = true) -> openSettings(
                Settings.ACTION_BLUETOOTH_SETTINGS, "I’ve opened Bluetooth settings. Android requires you to make the change there."
            )
            normalized.contains("volume", ignoreCase = true) -> openSettings(
                Settings.ACTION_SOUND_SETTINGS, "I’ve opened sound settings so you can adjust the volume."
            )
            normalized.contains("flashlight", ignoreCase = true) -> {
                if (!context.packageManager.hasSystemFeature("android.hardware.camera.flash")) {
                    "This device does not report a camera flash."
                } else {
                    "Direct torch control is not wired up yet. I have not changed the flashlight."
                }
            }
            normalized.contains("battery", ignoreCase = true) -> batteryStatus()
            normalized.contains("settings", ignoreCase = true) -> openSettings(
                Settings.ACTION_SETTINGS, "I’ve opened system settings."
            )
            else -> aiFacade.generateResponse(normalized)
        }
    }

    private fun openSettings(action: String, successMessage: String): String = try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        successMessage
    } catch (_: Exception) {
        "Android could not open that settings screen on this device."
    }

    private fun batteryStatus(): String {
        val battery = context.registerReceiver(null, Intent(Intent.ACTION_BATTERY_CHANGED))
            ?: return "Battery information is unavailable right now."
        val level = battery.getIntExtra("level", -1)
        val scale = battery.getIntExtra("scale", -1)
        if (level < 0 || scale <= 0) return "Battery information is unavailable right now."
        val status = battery.getIntExtra("status", -1)
        val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
            status == android.os.BatteryManager.BATTERY_STATUS_FULL
        return "Battery is at ${level * 100 / scale}%${if (charging) " and charging" else ""}."
    }
}
