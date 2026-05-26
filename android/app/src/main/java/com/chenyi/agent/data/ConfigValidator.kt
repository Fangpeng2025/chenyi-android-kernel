package com.chenyi.agent.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * 配置验证工具
 * 
 * 功能：
 * - API Key 格式验证
 * - Endpoint 连接测试
 * - 模型名称验证
 */
class ConfigValidator(private val context: Context) {

    // ==================== API Key 验证 ====================
    
    /**
     * 验证 API Key 格式
     * 
     * 支持的格式：
     * - OpenAI: sk-xxx (至少48字符)
     * - 智谱: xxx.xxx.xxx (JWT格式)
     * - Anthropic: sk-ant-xxx
     * - DeepSeek: sk-xxx
     * - 自定义: 最少32字符
     * 
     * @return ValidationResult
     */
    fun validateApiKey(apiKey: String, provider: String = "auto"): ValidationResult {
        if (apiKey.isBlank()) {
            return ValidationResult.Error("API Key 不能为空")
        }
        
        // 移除前后空格
        val trimmedKey = apiKey.trim()
        
        // 根据提供商验证
        return when (provider) {
            "openai" -> validateOpenAIKey(trimmedKey)
            "zhipu", "glm" -> validateZhipuKey(trimmedKey)
            "anthropic" -> validateAnthropicKey(trimmedKey)
            "deepseek" -> validateDeepSeekKey(trimmedKey)
            "auto" -> validateAutoKey(trimmedKey)
            else -> validateCustomKey(trimmedKey)
        }
    }
    
    private fun validateOpenAIKey(key: String): ValidationResult {
        if (!key.startsWith("sk-")) {
            return ValidationResult.Error("OpenAI API Key 应以 'sk-' 开头")
        }
        if (key.length < 48) {
            return ValidationResult.Error("OpenAI API Key 长度不足（至少48字符）")
        }
        return ValidationResult.Success("OpenAI API Key 格式正确")
    }
    
    private fun validateZhipuKey(key: String): ValidationResult {
        // 智谱 API Key 格式: xxx.xxx.xxx (JWT)
        val jwtPattern = Pattern.compile("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")
        if (!jwtPattern.matcher(key).matches()) {
            return ValidationResult.Error("智谱 API Key 格式应为 JWT 格式 (xxx.xxx.xxx)")
        }
        return ValidationResult.Success("智谱 API Key 格式正确")
    }
    
    private fun validateAnthropicKey(key: String): ValidationResult {
        if (!key.startsWith("sk-ant-")) {
            return ValidationResult.Error("Anthropic API Key 应以 'sk-ant-' 开头")
        }
        if (key.length < 40) {
            return ValidationResult.Error("Anthropic API Key 长度不足")
        }
        return ValidationResult.Success("Anthropic API Key 格式正确")
    }
    
    private fun validateDeepSeekKey(key: String): ValidationResult {
        if (!key.startsWith("sk-")) {
            return ValidationResult.Error("DeepSeek API Key 应以 'sk-' 开头")
        }
        if (key.length < 32) {
            return ValidationResult.Error("DeepSeek API Key 长度不足")
        }
        return ValidationResult.Success("DeepSeek API Key 格式正确")
    }
    
    private fun validateAutoKey(key: String): ValidationResult {
        // 自动检测格式
        if (key.startsWith("sk-ant-")) return validateAnthropicKey(key)
        if (key.startsWith("sk-") && key.length >= 48) return validateOpenAIKey(key)
        if (key.startsWith("sk-") && key.length >= 32) return validateDeepSeekKey(key)
        if (key.contains(".") && key.split(".").size == 3) return validateZhipuKey(key)
        
        // 自定义格式
        return validateCustomKey(key)
    }
    
    private fun validateCustomKey(key: String): ValidationResult {
        if (key.length < 32) {
            return ValidationResult.Warning("API Key 长度较短，可能无效")
        }
        if (key.contains(" ") || key.contains("\n")) {
            return ValidationResult.Error("API Key 不能包含空格或换行")
        }
        return ValidationResult.Success("API Key 格式已接受")
    }

    // ==================== Endpoint 验证 ====================
    
    /**
     * 验证 Endpoint 格式
     */
    fun validateEndpointFormat(endpoint: String): ValidationResult {
        if (endpoint.isBlank()) {
            return ValidationResult.Error("Endpoint 不能为空")
        }
        
        val trimmed = endpoint.trim()
        
        // 移除协议前缀
        val host = trimmed.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        
        // 验证域名格式
        val domainPattern = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
            "(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
        )
        
        if (!domainPattern.matcher(host).matches()) {
            return ValidationResult.Error("Endpoint 格式不正确，应为有效域名")
        }
        
