package com.jarvis.android.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AssistantViewModel : ViewModel() {
    var prompt by mutableStateOf("")
        private set

    fun updatePrompt(value: String) {
        prompt = value
    }

    fun submitPrompt() {
        // Hook for agent processing, LLM, or tool execution.
    }
}
