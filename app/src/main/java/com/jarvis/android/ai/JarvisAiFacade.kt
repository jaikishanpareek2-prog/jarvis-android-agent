package com.jarvis.android.ai

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import org.json.JSONObject

interface AiProvider {
    fun generate(prompt: String): String
}

class GeminiAiProvider(
    private val apiKey: String,
    private val model: String = "gemini-2.0-flash"
) : AiProvider {
    override fun generate(prompt: String): String {
        if (apiKey.isBlank()) return "No AI provider key is configured."
        return "Gemini provider is configured and ready for prompt routing."
    }
}

class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : AiProvider {
    override fun generate(prompt: String): String {
        if (apiKey.isBlank()) return "No AI provider key is configured."
        return "OpenAI provider is configured and ready for prompt routing."
    }
}

class JarvisAiFacade(
    private val context: Context,
    private val provider: AiProvider? = null,
    private val fallback: JarvisLocalResponder = JarvisLocalResponder()
) {
    fun generateResponse(prompt: String): String {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return "I’m ready when you are."

        val providerResponse = provider?.generate(cleanPrompt)
        if (!providerResponse.isNullOrBlank()) return providerResponse

        return fallback.generate(cleanPrompt)
    }

    fun buildToolPayload(prompt: String): JSONObject = JSONObject().apply {
        put("prompt", prompt)
        put("context", "android_jarvis_provider_layer")
        put("timestamp", System.currentTimeMillis())
    }
}

class JarvisLocalResponder {
    fun generate(prompt: String): String {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return "I’m ready when you are."

        return when {
            cleanPrompt.contains("battery", ignoreCase = true) -> "Battery details require Android system APIs and should be read with explicit user consent."
            cleanPrompt.contains("flashlight", ignoreCase = true) -> "Torch control requires device capability checks and explicit user permission flow."
            cleanPrompt.contains("weather", ignoreCase = true) -> "Weather requests require network access and an external data provider."
            cleanPrompt.contains("remind", ignoreCase = true) || cleanPrompt.contains("timer", ignoreCase = true) -> "Reminder and timer actions need a scheduling engine and user confirmation."
            cleanPrompt.contains("hello", ignoreCase = true) || cleanPrompt.contains("hi", ignoreCase = true) -> "Hello. I’m ready to help with voice, actions, and device-aware assistance."
            else -> "I can help with voice, Android actions, and contextual commands once the provider and tool layers are configured."
        }
    }
}
