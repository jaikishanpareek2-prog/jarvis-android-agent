package com.jarvis.android.agent

import android.content.Context
import com.jarvis.android.ai.JarvisAiFacade
import com.jarvis.android.device.DeviceActionController
import com.jarvis.android.storage.ConversationStore

data class WorkflowResult(
    val response: String,
    val shouldSpeak: Boolean = true
)

class AssistantWorkflow(
    private val context: Context
) {
    private val history = ConversationStore(context)
    private val ai = JarvisAiFacade(context)
    private val actions = DeviceActionController(context, ai)

    fun run(input: String): WorkflowResult {
        val prompt = input.trim()
        if (prompt.isEmpty()) return WorkflowResult("I’m ready when you are.")

        val memory = history.contextSummary(5)
        val workflowPrompt = if (memory.isBlank()) {
            prompt
        } else {
            "$memory\nUser: $prompt"
        }

        val response = actions.executeCommand(workflowPrompt)
        history.save(prompt, response)
        return WorkflowResult(response = response, shouldSpeak = true)
    }
}
