package com.jarvis.android.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import com.jarvis.android.ai.JarvisAiFacade

class DeviceActionController(
    private val context: Context,
    private val aiFacade: JarvisAiFacade
) {
    /**
     * [command] is what the user just said or typed and is the only text used to pick a device action.
     * [aiPrompt] may also carry recent conversation history and is used only for the AI fallback,
     * so words from earlier messages never trigger device actions again.
     */
    fun executeCommand(command: String, aiPrompt: String = command): String {
        // Drop hyphens (including the non-breaking one) so "Wi-Fi" and "Wi‑Fi" match "wifi".
        val normalized = command.trim().replace(Regex("[-\\u2010-\\u2015]"), "")
        if (normalized.isEmpty()) return "I’m ready when you are."

        return when {
            normalized.contains("wifi", ignoreCase = true) -> openSettings(
                Settings.ACTION_WIFI_SETTINGS,
                "I opened Wi‑Fi settings. Android requires you to make the change there."
            )
            normalized.contains("bluetooth", ignoreCase = true) -> openSettings(
                Settings.ACTION_BLUETOOTH_SETTINGS,
                "I opened Bluetooth settings. Android requires you to make the change there."
            )
            normalized.contains("volume", ignoreCase = true) -> openSettings(
                Settings.ACTION_SOUND_SETTINGS,
                "I opened sound settings so you can adjust the volume."
            )
            normalized.contains("battery", ignoreCase = true) -> batteryStatus()
            normalized.contains("flashlight", ignoreCase = true) -> flashlightAction()
            normalized.contains("settings", ignoreCase = true) -> openSettings(
                Settings.ACTION_SETTINGS,
                "I opened system settings."
            )
            normalized.contains("open app", ignoreCase = true) || normalized.contains("launch", ignoreCase = true) -> launchAppAction(normalized)
            else -> aiFacade.generateResponse(aiPrompt)
        }
    }

    private fun openSettings(action: String, successMessage: String): String = try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        successMessage
    } catch (_: Exception) {
        "Android could not open that settings screen on this device."
    }

    private fun flashlightAction(): String {
        val hasFlash = context.packageManager.hasSystemFeature("android.hardware.camera.flash")
        if (!hasFlash) return "This device does not report a camera flash."
        return "Direct flashlight control is gated by device capability checks and explicit user permission flow."
    }

    private fun launchAppAction(command: String): String {
        val lower = command.lowercase()
        return when {
            lower.contains("camera") -> tryLaunch("android.media.action.STILL_IMAGE_CAMERA")
            lower.contains("messages") -> tryLaunch("android.intent.action.MAIN", "com.android.mms")
            lower.contains("phone") -> tryLaunch("android.intent.action.DIAL")
            else -> "I can try to launch a system app, but I need the exact app name or intent."
        }
    }

    private fun tryLaunch(action: String, packageName: String? = null): String = try {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }
        context.startActivity(intent)
        "I attempted to launch the requested app or action."
    } catch (_: Exception) {
        "I could not launch that app or action on this device."
    }

    private fun batteryStatus(): String {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
