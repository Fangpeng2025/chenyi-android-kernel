package com.chenyi.agent

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * 热更新管理器
 * 支持动态下载并加载新的 Rust 内核库
 */
class HotUpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HotUpdate"
        private const val LIB_NAME = "libchenyi.so"
        private const val UPDATE_URL = "https://github.com/Fangpeng2025/chenyi-android-kernel/releases/latest/download/libchenyi.so"
        private const val VERSION_FILE = "kernel_version.txt"
    }
    
    private val executor = Executors.newSingleThreadExecutor()
    
    /**
     * 检查更新
     */
    fun checkUpdate(callback: (Boolean, String) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "检查更新...")
                
                // 获取当前版本
                val currentVersion = getCurrentVersion()
                Log.d(TAG, "当前版本: $currentVersion")
                
                // 获取远程版本
                val remoteVersion = getRemoteVersion()
                Log.d(TAG, "远程版本: $remoteVersion")
                
                val hasUpdate = currentVersion != remoteVersion
                val message = if (hasUpdate) {
                    "发现新版本: $remoteVersion"
                } else {
                    "已是最新版本"
                }
                
                callback(hasUpdate, message)
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败", e)
                callback(false, "检查更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 下载并应用更新
     */
    fun downloadAndUpdate(callback: (Boolean, String) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "开始下载更新...")
                
                // 下载新的 .so 文件
                val newLibFile = downloadLibrary()
                Log.d(TAG, "下载完成: ${newLibFile.absolutePath}")
                
                // 验证文件
                if (!verifyLibrary(newLibFile)) {
                    callback(false, "文件验证失败")
                    return@execute
                }
                
                // 备份旧文件
                val oldLibFile = File(context.filesDir, LIB_NAME)
                if (oldLibFile.exists()) {
                    val backupFile = File(context.filesDir, "$LIB_NAME.backup")
                    oldLibFile.renameTo(backupFile)
                    Log.d(TAG, "已备份旧文件")
                }
                
                // 应用新文件
                newLibFile.renameTo(oldLibFile)
                Log.d(TAG, "已应用新文件")
                
                // 更新版本号
                saveCurrentVersion(getRemoteVersion())
                
                callback(true, "更新成功，请重启应用")
            } catch (e: Exception) {
                Log.e(TAG, "更新失败", e)
                callback(false, "更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 下载库文件
     */
    private fun downloadLibrary(): File {
        val url = URL(UPDATE_URL)
        val tempFile = File(context.cacheDir, "$LIB_NAME.tmp")
        
        url.openStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
        
        return tempFile
    }
    
    /**
     * 验证库文件
     */
    private fun verifyLibrary(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) {
            return false
        }
        
        // TODO: 添加签名验证
        // 这里可以验证文件的 SHA256 或签名
        
        return true
    }
    
    /**
     * 获取当前版本
     */
    private fun getCurrentVersion(): String {
        val versionFile = File(context.filesDir, VERSION_FILE)
        return if (versionFile.exists()) {
            versionFile.readText().trim()
        } else {
            "unknown"
        }
    }
    
    /**
     * 保存当前版本
     */
    private fun saveCurrentVersion(version: String) {
        val versionFile = File(context.filesDir, VERSION_FILE)
        versionFile.writeText(version)
    }
    
    /**
     * 获取远程版本
     */
    private fun getRemoteVersion(): String {
        // TODO: 从服务器获取版本号
        // 这里可以访问 API 或读取版本文件
        return System.currentTimeMillis().toString()
    }
    
    /**
     * 加载更新的库
     */
    fun loadUpdatedLibrary(): Boolean {
        return try {
            val libFile = File(context.filesDir, LIB_NAME)
            if (libFile.exists()) {
                System.load(libFile.absolutePath)
                Log.d(TAG, "已加载更新的库: ${libFile.absolutePath}")
                true
            } else {
                Log.d(TAG, "没有更新的库，使用默认库")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载更新的库失败", e)
            false
        }
    }
    
    /**
     * 回滚到旧版本
     */
    fun rollback(): Boolean {
        return try {
            val backupFile = File(context.filesDir, "$LIB_NAME.backup")
            val currentFile = File(context.filesDir, LIB_NAME)
            
            if (backupFile.exists()) {
                currentFile.delete()
                backupFile.renameTo(currentFile)
                Log.d(TAG, "已回滚到旧版本")
                true
            } else {
                Log.d(TAG, "没有备份文件")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "回滚失败", e)
            false
        }
    }
}
