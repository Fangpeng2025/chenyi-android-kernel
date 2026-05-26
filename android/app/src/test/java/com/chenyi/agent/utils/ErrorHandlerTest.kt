package com.chenyi.agent.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * ErrorHandler 单元测试
 */
class ErrorHandlerTest {
    
    @Test
    fun `test unknownError returns correct message`() {
        val message = ErrorHandler.unknownError("测试操作")
        assertEquals("测试操作 失败，请重试", message)
    }
    
    @Test
    fun `test networkError includes operation name`() {
        val message = ErrorHandler.networkError("下载文件")
        assertTrue(message.contains("下载文件"))
        assertTrue(message.contains("网络错误"))
    }
    
    @Test
    fun `test toolExecutionError formats correctly`() {
        val message = ErrorHandler.toolExecutionError("screenshot", "权限被拒绝")
        assertEquals("工具 'screenshot' 执行失败：权限被拒绝", message)
    }
    
    @Test
    fun `test invalidParameterError includes type info`() {
        val message = ErrorHandler.invalidParameterError("x", "整数")
        assertTrue(message.contains("x"))
        assertTrue(message.contains("整数"))
    }
}
