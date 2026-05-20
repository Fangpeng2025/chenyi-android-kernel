package com.chenyi.agent

import org.json.JSONObject
import java.util.UUID

/**
 * 自动任务
 */
data class AutoTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val steps: List<TaskStep>,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null,
    val result: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("steps", steps.map { it.toJson() }.toString())
            put("status", status.name)
            put("created_at", createdAt)
            put("executed_at", executedAt)
            put("result", result)
        }
    }
    
    companion object {
        fun fromJson(json: String): AutoTask {
            val obj = JSONObject(json)
            val stepsStr = obj.optString("steps", "[]")
            val stepsArray = org.json.JSONArray(stepsStr)
            val steps = (0 until stepsArray.length()).map { i ->
                TaskStep.fromJson(stepsArray.getString(i))
            }
            
            return AutoTask(
                id = obj.getString("id"),
                name = obj.getString("name"),
                description = obj.optString("description"),
                steps = steps,
                status = TaskStatus.valueOf(obj.getString("status")),
                createdAt = obj.getLong("created_at"),
                executedAt = obj.optLong("executed_at"),
                result = obj.optString("result")
            )
        }
    }
}

/**
 * 任务步骤
 */
data class TaskStep(
    val id: String = UUID.randomUUID().toString(),
    val tool: String,
    val params: Map<String, Any>,
    val description: String = "",
    val order: Int = 0,
    val status: StepStatus = StepStatus.PENDING,
    val result: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("tool", tool)
            put("params", JSONObject(params).toString())
            put("description", description)
            put("order", order)
            put("status", status.name)
            put("result", result)
        }
    }
    
    companion object {
        fun fromJson(json: String): TaskStep {
            val obj = JSONObject(json)
            val paramsStr = obj.optString("params", "{}")
            val paramsObj = JSONObject(paramsStr)
            val params = paramsObj.keys().asSequence().associateWith { 
                paramsObj.get(it) 
            }
            
            return TaskStep(
                id = obj.getString("id"),
                tool = obj.getString("tool"),
                params = params,
                description = obj.optString("description"),
                order = obj.getInt("order"),
                status = StepStatus.valueOf(obj.getString("status")),
                result = obj.optString("result")
            )
        }
    }
}

enum class TaskStatus {
    PENDING,    // 待执行
    RUNNING,    // 执行中
    COMPLETED,  // 已完成
    FAILED,     // 失败
    CANCELLED   // 已取消
}

enum class StepStatus {
    PENDING,    // 待执行
    RUNNING,    // 执行中
    COMPLETED,  // 已完成
    FAILED      // 失败
}

/**
 * 预设任务模板
 */
object TaskTemplates {
    /**
     * 抖音点赞第一个视频
     */
    fun douyinLikeFirstVideo(): AutoTask {
        return AutoTask(
            name = "抖音点赞第一个视频",
            description = "打开抖音，点击第一个视频，点赞",
            steps = listOf(
                TaskStep(
                    tool = "openApp",
                    params = mapOf("package" to "com.ss.android.ugc.aweme"),
                    description = "打开抖音",
                    order = 0
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 610, "y" to 2550),
                    description = "点击第一个视频",
                    order = 1
                ),
                TaskStep(
                    tool = "wait",
                    params = mapOf("seconds" to 2),
                    description = "等待视频加载",
                    order = 2
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 1100, "y" to 2400),
                    description = "点击点赞按钮",
                    order = 3
                )
            )
        )
    }
    
    /**
     * 微信发送消息
     */
    fun wechatSendMessage(contact: String, message: String): AutoTask {
        return AutoTask(
            name = "微信发送消息",
            description = "打开微信，发送消息给 $contact",
            steps = listOf(
                TaskStep(
                    tool = "openApp",
                    params = mapOf("package" to "com.tencent.mm"),
                    description = "打开微信",
                    order = 0
                ),
                TaskStep(
                    tool = "wait",
                    params = mapOf("seconds" to 2),
                    description = "等待微信加载",
                    order = 1
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 540, "y" to 200),
                    description = "点击搜索",
                    order = 2
                ),
                TaskStep(
                    tool = "typeText",
                    params = mapOf("text" to contact),
                    description = "输入联系人名称",
                    order = 3
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 540, "y" to 400),
                    description = "选择联系人",
                    order = 4
                ),
                TaskStep(
                    tool = "typeText",
                    params = mapOf("text" to message),
                    description = "输入消息",
                    order = 5
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 1000, "y" to 2400),
                    description = "发送消息",
                    order = 6
                )
            )
        )
    }
    
    /**
     * 浏览器搜索
     */
    fun browserSearch(query: String): AutoTask {
        return AutoTask(
            name = "浏览器搜索",
            description = "打开浏览器，搜索 $query",
            steps = listOf(
                TaskStep(
                    tool = "openApp",
                    params = mapOf("package" to "com.android.browser"),
                    description = "打开浏览器",
                    order = 0
                ),
                TaskStep(
                    tool = "wait",
                    params = mapOf("seconds" to 2),
                    description = "等待浏览器加载",
                    order = 1
                ),
                TaskStep(
                    tool = "tap",
                    params = mapOf("x" to 540, "y" to 200),
                    description = "点击搜索框",
                    order = 2
                ),
                TaskStep(
                    tool = "typeText",
                    params = mapOf("text" to query),
                    description = "输入搜索内容",
                    order = 3
                ),
                TaskStep(
                    tool = "pressKey",
                    params = mapOf("key" to "enter"),
                    description = "按回车搜索",
                    order = 4
                )
            )
        )
    }
    
    /**
     * 截图并 OCR
     */
    fun screenshotAndOcr(): AutoTask {
        return AutoTask(
            name = "截图并识别文字",
            description = "截取当前屏幕并识别文字",
            steps = listOf(
                TaskStep(
                    tool = "screenshot",
                    params = emptyMap(),
                    description = "截取屏幕",
                    order = 0
                ),
                TaskStep(
                    tool = "ocr",
                    params = emptyMap(),
                    description = "识别文字",
                    order = 1
                )
            )
        )
    }
    
    /**
     * 获取所有模板
     */
    fun getAllTemplates(): List<AutoTask> {
        return listOf(
            douyinLikeFirstVideo(),
            screenshotAndOcr()
        )
    }
}
