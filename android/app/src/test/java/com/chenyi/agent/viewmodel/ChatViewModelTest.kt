package com.chenyi.agent.viewmodel

import org.junit.Test
import org.junit.Assert.*

/**
 * ChatViewModel 单元测试
 */
class ChatViewModelTest {
    
    @Test
    fun `test sendMessage adds user message`() {
        val viewModel = ChatViewModel()
        
        viewModel.sendMessage("Hello")
        
        val messages = viewModel.messages.value
        assertEquals(2, messages.size) // 用户消息 + AI 回复
        assertTrue(messages[0].isUser)
        assertEquals("Hello", messages[0].content)
    }
    
    @Test
    fun `test clearMessages removes all messages`() {
        val viewModel = ChatViewModel()
        
        viewModel.sendMessage("Test 1")
        viewModel.sendMessage("Test 2")
        viewModel.clearMessages()
        
        assertEquals(0, viewModel.messages.value.size)
        assertEquals(0, viewModel.tokenUsage.value)
    }
    
    @Test
    fun `test token usage does not exceed max`() {
        val viewModel = ChatViewModel()
        
        // 发送大量消息
        repeat(100) {
            viewModel.sendMessage("A".repeat(1000))
        }
        
        assertTrue(viewModel.tokenUsage.value <= ChatViewModel.MAX_TOKENS)
    }
}
