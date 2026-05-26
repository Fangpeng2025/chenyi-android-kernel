package com.chenyi.agent.tools

import org.json.JSONObject

/**
 * 工具执行结果
 * 
 * 统一的工具执行结果封装，用于：
 * - 返回成功/失败状态
 * - 携带返回数据
 * - 提供错误信息
 */
data class Result(
    val success: Boolean,
    val data: JSONObject? = null,
    val error: String? = null
) {
    companion object {
        fun success(data: JSONObject? = null) = Result(true, data, null)
        fun error(message: String) = Result(false, null, message)
    }
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("success", success)
            if (data != null) put("data", data)
            if (error != null) put("error", error)
        }
    }
}
