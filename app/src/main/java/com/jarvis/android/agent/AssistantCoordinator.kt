package com.jarvis.android.agent

import android.content.Context
import com.jarvis.android.ai.JarvisAiFacade
import com.jarvis.android.device.DeviceActionController
import com.jarvis.android.storage.ConversationStore

class AssistantCoordinator(
    private val context: Context
) {
    private val ai = JarvisAiFacade(context)
    private val actions = DeviceActionController(context, ai)
    private val history = ConversationStore(context)

    fun process(prompt: String): String {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return "I’m ready when you are."

        val memoryContext = history.contextSummary(5)
        val contextualPrompt = if (memoryContext.isBlank()) {
            cleanPrompt
        } else {
            "$memoryContext\nUser: $cleanPrompt"
        }

        val response = actions.executeCommand(contextualPrompt)
        history.save(cleanPrompt, response)
        return response
    }
}
