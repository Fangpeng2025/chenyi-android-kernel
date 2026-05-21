package com.chenyi.agent

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rust 内核 JNI 桥接
 */
class Kernel(private val context: Context) {

    companion object {
        private const val TAG = "Kernel"
        
        init {
            // 尝试加载热更新的库
            try {
                val hotUpdateManager = HotUpdateManager(android.app.Application())
                if (!hotUpdateManager.loadUpdatedLibrary()) {
                    // 如果没有热更新的库，加载默认库
                    System.loadLibrary("chenyi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载热更新库失败，使用默认库", e)
                System.loadLibrary("chenyi")
            }
        }
    }

    // JNI 方法
    private external fun nativeInit(dataDir: String): Boolean
    private external fun nativeChat(message: String): String
    private external fun nativeExecuteTool(tool: String, params: String): String
    private external fun nativeGetStatus(): String
    private external fun nativeDestroy()
    private external fun nativeUpdateConfig(configJson: String): Boolean
    private external fun nativeClearHistory(): Boolean
    private external fun nativeSubmitToolResults(toolResultsJson: String): String

    // 工具执行回调（由 AccessibilityService 设置）
    private var toolCallback: ((String, String) -> String)? = null
    private val initialized = AtomicBoolean(false)

    /**
     * 初始化内核
     */
    fun init(): Boolean {
        if (initialized.get()) {
            Log.w(TAG, "内核已初始化")
            return true
        }
        
        val dataDir = context.filesDir.absolutePath
        val success = nativeInit(dataDir)
        
        if (success) {
            initialized.set(true)
            Log.d(TAG, "内核初始化成功")
        } else {
            Log.e(TAG, "内核初始化失败")
        }
        
        return success
    }
    
    /**
     * 设置工具执行回调（已弃用）
     * 当前架构下，工具执行由 chatWithTools 自动处理
     */
    @Deprecated("使用 chatWithTools 自动处理工具执行")
    fun setToolCallback(callback: (String, String) -> String) {
        this.toolCallback = callback
        Log.d(TAG, "工具回调已设置（已弃用）")
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(config: KernelConfig): Boolean {
        val json = config.toJson()
        val success = nativeUpdateConfig(json)
        Log.d(TAG, "更新配置: $success")
        return success
    }

    /**
     * 发送消息
     */
    fun chat(message: String): Result {
        return try {
            // 先检查无障碍服务
            val service = ChenyiAccessibilityService.getInstance()
            if (service == null) {
                Log.w(TAG, "无障碍服务未连接，仅使用 LLM")
            }
            
            val json = nativeChat(message)
            Result.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "聊天失败", e)
            Result.error(e.message ?: "聊天失败")
        }
    }
    
    /**
     * 发送消息（带工具执行支持）
     * 这个方法会自动处理工具调用循环
     */
    fun chatWithTools(message: String, maxRounds: Int = 10): Result {
        val startTime = System.currentTimeMillis()
        val timeoutMs = 120_000L // 2分钟总超时
        var rounds = 0
        
        // 第一次调用
        var result = chat(message)
        
        while (rounds < maxRounds) {
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                Log.w(TAG, "工具执行超时 (${System.currentTimeMillis() - startTime}ms)")
                return Result.error("操作超时，请稍后重试")
            }
            
            if (result.success) {
                // 成功返回响应
                return result
            }
            
            // 检查是否需要执行工具
            val error = result.error ?: ""
            if (error.startsWith("TOOL_CALLS_REQUIRED:")) {
                Log.d(TAG, "第 ${rounds + 1} 轮：需要执行工具")
                
                try {
                    // 解析工具调用
                    val toolCallsJson = error.substringAfter("TOOL_CALLS_REQUIRED:")
                    val toolCallsArray = org.json.JSONArray(toolCallsJson)
                    
                    // 执行每个工具
                    val service = ChenyiAccessibilityService.getInstance()
                    if (service == null) {
                        return Result.error("无障碍服务未连接，无法执行工具。请在设置中开启无障碍服务。")
                    }
                    
                    // 收集工具结果
                    val toolResults = org.json.JSONArray()
                    for (i in 0 until toolCallsArray.length()) {
                        // 检查超时
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            return Result.error("工具执行超时")
                        }
                        
                        val toolCall = toolCallsArray.getJSONObject(i)
                        val toolId = toolCall.getString("id")
                        val function = toolCall.getJSONObject("function")
                        val toolName = function.getString("name")
                        val toolArgs = function.getString("arguments")
                        
                        Log.d(TAG, "执行工具: $toolName ($toolArgs)")
                        val toolResult = service.executeTool(toolName, toolArgs)
                        
                        // 添加到结果数组
                        val resultObj = org.json.JSONObject()
                        resultObj.put("tool_call_id", toolId)
                        resultObj.put("result", toolResult)
                        toolResults.put(resultObj)
                    }
                    
                    Log.d(TAG, "提交 ${toolResults.length()} 个工具结果")
                    
                    // 提交工具结果，继续对话
                    val continueResultJson = nativeSubmitToolResults(toolResults.toString())
                    result = Result.fromJson(continueResultJson)
                    
                    rounds++
                } catch (e: Exception) {
                    Log.e(TAG, "工具执行失败", e)
                    return Result.error("工具执行失败: ${e.message}")
                }
            } else {
                // 其他错误，直接返回
                return result
            }
        }
        
