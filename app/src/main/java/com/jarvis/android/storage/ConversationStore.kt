package com.jarvis.android.storage

import android.content.Context
import androidx.core.content.edit

data class ConversationMessage(
    val id: Long = 0L,
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ConversationStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_conversation_store", Context.MODE_PRIVATE)

    fun save(prompt: String, response: String) {
        val nextId = prefs.getLong("next_id", 0L) + 1L
        prefs.edit {
            putLong("next_id", nextId)
            putString("msg_${nextId}_prompt", prompt)
            putString("msg_${nextId}_response", response)
            putLong("msg_${nextId}_ts", System.currentTimeMillis())
        }
    }

    fun loadRecent(limit: Int = 10): List<ConversationMessage> {
        val items = mutableListOf<ConversationMessage>()
        val nextId = prefs.getLong("next_id", 0L)
        for (id in nextId downTo 1L) {
            if (items.size >= limit) break
            val key = "msg_${id}_prompt"
            val prompt = prefs.getString(key, null) ?: continue
            val response = prefs.getString("msg_${id}_response", "") ?: ""
            val timestamp = prefs.getLong("msg_${id}_ts", System.currentTimeMillis())
            items.add(ConversationMessage(id, prompt, response, timestamp))
        }
        return items
    }
}
