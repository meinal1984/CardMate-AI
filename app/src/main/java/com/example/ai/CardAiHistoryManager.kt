package com.example.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent AI chat histories per business card.
 * Each contact card preserves its own conversation history with Gemini AI.
 * A general history is also preserved for directory-wide assistant chats.
 */
object CardAiHistoryManager {
    private const val PREFS_NAME = "cardmate_ai_chat_history_store"
    private const val KEY_PREFIX = "chat_history_card_"

    fun saveHistoryForCard(context: Context, cardId: Long, messages: List<ChatMessage>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (cardId > 0L) "$KEY_PREFIX$cardId" else "${KEY_PREFIX}general"

            val jsonArray = JSONArray()
            // Retain up to 40 most recent messages per card to manage storage efficiently
            messages.takeLast(40).forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("role", msg.role.name)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("modelUsed", msg.modelUsed)
                    put("isGroundedWithMaps", msg.isGroundedWithMaps)
                    put("isError", msg.isError)
                    if (msg.suggestedFollowUpDraft != null) {
                        put("suggestedFollowUpDraft", msg.suggestedFollowUpDraft)
                    }
                }
                jsonArray.put(obj)
            }

            prefs.edit().putString(key, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistoryForCard(context: Context, cardId: Long): List<ChatMessage> {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (cardId > 0L) "$KEY_PREFIX$cardId" else "${KEY_PREFIX}general"
            val raw = prefs.getString(key, null) ?: return emptyList()

            val jsonArray = JSONArray(raw)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val roleStr = obj.optString("role", ChatRole.MODEL.name)
                val role = try {
                    ChatRole.valueOf(roleStr)
                } catch (e: Exception) {
                    ChatRole.MODEL
                }

                list.add(
                    ChatMessage(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        role = role,
                        text = obj.optString("text", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        modelUsed = obj.optString("modelUsed", GeminiModels.GEMINI_3_5_FLASH),
                        isGroundedWithMaps = obj.optBoolean("isGroundedWithMaps", false),
                        isError = obj.optBoolean("isError", false),
                        suggestedFollowUpDraft = if (obj.has("suggestedFollowUpDraft") && !obj.isNull("suggestedFollowUpDraft")) {
                            obj.getString("suggestedFollowUpDraft")
                        } else null
                    )
                )
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun clearHistoryForCard(context: Context, cardId: Long) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (cardId > 0L) "$KEY_PREFIX$cardId" else "${KEY_PREFIX}general"
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hasHistoryForCard(context: Context, cardId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (cardId > 0L) "$KEY_PREFIX$cardId" else "${KEY_PREFIX}general"
        val raw = prefs.getString(key, null)
        return !raw.isNullOrBlank() && raw != "[]"
    }
}
