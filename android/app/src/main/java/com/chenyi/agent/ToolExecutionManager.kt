package com.chenyi.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 工具执行状态
 */
sealed class ToolExecutionState {
    object Idle : ToolExecutionState()
    data class Executing(
        val toolName: String,
        val arguments: String,
        val round: Int,
        val total: Int
    ) : ToolExecutionState()
    data class Success(val result: String) : ToolExecutionState()
    data class Error(val message: String) : ToolExecutionState()
    data class Progress(
        val completed: Int,
        val total: Int,
        val currentTool: String,
        val logs: List<ToolLog>
    ) : ToolExecutionState()
}

/**
 * 工具执行日志
 */
data class ToolLog(
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val status: ToolStatus,
    val message: String? = null
)

enum class ToolStatus {
    PENDING,    // 等待执行
    EXECUTING,  // 执行中
    SUCCESS,    // 成功
    ERROR       // 失败
}

/**
 * 工具执行管理器
 */
class ToolExecutionManager {
    private val _state = MutableStateFlow<ToolExecutionState>(ToolExecutionState.Idle)
    val state: StateFlow<ToolExecutionState> = _state.asStateFlow()
    
    private val _logs = MutableStateFlow<List<ToolLog>>(emptyList())
    val logs: StateFlow<List<ToolLog>> = _logs.asStateFlow()
    
    /**
     * 开始执行工具
     */
    fun startExecution(totalRounds: Int) {
        _logs.value = emptyList()
        _state.value = ToolExecutionState.Progress(
            completed = 0,
            total = totalRounds,
            currentTool = "",
            logs = emptyList()
        )
    }
    
    /**
     * 更新工具执行状态
     */
    fun updateToolExecution(
        toolName: String,
        arguments: String,
        round: Int,
        total: Int,
        status: ToolStatus,
        message: String? = null
    ) {
        val log = ToolLog(
            toolName = toolName,
            status = status,
            message = message
        )
        
        val currentLogs = _logs.value + log
        _logs.value = currentLogs
        
        _state.value = ToolExecutionState.Progress(
            completed = round,
            total = total,
            currentTool = toolName,
            logs = currentLogs
        )
    }
    
    /**
     * 工具执行成功
     */
    fun toolSuccess(toolName: String, result: String) {
        val log = ToolLog(
            toolName = toolName,
            status = ToolStatus.SUCCESS,
            message = result
        )
        
        _logs.value = _logs.value + log
    }
    
    /**
     * 工具执行失败
     */
    fun toolError(toolName: String, error: String) {
        val log = ToolLog(
            toolName = toolName,
            status = ToolStatus.ERROR,
            message = error
        )
        
        _logs.value = _logs.value + log
    }
    
    /**
     * 完成执行
     */
    fun finishExecution() {
        _state.value = ToolExecutionState.Idle
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _state.value = ToolExecutionState.Idle
        _logs.value = emptyList()
    }
}
