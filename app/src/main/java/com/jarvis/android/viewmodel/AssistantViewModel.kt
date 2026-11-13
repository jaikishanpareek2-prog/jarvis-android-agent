package com.jarvis.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AssistantViewModel : ViewModel() {
    var prompt by mutableStateOf("")
        private set

    var assistantResponse by mutableStateOf("JARVIS is ready. Type a command or speak to activate.")
        private set

    fun updatePrompt(value: String) {
        prompt = value
    }

    fun submitPrompt() {
        val input = prompt.trim()
        if (input.isEmpty()) return

        assistantResponse = when {
            input.contains("battery", ignoreCase = true) -> "Battery details can be read from Android system APIs once device checks are in place."
            input.contains("wifi", ignoreCase = true) -> "I can open the Wi‑Fi settings screen for you."
            input.contains("bluetooth", ignoreCase = true) -> "I can open the Bluetooth settings screen for you."
            input.contains("flashlight", ignoreCase = true) -> "Flashlight control is gated by device capability checks and user consent."
            else -> "I’m ready to help with commands, settings, and local device assistance."
        }
        prompt = ""
    }
}
