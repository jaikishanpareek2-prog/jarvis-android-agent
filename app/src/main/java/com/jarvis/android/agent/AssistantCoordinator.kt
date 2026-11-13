package com.jarvis.android.agent

import android.content.Context

class AssistantCoordinator(
    private val context: Context
) {
    private val workflow = AssistantWorkflow(context)

    fun process(prompt: String): String {
        return workflow.run(prompt).response
    }
}
