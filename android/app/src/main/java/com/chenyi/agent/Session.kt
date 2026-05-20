package com.chenyi.agent

import org.json.JSONObject
import java.util.UUID

/**
 * 会话数据类
 */
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新会话",
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 转换为 JSON
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("messages", messages.map { it.toJson() }.toString())
            put("created_at", createdAt)
            put("updated_at", updatedAt)
        }
    }
    
    companion object {
        /**
         * 从 JSON 解析
         */
        fun fromJson(json: String): Session {
            val obj = JSONObject(json)
            val messagesStr = obj.optString("messages", "[]")
            val messagesArray = org.json.JSONArray(messagesStr)
            val messages = (0 until messagesArray.length()).map { i ->
                Message.fromJson(messagesArray.getString(i))
            }
            
            return Session(
                id = obj.getString("id"),
                title = obj.getString("title"),
                messages = messages,
                createdAt = obj.getLong("created_at"),
                updatedAt = obj.getLong("updated_at")
            )
        }
    }
    
    /**
     * 添加消息
     */
    fun addMessage(message: Message): Session {
        return copy(
            messages = messages + message,
            updatedAt = System.currentTimeMillis(),
            title = if (messages.isEmpty() && message.role == "user") {
                // 使用第一条用户消息作为标题（最多20字）
                message.content.take(20)
            } else {
                title
            }
        )
    }
    
    /**
     * 删除消息
     */
    fun removeMessage(messageId: String): Session {
        return copy(
            messages = messages.filter { it.id != messageId },
            updatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * 消息数据类
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCall> = emptyList()
) {
    /**
     * 转换为 JSON
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("role", role)
            put("content", content)
            put("timestamp", timestamp)
            put("tool_calls", toolCalls.map { it.toJson() }.toString())
        }
    }
    
    companion object {
        /**
         * 从 JSON 解析
         */
        fun fromJson(json: String): Message {
            val obj = JSONObject(json)
            val toolCallsStr = obj.optString("tool_calls", "[]")
            val toolCallsArray = org.json.JSONArray(toolCallsStr)
            val toolCalls = (0 until toolCallsArray.length()).map { i ->
                ToolCall.fromJson(toolCallsArray.getString(i))
            }
            
            return Message(
                id = obj.getString("id"),
                role = obj.getString("role"),
                content = obj.getString("content"),
                timestamp = obj.getLong("timestamp"),
                toolCalls = toolCalls
            )
        }
    }
}

/**
 * 工具调用记录
 */
data class ToolCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val arguments: String,
    val result: String? = null,
    val success: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("arguments", arguments)
            put("result", result)
            put("success", success)
            put("timestamp", timestamp)
        }
    }
    
    companion object {
        fun fromJson(json: String): ToolCall {
            val obj = JSONObject(json)
            return ToolCall(
                id = obj.getString("id"),
                name = obj.getString("name"),
                arguments = obj.getString("arguments"),
                result = obj.optString("result"),
                success = obj.optBoolean("success"),
                timestamp = obj.getLong("timestamp")
            )
        }
    }
}
