package com.jarvis.android.ai

import android.content.Context
import org.json.JSONObject

class JarvisAiFacade(
    private val context: Context
) {
    fun generateResponse(prompt: String): String {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return "I’m ready when you are."

        val normalized = cleanPrompt.lowercase()
        val memoryContext = if (normalized.contains("user:") && normalized.contains("assistant:")) {
            "I can see the recent conversation context, and I’m keeping the response aligned with it."
        } else {
            "" 
        }

        val message = when {
            normalized.contains("battery", ignoreCase = true) -> "Your phone battery level is best checked through Android system APIs, and I can wire that in once device-control permissions are confirmed."
            normalized.contains("flashlight", ignoreCase = true) -> "I can trigger the flashlight through Android APIs after permission and device checks are complete."
            normalized.contains("weather", ignoreCase = true) -> "Weather data requires a configured external provider with user-approved network access."
            normalized.contains("remind", ignoreCase = true) || normalized.contains("timer", ignoreCase = true) -> "This requires a reminder or alarm integration layer and user confirmation for scheduling."
            normalized.contains("jarvis", ignoreCase = true) -> "JARVIS is a local Android assistant shell. It can process prompts, device settings, and recent memory while the AI provider layer is being connected."
            else -> "I’m ready to help with commands, device actions, and context-aware assistance once the AI provider and tool layer are configured."
        }

        return if (memoryContext.isBlank()) message else "$memoryContext $message"
    }

    fun buildToolPayload(prompt: String): JSONObject = JSONObject().apply {
        put("prompt", prompt)
        put("context", "android_jarvis_local_stub")
        put("timestamp", System.currentTimeMillis())
    }
}
