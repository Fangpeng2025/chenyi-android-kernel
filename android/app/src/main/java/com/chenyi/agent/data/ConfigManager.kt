package com.chenyi.agent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore 扩展
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chenyi_agent_config")

/**
 * 晨翼Agent 配置管理器
 * 
 * 功能：
 * - API 配置持久化
 * - 用户画像持久化
 * - 压缩配置持久化
 * 
 * 使用 DataStore (异步、类型安全、支持协程)
 */
class ConfigManager(private val context: Context) {

    // ==================== Keys ====================
    
    private object Keys {
        // API 配置
        val API_KEY = stringPreferencesKey("api_key")
        val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        val MODEL_NAME = stringPreferencesKey("model_name")
        
        // 用户画像
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val USER_TIMEZONE = stringPreferencesKey("user_timezone")
        val USER_PREFERENCES = stringPreferencesKey("user_preferences")
        val USER_AVATAR_PATH = stringPreferencesKey("user_avatar_path")
        val USER_LANGUAGE = stringPreferencesKey("user_language")
        val USER_RESPONSE_STYLE = stringPreferencesKey("user_response_style")
        val USER_EXPERTISE = stringPreferencesKey("user_expertise")
        
        // 压缩配置
        val COMPRESSION_ENABLED = booleanPreferencesKey("compression_enabled")
        val COMPRESSION_THRESHOLD = intPreferencesKey("compression_threshold")
        val COMPRESSION_RATIO = intPreferencesKey("compression_ratio")
    }

    // ==================== API 配置 ====================
    
    /**
     * 获取 API Key
     */
    fun getApiKey(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.API_KEY]
    }
    
    /**
     * 保存 API Key
     */
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.API_KEY] = apiKey
        }
    }
    
    /**
     * 清除 API Key
     */
    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.API_KEY)
        }
    }
    
    /**
     * 获取 API Endpoint
     */
    fun getApiEndpoint(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.API_ENDPOINT] ?: "api.openai.com"
    }
    
    /**
     * 保存 API Endpoint
     */
    suspend fun saveApiEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.API_ENDPOINT] = endpoint
        }
    }
    
    /**
     * 获取模型名称
     */
    fun getModelName(): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.MODEL_NAME] ?: "glm-5"
    }
    
    /**
     * 保存模型名称
     */
    suspend fun saveModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MODEL_NAME] = modelName
        }
    }

    // ==================== 用户画像 ====================
    
    /**
     * 获取用户画像
     */
    fun getUserProfile(): Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            name = preferences[Keys.USER_NAME] ?: "",
            role = preferences[Keys.USER_ROLE] ?: "",
            timezone = preferences[Keys.USER_TIMEZONE] ?: "UTC+8",
            preferences = preferences[Keys.USER_PREFERENCES] ?: "",
            avatarPath = preferences[Keys.USER_AVATAR_PATH],
            language = preferences[Keys.USER_LANGUAGE] ?: "zh-CN",
            responseStyle = preferences[Keys.USER_RESPONSE_STYLE] ?: "concise",
            expertise = preferences[Keys.USER_EXPERTISE] ?: ""
        )
    }
    
    /**
     * 保存用户画像
     */
    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_NAME] = profile.name
            preferences[Keys.USER_ROLE] = profile.role
            preferences[Keys.USER_TIMEZONE] = profile.timezone
            preferences[Keys.USER_PREFERENCES] = profile.preferences
            profile.avatarPath?.let { preferences[Keys.USER_AVATAR_PATH] = it }
            preferences[Keys.USER_LANGUAGE] = profile.language
            preferences[Keys.USER_RESPONSE_STYLE] = profile.responseStyle
            preferences[Keys.USER_EXPERTISE] = profile.expertise
        }
    }
    
    /**
     * 保存头像路径
     */
    suspend fun saveAvatarPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_AVATAR_PATH] = path
        }
    }
    
    /**
     * 清除用户画像
     */
    suspend fun clearUserProfile() {
        context.dataStore.edit { preferences ->
            preferences.remove(Keys.USER_NAME)
            preferences.remove(Keys.USER_ROLE)
            preferences.remove(Keys.USER_TIMEZONE)
            preferences.remove(Keys.USER_PREFERENCES)
            preferences.remove(Keys.USER_AVATAR_PATH)
            preferences.remove(Keys.USER_LANGUAGE)
            preferences.remove(Keys.USER_RESPONSE_STYLE)
            preferences.remove(Keys.USER_EXPERTISE)
        }
    }

    // ==================== 压缩配置 ====================
    
    /**
     * 获取压缩是否启用
     */
    fun getCompressionEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.COMPRESSION_ENABLED] ?: true
    }
    
    /**
     * 保存压缩是否启用
     */
    suspend fun saveCompressionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.COMPRESSION_ENABLED] = enabled
        }
    }
    
    /**
     * 获取压缩阈值
     */
    fun getCompressionThreshold(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.COMPRESSION_THRESHOLD] ?: 4000
    }
    
    /**
     * 保存压缩阈值
     */
    suspend fun saveCompressionThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.COMPRESSION_THRESHOLD] = threshold
        }
    }
    
    /**
     * 获取压缩比例
     */
    fun getCompressionRatio(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.COMPRESSION_RATIO] ?: 50
    }
    
    /**
     * 保存压缩比例
     */
    suspend fun saveCompressionRatio(ratio: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.COMPRESSION_RATIO] = ratio
        }
    }

    // ==================== 批量操作 ====================
    
    /**
     * 清除所有配置
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * 用户画像数据类（增强版）
 */
data class UserProfile(
    val name: String = "",
    val role: String = "",
    val timezone: String = "UTC+8",
    val preferences: String = "",
    val avatarPath: String? = null,
    val language: String = "zh-CN",
    val responseStyle: String = "concise",  // concise, detailed, balanced
    val expertise: String = ""  // 专业领域
)

/**
 * 响应风格选项
 */
enum class ResponseStyle(val displayName: String, val value: String) {
    CONCISE("简洁", "concise"),
    BALANCED("平衡", "balanced"),
    DETAILED("详细", "detailed")
}

/**
 * 语言选项
 */
enum class LanguageOption(val displayName: String, val value: String) {
    CHINESE("简体中文", "zh-CN"),
    ENGLISH("English", "en-US"),
    JAPANESE("日本語", "ja-JP")
}
