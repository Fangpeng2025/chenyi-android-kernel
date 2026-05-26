package com.chenyi.agent.utils

import android.util.Log
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 错误追踪工具
 * 
 * 职责：
 * - 记录错误日志
 * - 收集错误上下文
 * - 提供错误报告
 */
object ErrorTracker {
    private const val TAG = "ErrorTracker"
    private const val MAX_LOGS = 100
    
    private val errorLogs = mutableListOf<ErrorLog>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 记录错误
     * 
     * @param tag 标签
     * @param message 错误信息
     * @param exception 异常（可选）
     * @param context 上下文（可选）
     */
    fun track(
        tag: String,
        message: String,
        exception: Throwable? = null,
        context: Map<String, Any?>? = null
    ) {
        val errorLog = ErrorLog(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            stackTrace = exception?.getStackTraceString(),
            context = context
        )
        
        synchronized(errorLogs) {
            errorLogs.add(errorLog)
            if (errorLogs.size > MAX_LOGS) {
                errorLogs.removeAt(0)
            }
        }
        
        Log.e(tag, message, exception)
    }
    
    /**
     * 记录工具执行错误
     */
    fun trackToolError(toolName: String, error: String, params: JSONObject? = null) {
        track(
            tag = "ToolError",
            message = "工具 '$toolName' 执行失败: $error",
            context = mapOf(
                "tool" to toolName,
                "error" to error,
                "params" to params?.toString()
            )
        )
    }
    
    /**
     * 记录网络错误
     */
    fun trackNetworkError(operation: String, url: String?, error: Throwable) {
        track(
            tag = "NetworkError",
            message = "网络请求失败: $operation",
            exception = error,
            context = mapOf(
                "operation" to operation,
                "url" to url
            )
        )
    }
    
    /**
     * 记录 UI 错误
     */
    fun trackUIError(component: String, error: String) {
        track(
            tag = "UIError",
            message = "UI 错误 [$component]: $error",
            context = mapOf("component" to component)
        )
    }
    
    /**
     * 获取所有错误日志
     */
    fun getErrorLogs(): List<ErrorLog> {
        return synchronized(errorLogs) {
            errorLogs.toList()
        }
    }
    
    /**
     * 获取最近的错误
     */
    fun getRecentErrors(count: Int = 10): List<ErrorLog> {
        return synchronized(errorLogs) {
            errorLogs.takeLast(count)
        }
    }
    
    /**
     * 清空错误日志
     */
    fun clear() {
        synchronized(errorLogs) {
            errorLogs.clear()
        }
    }
    
    /**
     * 导出错误报告
     */
    fun exportReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== 错误报告 ===")
        sb.appendLine("生成时间: ${dateFormat.format(Date())}")
        sb.appendLine("错误总数: ${errorLogs.size}")
        sb.appendLine()
        
        getErrorLogs().forEachIndexed { index, log ->
            sb.appendLine("--- 错误 ${index + 1} ---")
            sb.appendLine("时间: ${dateFormat.format(Date(log.timestamp))}")
            sb.appendLine("标签: ${log.tag}")
            sb.appendLine("信息: ${log.message}")
            
            log.stackTrace?.let {
                sb.appendLine("堆栈:")
                sb.appendLine(it)
            }
            
            log.context?.let { ctx ->
                sb.appendLine("上下文:")
                ctx.forEach { (key, value) ->
                    sb.appendLine("  $key: $value")
                }
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    /**
     * 错误日志数据类
     */
    data class ErrorLog(
        val timestamp: Long,
        val tag: String,
        val message: String,
        val stackTrace: String? = null,
        val context: Map<String, Any?>? = null
    )
    
    /**
     * 获取异常堆栈字符串
     */
    private fun Throwable.getStackTraceString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        this.printStackTrace(pw)
        return sw.toString()
    }
}
