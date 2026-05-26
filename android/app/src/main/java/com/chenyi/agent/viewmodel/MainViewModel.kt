package com.chenyi.agent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenyi.agent.data.ConfigManager
import com.chenyi.agent.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 主 ViewModel - 管理 UI 状态和业务逻辑
 * 
 * 职责：
 * - 配置管理（API Key, Endpoint, Model）
 * - 用户画像管理
 * - 会话管理
 * - 更新检查
 */
class MainViewModel(
    private val configManager: ConfigManager
) : ViewModel() {
    
    // ========== 配置状态 ==========
    
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()
    
    private val _apiEndpoint = MutableStateFlow("https://api.openai.com/v1")
    val apiEndpoint: StateFlow<String> = _apiEndpoint.asStateFlow()
    
    private val _modelName = MutableStateFlow("gpt-4")
    val modelName: StateFlow<String> = _modelName.asStateFlow()
    
    // ========== 用户画像状态 ==========
    
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    
    // ========== UI 状态 ==========
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // ========== 对话框状态 ==========
    
    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()
    
    private val _showApiEndpointDialog = MutableStateFlow(false)
    val showApiEndpointDialog: StateFlow<Boolean> = _showApiEndpointDialog.asStateFlow()
    
    private val _showModelNameDialog = MutableStateFlow(false)
    val showModelNameDialog: StateFlow<Boolean> = _showModelNameDialog.asStateFlow()
    
    private val _showUserProfileDialog = MutableStateFlow(false)
    val showUserProfileDialog: StateFlow<Boolean> = _showUserProfileDialog.asStateFlow()
    
    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()
    
    // ========== 初始化 ==========
    
    init {
        loadConfig()
    }
    
    /**
     * 加载配置
     */
    private fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载配置
                _apiKey.value = configManager.getApiKey()
                _apiEndpoint.value = configManager.getApiEndpoint()
                _modelName.value = configManager.getModelName()
                _userProfile.value = configManager.getUserProfile()
            } catch (e: Exception) {
                _errorMessage.value = "加载配置失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ========== 配置操作 ==========
    
    /**
     * 保存 API Key
     */
    fun saveApiKey(key: String) {
        viewModelScope.launch {
            try {
                configManager.saveApiKey(key)
                _apiKey.value = key
                _showApiKeyDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            }
        }
    }
    
    /**
     * 保存 API Endpoint
     */
    fun saveApiEndpoint(endpoint: String) {
        viewModelScope.launch {
            try {
                configManager.saveApiEndpoint(endpoint)
                _apiEndpoint.value = endpoint
                _showApiEndpointDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            }
        }
    }
    
    /**
     * 保存 Model Name
     */
    fun saveModelName(model: String) {
        viewModelScope.launch {
            try {
                configManager.saveModelName(model)
                _modelName.value = model
                _showModelNameDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            }
        }
    }
    
    /**
     * 保存用户画像
     */
    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                configManager.saveUserProfile(profile)
                _userProfile.value = profile
                _showUserProfileDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            }
        }
    }
    
    // ========== 对话框控制 ==========
    
    fun showApiKeyDialog() {
        _showApiKeyDialog.value = true
    }
    
    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
    }
    
    fun showApiEndpointDialog() {
        _showApiEndpointDialog.value = true
    }
    
    fun hideApiEndpointDialog() {
        _showApiEndpointDialog.value = false
    }
    
    fun showModelNameDialog() {
        _showModelNameDialog.value = true
    }
    
    fun hideModelNameDialog() {
        _showModelNameDialog.value = false
    }
    
    fun showUserProfileDialog() {
        _showUserProfileDialog.value = true
    }
    
    fun hideUserProfileDialog() {
        _showUserProfileDialog.value = false
    }
    
    fun showAboutDialog() {
        _showAboutDialog.value = true
    }
    
    fun hideAboutDialog() {
        _showAboutDialog.value = false
    }
    
    // ========== 错误处理 ==========
    
    fun clearError() {
        _errorMessage.value = null
    }
}
