package com.chenyi.agent.repository

import com.chenyi.agent.data.ConfigManager
import com.chenyi.agent.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 配置仓库 - 封装配置数据访问
 * 
 * 职责：
 * - 提供统一的配置访问接口
 * - 处理数据持久化
 * - 隔离数据层和业务层
 */
class ConfigRepository(
    private val configManager: ConfigManager
) {
    /**
     * 获取 API Key
     */
    suspend fun getApiKey(): String = withContext(Dispatchers.IO) {
        configManager.getApiKey().first() ?: ""
    }
    
    /**
     * 保存 API Key
     */
    suspend fun saveApiKey(key: String) = withContext(Dispatchers.IO) {
        configManager.saveApiKey(key)
    }
    
    /**
     * 获取 API Endpoint
     */
    suspend fun getApiEndpoint(): String = withContext(Dispatchers.IO) {
        configManager.getApiEndpoint().first()
    }
    
    /**
     * 保存 API Endpoint
     */
    suspend fun saveApiEndpoint(endpoint: String) = withContext(Dispatchers.IO) {
        configManager.saveApiEndpoint(endpoint)
    }
    
    /**
     * 获取 Model Name
     */
    suspend fun getModelName(): String = withContext(Dispatchers.IO) {
        configManager.getModelName().first()
    }
    
    /**
     * 保存 Model Name
     */
    suspend fun saveModelName(model: String) = withContext(Dispatchers.IO) {
        configManager.saveModelName(model)
    }
    
    /**
     * 获取用户画像
     */
    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        configManager.getUserProfile().first()
    }
    
    /**
     * 保存用户画像
     */
    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        configManager.saveUserProfile(profile)
    }
}