        return ValidationResult.Success("Endpoint 格式正确")
    }
    
    /**
     * 测试 Endpoint 连接
     * 
     * @param endpoint Endpoint 地址
     * @param timeoutMs 超时时间（毫秒）
     * @return ConnectionTestResult
     */
    suspend fun testEndpointConnection(
        endpoint: String,
        timeoutMs: Int = 5000
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        
        // 检查网络
        if (!isNetworkAvailable()) {
            return@withContext ConnectionTestResult.Error("网络不可用")
        }
        
        try {
            val host = endpoint.trim()
                .removePrefix("https://").removePrefix("http://")
                .removeSuffix("/")
            
            // 构建测试 URL
            val testUrl = "https://$host/v1/models"
            
            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            val responseTime = connection.headerFields?.get("date")?.firstOrNull()
            
            connection.disconnect()
            
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> 
                    ConnectionTestResult.Success("连接成功", responseTime ?: "")
                HttpURLConnection.HTTP_UNAUTHORIZED ->
                    ConnectionTestResult.Warning("连接成功，但需要认证")
                HttpURLConnection.HTTP_NOT_FOUND ->
                    ConnectionTestResult.Warning("Endpoint 可访问，但 API 路径不匹配")
                in 500..599 ->
                    ConnectionTestResult.Error("服务器错误: $responseCode")
                else ->
                    ConnectionTestResult.Warning("返回状态码: $responseCode")
            }
        } catch (e: java.net.SocketTimeoutException) {
            ConnectionTestResult.Error("连接超时")
        } catch (e: java.net.UnknownHostException) {
            ConnectionTestResult.Error("无法解析域名")
        } catch (e: Exception) {
            ConnectionTestResult.Error("连接失败: ${e.message}")
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ==================== 模型验证 ====================
    
    /**
     * 验证模型名称
     */
    fun validateModelName(modelName: String): ValidationResult {
        if (modelName.isBlank()) {
            return ValidationResult.Error("模型名称不能为空")
        }
        
        // 验证格式
        val modelPattern = Pattern.compile("^[a-zA-Z0-9-_.]+$")
        if (!modelPattern.matcher(modelName.trim()).matches()) {
            return ValidationResult.Error("模型名称包含非法字符")
        }
        
        return ValidationResult.Success("模型名称格式正确")
    }

    // ==================== 综合验证 ====================
    
    /**
     * 综合配置验证
     */
    suspend fun validateFullConfig(
        apiKey: String,
        endpoint: String,
        modelName: String
    ): FullConfigValidationResult {
        
        val apiKeyResult = validateApiKey(apiKey)
        val endpointFormatResult = validateEndpointFormat(endpoint)
        val modelNameResult = validateModelName(modelName)
        val connectionResult = testEndpointConnection(endpoint)
        
        return FullConfigValidationResult(
            apiKeyValidation = apiKeyResult,
            endpointValidation = endpointFormatResult,
            connectionTest = connectionResult,
            modelNameValidation = modelNameResult,
            isValid = apiKeyResult.isSuccess && 
                      endpointFormatResult.isSuccess && 
                      modelNameResult.isSuccess &&
                      !connectionResult.isError
        )
    }
}

// ==================== Result Types ====================

sealed class ValidationResult {
    abstract val message: String
    abstract val isSuccess: Boolean
    abstract val isError: Boolean
    abstract val isWarning: Boolean
    
    data class Success(override val message: String) : ValidationResult() {
        override val isSuccess = true
        override val isError = false
        override val isWarning = false
    }
    
    data class Warning(override val message: String) : ValidationResult() {
        override val isSuccess = true
        override val isError = false
        override val isWarning = true
    }
    
    data class Error(override val message: String) : ValidationResult() {
        override val isSuccess = false
        override val isError = true
        override val isWarning = false
    }
}

sealed class ConnectionTestResult {
    abstract val message: String
    abstract val isSuccess: Boolean
    abstract val isError: Boolean
    
    data class Success(
        override val message: String,
        val serverTime: String = ""
    ) : ConnectionTestResult() {
        override val isSuccess = true
        override val isError = false
    }
    
    data class Warning(override val message: String) : ConnectionTestResult() {
        override val isSuccess = true
        override val isError = false
    }
    
    data class Error(override val message: String) : ConnectionTestResult() {
        override val isSuccess = false
        override val isError = true
    }
}

data class FullConfigValidationResult(
    val apiKeyValidation: ValidationResult,
    val endpointValidation: ValidationResult,
    val connectionTest: ConnectionTestResult,
    val modelNameValidation: ValidationResult,
    val isValid: Boolean
) {
    fun getAllErrors(): List<String> {
        return listOf(
            if (apiKeyValidation.isError) "API Key: ${apiKeyValidation.message}" else null,
            if (endpointValidation.isError) "Endpoint: ${endpointValidation.message}" else null,
            if (connectionTest.isError) "连接: ${connectionTest.message}" else null,
            if (modelNameValidation.isError) "模型: ${modelNameValidation.message}" else null
        ).filterNotNull()
    }
    
    fun getAllWarnings(): List<String> {
        return listOf(
            if (apiKeyValidation.isWarning) "API Key: ${apiKeyValidation.message}" else null,
            if (endpointValidation.isWarning) "Endpoint: ${endpointValidation.message}" else null,
            if (modelNameValidation.isWarning) "模型: ${modelNameValidation.message}" else null
        ).filterNotNull()
    }
}