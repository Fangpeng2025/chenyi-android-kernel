package com.chenyi.agent

import android.content.Context

/**
 * Rust 内核 JNI 桥接
 */
class Kernel(private val context: Context) {

    companion object {
        init {
            System.loadLibrary("chenyi")
        }
    }

    // JNI 方法
    private external fun nativeInit(dataDir: String): Boolean
    private external fun nativeChat(message: String): String
    private external fun nativeExecuteTool(tool: String, params: String): String
    private external fun nativeGetStatus(): String
    private external fun nativeDestroy()

    /**
     * 初始化内核
     */
    fun init(): Boolean {
        val dataDir = context.filesDir.absolutePath
        return nativeInit(dataDir)
    }

    /**
     * 发送消息
     */
    fun chat(message: String): Result {
        val json = nativeChat(message)
        return Result.fromJson(json)
    }

    /**
     * 执行工具
     */
    fun executeTool(tool: String, params: Map<String, Any> = emptyMap()): Result {
        val paramsJson = if (params.isEmpty()) "{}" else {
            val obj = org.json.JSONObject()
            params.forEach { (k, v) ->
                obj.put(k, v)
            }
            obj.toString()
        }
        val json = nativeExecuteTool(tool, paramsJson)
        return Result.fromJson(json)
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
    }
}

/**
 * 执行结果
 */
data class Result(
    val success: Boolean,
    val data: Map<String, Any?>?,
    val error: String?
) {
    companion object {
        fun fromJson(json: String): Result {
            val obj = org.json.JSONObject(json)
            return Result(
                success = obj.optBoolean("success", false),
                data = obj.optJSONObject("data")?.toMap(),
                error = obj.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
}

/**
 * 内核状态
 */
data class Status(
    val initialized: Boolean,
    val dataDir: String,
    val memoryEntries: Int
) {
    companion object {
        fun fromJson(json: String): Status {
            val obj = org.json.JSONObject(json)
            return Status(
                initialized = obj.optBoolean("initialized", false),
                dataDir = obj.optString("data_dir", ""),
                memoryEntries = obj.optInt("memory_entries", 0)
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