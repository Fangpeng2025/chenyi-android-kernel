package com.chenyi.agent.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chenyi.agent.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 更新 ViewModel - 管理应用更新逻辑
 * 
 * 职责：
 * - 检查更新
 * - 下载 APK
 * - 安装更新
 */
class UpdateViewModel(
    private val context: Context,
    private val updateManager: UpdateManager
) : ViewModel() {
    
    // ========== 更新状态 ==========
    
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()
    
    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()
    
    private val _versionInfo = MutableStateFlow<UpdateManager.VersionInfo?>(null)
    val versionInfo: StateFlow<UpdateManager.VersionInfo?> = _versionInfo.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()
    
    private val _currentVersion = MutableStateFlow(updateManager.getCurrentVersionName())
    val currentVersion: StateFlow<String> = _currentVersion.asStateFlow()
    
    // ========== 更新操作 ==========
    
    /**
     * 检查更新
     */
    fun checkForUpdate() {
        if (_isCheckingUpdate.value) return
        
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _downloadProgress.value = 0
            
            try {
                val info = updateManager.checkForUpdate()
                
                if (info != null) {
                    _versionInfo.value = info
                    _updateAvailable.value = true
                    Toast.makeText(context, "发现新版本 v${info.versionName}", Toast.LENGTH_LONG).show()
                } else {
                    _updateAvailable.value = false
                    Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            
            _isCheckingUpdate.value = false
        }
    }
    
    /**
     * 下载并安装更新
     */
    fun downloadAndInstall() {
        val info = _versionInfo.value ?: return
        
        viewModelScope.launch {
            try {
                val apkPath = updateManager.downloadApk(info) { progress ->
                    _downloadProgress.value = progress
                }
                
                _downloadProgress.value = 100
                Toast.makeText(context, "下载完成，正在安装...", Toast.LENGTH_SHORT).show()
                
                // 安装 APK
                updateManager.installApk(apkPath)
            } catch (e: Exception) {
                Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                _downloadProgress.value = 0
            }
        }
    }
    
    /**
     * 检查或下载更新（智能判断）
     */
    fun checkOrDownload() {
        if (_updateAvailable.value && _versionInfo.value != null) {
            downloadAndInstall()
        } else {
            checkForUpdate()
        }
    }
}