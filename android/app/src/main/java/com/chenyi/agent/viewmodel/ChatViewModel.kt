package com.chenyi.agent.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.chenyi.agent.ui.components.ChatMessage

/**
 * 聊天 ViewModel - 管理聊天状态和逻辑
 * 
 * 职责：
 * - 聊天消息管理
 * - Token 使用统计
 * - 消息发送和接收
 */
class ChatViewModel : ViewModel() {
    
    // ========== 聊天状态 ==========
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _tokenUsage = MutableStateFlow(0)
    val tokenUsage: StateFlow<Int> = _tokenUsage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // ========== 常量 ==========
    
    companion object {
        const val MAX_TOKENS = 4096
    }
    
    // ========== 聊天操作 ==========
    
    /**
     * 发送消息
     */
    fun sendMessage(content: String) {
        // 添加用户消息
        val userMessage = ChatMessage(
            content = content,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage
        
        // 模拟 AI 回复（实际应用中应该调用 Rust Kernel）
        simulateAiReply(content)
    }
    
    /**
     * 模拟 AI 回复
     */
    private fun simulateAiReply(userMessage: String) {
        _isLoading.value = true
        
        // 添加 AI 消息
        val aiMessage = ChatMessage(
            content = "收到！我正在处理你的请求...",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + aiMessage
        
        // 更新 token 使用量（模拟）
        _tokenUsage.value = (_tokenUsage.value + userMessage.length / 4).coerceAtMost(MAX_TOKENS)
        
        _isLoading.value = false
    }
    
    /**
     * 清空聊天记录
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _tokenUsage.value = 0
    }
    
    /**
     * 添加工具执行结果
     */
    fun addToolResult(toolName: String, result: String) {
        val toolMessage = ChatMessage(
            content = "🔧 $toolName: $result",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + toolMessage
    }
}
