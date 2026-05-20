package com.chenyi.agent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/**
 * 任务管理器
 */
class TaskManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TaskManager"
        private const val TASKS_DIR = "tasks"
    }
    
    private val tasksDir = File(context.filesDir, TASKS_DIR).apply {
        if (!exists()) mkdirs()
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null
    
    /**
     * 清理资源
     */
    fun cleanup() {
        currentJob?.cancel()
        scope.cancel()
        Log.d(TAG, "清理资源")
    }
    
    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<AutoTask> {
        return try {
            tasksDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { file ->
                    AutoTask.fromJson(file.readText())
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "加载任务列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 保存任务
     */
    fun saveTask(task: AutoTask) {
        try {
            val file = File(tasksDir, "${task.id}.json")
            file.writeText(task.toJson().toString())
            Log.d(TAG, "保存任务: ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "保存任务失败", e)
        }
    }
    
    /**
     * 删除任务
     */
    fun deleteTask(taskId: String) {
        try {
            val file = File(tasksDir, "$taskId.json")
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "删除任务: $taskId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除任务失败", e)
        }
    }
    
    /**
     * 执行任务
     */
    suspend fun executeTask(
        task: AutoTask,
        service: ChenyiAccessibilityService,
        onProgress: (Int, Int, String) -> Unit,
        onComplete: (AutoTask) -> Unit,
        onError: (String) -> Unit
    ) {
        currentJob = scope.launch {
            try {
                var currentTask = task.copy(status = TaskStatus.RUNNING)
                saveTask(currentTask)
                
                val totalSteps = currentTask.steps.size
                
                currentTask.steps.forEachIndexed { index, step ->
                    if (!isActive) {
                        // 任务被取消
                        currentTask = currentTask.copy(status = TaskStatus.CANCELLED)
                        saveTask(currentTask)
                        return@launch
                    }
                    
                    // 更新进度
                    onProgress(index + 1, totalSteps, step.description)
                    
                    // 执行步骤
                    val result = executeStep(step, service)
                    
                    if (result.success) {
                        currentTask = currentTask.copy(
                            steps = currentTask.steps.map {
                                if (it.id == step.id) {
                                    it.copy(status = StepStatus.COMPLETED, result = result.data?.toString())
                                } else {
                                    it
                                }
                            }
                        )
                    } else {
                        // 步骤失败
                        currentTask = currentTask.copy(
                            steps = currentTask.steps.map {
                                if (it.id == step.id) {
                                    it.copy(status = StepStatus.FAILED, result = result.error)
                                } else {
                                    it
                                }
                            },
                            status = TaskStatus.FAILED,
                            result = "步骤 ${index + 1} 失败: ${result.error}"
                        )
                        saveTask(currentTask)
                        onError(result.error ?: "执行失败")
                        return@launch
                    }
                    
                    saveTask(currentTask)
                    
                    // 步骤间延迟
                    delay(500)
                }
                
                // 所有步骤完成
                currentTask = currentTask.copy(
                    status = TaskStatus.COMPLETED,
                    executedAt = System.currentTimeMillis(),
                    result = "执行成功"
                )
                saveTask(currentTask)
                onComplete(currentTask)
                
            } catch (e: Exception) {
                Log.e(TAG, "执行任务失败", e)
                val failedTask = task.copy(
                    status = TaskStatus.FAILED,
                    result = e.message
                )
                saveTask(failedTask)
                onError(e.message ?: "执行失败")
            }
        }
    }
    
    /**
     * 执行单个步骤
     */
    private fun executeStep(step: TaskStep, service: ChenyiAccessibilityService): Result {
        val paramsJson = JSONObject(step.params).toString()
        val resultStr = service.executeTool(step.tool, paramsJson)
        // 将 String 结果转换为 Result 对象
        return try {
            val json = JSONObject(resultStr)
            if (json.optBoolean("success", false)) {
                Result.ok(mapOf("data" to json.optString("data", "")))
            } else {
                Result.error(json.optString("error", "执行失败"))
            }
        } catch (e: Exception) {
            Result.error(resultStr)
        }
    }
    
    /**
     * 取消当前任务
     */
    fun cancelCurrentTask() {
        currentJob?.cancel()
        currentJob = null
        Log.d(TAG, "取消当前任务")
    }
    
    /**
     * 创建自定义任务
     */
    fun createCustomTask(name: String, description: String, steps: List<TaskStep>): AutoTask {
        val task = AutoTask(
            name = name,
            description = description,
            steps = steps.sortedBy { it.order }
        )
        saveTask(task)
        return task
    }
    
    /**
     * 从模板创建任务
     */
    fun createFromTemplate(template: AutoTask): AutoTask {
        val task = template.copy(
            id = java.util.UUID.randomUUID().toString(),
            status = TaskStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            executedAt = null,
            result = null
        )
        saveTask(task)
        return task
    }
}
