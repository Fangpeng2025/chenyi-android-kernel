package com.chenyi.agent

import android.content.Context
import android.util.Log

/**
 * Rust 内核 JNI 桥接
 */
class Kernel(private val context: Context) {

    companion object {
        private const val TAG = "Kernel"
        
        init {
            System.loadLibrary("chenyi")
        }
    }

    // JNI 方法
    private external fun nativeInit(dataDir: String): Boolean
    private external fun nativeRegisterToolCallback(callbackPtr: Long)
    private external fun nativeChat(message: String): String
    private external fun nativeExecuteTool(tool: String, params: String): String
    private external fun nativeGetStatus(): String
    private external fun nativeDestroy()

    /**
     * 初始化内核
     */
    fun init(): Boolean {
        val dataDir = context.filesDir.absolutePath
        val success = nativeInit(dataDir)
        
        Log.d(TAG, "内核初始化: $success")
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
     * 销毁内核
     */
    fun destroy() {
        nativeDestroy()
        Log.d(TAG, "内核已销毁")
    }
}

/**
 * 执行结果
 */
data class Result(
    val success: Boolean,
    val response: String?,  // 添加 response 字段
    val data: Map<String, Any?>?,
    val error: String?
) {
    companion object {
        fun fromJson(json: String): Result {
            val obj = org.json.JSONObject(json)
            return Result(
                success = obj.optBoolean("success", false),
                response = obj.optString("response").takeIf { it.isNotEmpty() },
                data = obj.optJSONObject("data")?.toMap(),
                error = obj.optString("error").takeIf { it.isNotEmpty() }
            )
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
    val toolsCount: Int
) {
    companion object {
        fun fromJson(json: String): Status {
            val obj = org.json.JSONObject(json)
            return Status(
                initialized = obj.optBoolean("initialized", false),
                dataDir = obj.optString("data_dir", ""),
                memoryEntries = obj.optInt("memory_entries", 0),
                toolsCount = obj.optInt("tools_count", 0)
            )
        }
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