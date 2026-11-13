package com.jarvis.android.ai

import android.content.Context
import org.json.JSONObject

class JarvisAiFacade(
    private val context: Context
) {
    fun generateResponse(prompt: String): String {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return "I’m ready when you are."

        // This is a safe local placeholder until a real provider is configured.
        // It preserves the app’s architecture and keeps the provider abstraction ready.
        val message = when {
            cleanPrompt.contains("battery", ignoreCase = true) -> "Your phone battery level is best checked through Android system APIs, and I can wire that in once device-control permissions are enabled."
            cleanPrompt.contains("flashlight", ignoreCase = true) -> "I can trigger the flashlight through Android APIs after permission and device checks are complete."
            cleanPrompt.contains("weather", ignoreCase = true) -> "Weather data requires a configured external provider with user-approved network access."
            cleanPrompt.contains("remind", ignoreCase = true) || cleanPrompt.contains("timer", ignoreCase = true) -> "This requires a reminder or alarm integration layer and user confirmation for scheduling."
            else -> "I’m ready to help you with commands, device actions, and context-aware assistance once the AI provider and tool layer are configured."
        }

        return message
    }

    fun buildToolPayload(prompt: String): JSONObject = JSONObject().apply {
        put("prompt", prompt)
        put("context", "android_jarvis_local_stub")
        put("timestamp", System.currentTimeMillis())
    }
}
