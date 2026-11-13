package com.jarvis.android.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.jarvis.android.agent.AssistantCoordinator
import com.jarvis.android.storage.ConversationStore

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val coordinator = AssistantCoordinator(application)
    private val store = ConversationStore(application)

    var prompt by mutableStateOf("")
        private set

    var response by mutableStateOf("Your conversations stay on this device and are not shared by default.")
        private set

    fun updatePrompt(value: String) {
        prompt = value
    }

    fun submitPrompt() {
        val input = prompt.trim()
        if (input.isEmpty()) return
        response = coordinator.process(input)
        prompt = ""
    }

    fun recentMessages() = store.loadRecent(5)
}
