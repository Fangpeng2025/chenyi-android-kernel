package com.chenyi.agent.utils

/**
 * 错误处理工具
 * 
 * 提供统一的错误信息和异常处理
 */
object ErrorHandler {
    
    // ========== 通用错误 ==========
    
    fun unknownError(operation: String): String = 
        "$operation 失败，请重试"
    
    fun networkError(operation: String): String = 
        "网络错误：$operation 失败，请检查网络连接"
    
    fun timeoutError(operation: String): String = 
        "超时：$operation 响应时间过长"
    
    // ========== 工具执行错误 ==========
    
    fun toolExecutionError(toolName: String, reason: String): String =
        "工具 '$toolName' 执行失败：$reason"
    
    fun toolNotFoundError(toolName: String): String =
        "未找到工具：$toolName"
    
    fun invalidParameterError(paramName: String, expectedType: String): String =
        "参数错误：'$paramName' 应为 $expectedType 类型"
    
    // ========== 配置错误 ==========
    
    fun configLoadError(configName: String): String =
        "配置加载失败：$configName"
    
    fun configSaveError(configName: String): String =
        "配置保存失败：$configName"
    
    fun invalidApiKeyError(): String =
        "API Key 格式无效，请检查"
    
    fun invalidEndpointError(): String =
        "API Endpoint 格式无效，请检查"
    
    // ========== UI 错误 ==========
    
    fun dialogShowError(dialogName: String): String =
        "无法显示对话框：$dialogName"
    
    fun permissionDeniedError(permission: String): String =
        "权限被拒绝：$permission"
    
    // ========== 无障碍服务错误 ==========
    
    fun accessibilityNotEnabled(): String =
        "无障碍服务未启用，请在设置中开启"
    
    fun gestureFailed(gesture: String): String =
        "手势执行失败：$gesture"
    
    fun nodeNotFoundError(selector: String): String =
        "未找到界面元素：$selector"
    
    // ========== OCR 错误 ==========
    
    fun ocrInitError(): String =
        "OCR 引擎初始化失败"
    
    fun ocrRecognitionError(reason: String): String =
        "文字识别失败：$reason"
    
    // ========== 文件错误 ==========
    
    fun fileNotFoundError(filePath: String): String =
        "文件不存在：$filePath"
    
    fun fileReadError(filePath: String): String =
        "文件读取失败：$filePath"
    
    fun fileWriteError(filePath: String): String =
        "文件写入失败：$filePath"
    
    // ========== 更新错误 ==========
    
    fun updateCheckError(): String =
        "检查更新失败，请稍后重试"
    
    fun updateDownloadError(): String =
        "下载更新失败，请检查网络连接"
    
    fun updateInstallError(): String =
        "安装更新失败，请手动安装"
}

/**
 * 扩展函数：安全的执行操作
 */
inline fun <T> safeExecute(operation: String, block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(Exception(ErrorHandler.unknownError(operation), e))
    }
}

/**
 * 扩展函数：记录错误日志
 */
fun Exception.logError(tag: String, operation: String) {
    android.util.Log.e(tag, "$operation 失败", this)
}