        return Result.error("达到最大工具调用轮数 ($maxRounds)")
    }

    /**
     * 执行工具（通过 AccessibilityService）
     */
    fun executeTool(tool: String, params: Map<String, Any> = emptyMap()): Result {
        val paramsJson = if (params.isEmpty()) "{}" else {
            val obj = org.json.JSONObject()
            params.forEach { (k, v) ->
                obj.put(k, v)
            }
            obj.toString()
        }
        
        return executeToolJson(tool, paramsJson)
    }
    
    /**
     * 执行工具（JSON 参数）
     */
    fun executeToolJson(tool: String, paramsJson: String): Result {
        return try {
            val service = ChenyiAccessibilityService.getInstance()
            if (service != null) {
                val json = service.executeTool(tool, paramsJson)
                Result.fromJson(json)
            } else {
                Result.error("无障碍服务未连接")
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行工具失败: $tool", e)
            Result.error(e.message ?: "执行失败")
        }
    }

    /**
     * 获取内核状态
     */
    fun getStatus(): Status {
        val json = nativeGetStatus()
        return Status.fromJson(json)
    }
    
    /**
     * 清除会话历史
     */
    fun clearHistory(): Boolean {
        return nativeClearHistory()
    }

    /**
     * 销毁内核
     */
    fun destroy() {
        nativeDestroy()
        initialized.set(false)
        Log.d(TAG, "内核已销毁")
    }
}

/**
 * 执行结果
 */
data class Result(
    val success: Boolean,
    val response: String?,
    val data: Map<String, Any?>?,
    val error: String?
) {
    companion object {
        fun fromJson(json: String): Result {
            return try {
                val obj = org.json.JSONObject(json)
                Result(
                    success = obj.optBoolean("success", false),
                    response = obj.optString("response").takeIf { it.isNotEmpty() },
                    data = obj.optJSONObject("data")?.toMap(),
                    error = obj.optString("error").takeIf { it.isNotEmpty() }
                )
            } catch (e: Exception) {
                Log.e("Result", "解析 JSON 失败: ${e.message}", e)
                Result.error("解析响应失败: ${e.message}")
            }
        }

        fun ok(data: Map<String, Any?>): Result = Result(true, null, data, null)
        fun error(msg: String): Result = Result(false, null, null, msg)
    }

    fun toJson(): String {
        val json = org.json.JSONObject()
        json.put("success", success)
        if (response != null) {
            json.put("response", response)
        }
        if (data != null) {
            json.put("data", org.json.JSONObject(data))
        }
        if (error != null) {
            json.put("error", error)
        }
        return json.toString()
    }
}

/**
 * 内核状态
 */
data class Status(
    val initialized: Boolean,
    val dataDir: String,
    val memoryEntries: Int,
    val toolsCount: Int,
    val historyLength: Int = 0
) {
    companion object {
        fun fromJson(json: String): Status {
            val obj = org.json.JSONObject(json)
            return Status(
                initialized = obj.optBoolean("initialized", false),
                dataDir = obj.optString("data_dir", ""),
                memoryEntries = obj.optInt("memory_entries", 0),
                toolsCount = obj.optInt("tools_count", 0),
                historyLength = obj.optInt("history_length", 0)
            )
        }
    }
}

/**
 * 内核配置更新
 */
data class KernelConfig(
    val apiEndpoint: String,
    val apiKey: String,
    val modelName: String,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
) {
    fun toJson(): String {
        val json = org.json.JSONObject()
        json.put("endpoint", apiEndpoint)
        json.put("api_key", apiKey)
        json.put("model", modelName)
        json.put("max_tokens", maxTokens)
        json.put("temperature", temperature.toDouble())  // Float 转 Double
        return json.toString()
    }
}

/**
 * JSONObject 扩展：转换为 Map
 */
fun org.json.JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = this.get(key)
        map[key] = when (value) {
            is org.json.JSONObject -> value.toMap()
            is org.json.JSONArray -> value.toList()
            org.json.JSONObject.NULL -> null
            else -> value
        }
    }
    return map
}

/**
 * JSONArray 扩展：转换为 List
 */
fun org.json.JSONArray.toList(): List<Any?> {
    val list = mutableListOf<Any?>()
    for (i in 0 until this.length()) {
        val value = this.get(i)
        list.add(when (value) {
            is org.json.JSONObject -> value.toMap()
            is org.json.JSONArray -> value.toList()
            org.json.JSONObject.NULL -> null
            else -> value
        })
    }
    return list
}
