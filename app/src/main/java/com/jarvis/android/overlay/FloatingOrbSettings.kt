package com.jarvis.android.overlay

import android.content.Context
import androidx.core.content.edit

data class FloatingOrbSettings(
    val enabled: Boolean = true,
    val sizeDp: Int = 64,
    val opacity: Float = 0.92f,
    val animationIntensity: Float = 1f,
    val backgroundListening: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val voiceResponses: Boolean = true
)

object FloatingOrbSettingsStore {
    private const val PREFS_NAME = "jarvis_floating_orb_settings"
    private const val KEY_ENABLED = "key_enabled"
    private const val KEY_SIZE_DP = "key_size_dp"
    private const val KEY_OPACITY = "key_opacity"
    private const val KEY_INTENSITY = "key_intensity"
    private const val KEY_BACKGROUND_LISTENING = "key_background_listening"
    private const val KEY_WAKE_WORD = "key_wake_word"
    private const val KEY_VOICE_RESPONSES = "key_voice_responses"

    fun load(context: Context): FloatingOrbSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FloatingOrbSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            sizeDp = prefs.getInt(KEY_SIZE_DP, 64),
            opacity = prefs.getFloat(KEY_OPACITY, 0.92f),
            animationIntensity = prefs.getFloat(KEY_INTENSITY, 1f),
            backgroundListening = prefs.getBoolean(KEY_BACKGROUND_LISTENING, false),
            wakeWordEnabled = prefs.getBoolean(KEY_WAKE_WORD, false),
            voiceResponses = prefs.getBoolean(KEY_VOICE_RESPONSES, true)
        )
    }

    fun save(context: Context, settings: FloatingOrbSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_ENABLED, settings.enabled)
            putInt(KEY_SIZE_DP, settings.sizeDp)
            putFloat(KEY_OPACITY, settings.opacity)
            putFloat(KEY_INTENSITY, settings.animationIntensity)
            putBoolean(KEY_BACKGROUND_LISTENING, settings.backgroundListening)
            putBoolean(KEY_WAKE_WORD, settings.wakeWordEnabled)
            putBoolean(KEY_VOICE_RESPONSES, settings.voiceResponses)
        }
    }
}
