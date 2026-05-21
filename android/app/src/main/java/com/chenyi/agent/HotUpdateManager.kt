package com.chenyi.agent

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
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
        private const val GITHUB_API = "https://api.github.com/repos/Fangpeng2025/chenyi-android-kernel/releases/latest"
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
                
                // 获取远程版本（设置超时）
                val remoteVersion = getRemoteVersionWithTimeout()
                Log.d(TAG, "远程版本: $remoteVersion")
                
                val hasUpdate = currentVersion != remoteVersion
                val message = if (hasUpdate) {
                    "发现新版本: $remoteVersion"
                } else {
                    "已是最新版本"
                }
                
                // 在主线程回调
                Handler(Looper.getMainLooper()).post {
                    callback(hasUpdate, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查更新失败", e)
                Handler(Looper.getMainLooper()).post {
                    callback(false, "检查更新失败: ${e.message}")
                }
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
     * 获取远程版本（带超时）
     */
    private fun getRemoteVersionWithTimeout(): String {
        return try {
            val url = URL("https://github.com/Fangpeng2025/chenyi-android-kernel/releases/latest/download/version.txt")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10秒连接超时
            connection.readTimeout = 10000 // 10秒读取超时
            connection.inputStream.use { it.bufferedReader().readText().trim() }
        } catch (e: Exception) {
            Log.e(TAG, "获取远程版本失败: ${e.message}")
            // 返回当前版本，表示无更新
            getCurrentVersion()
        }
    }
    
    /**
     * 获取远程版本
     */
    private fun getRemoteVersion(): String {
        return getRemoteVersionWithTimeout()
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
    
    // ==================== APK 自动更新功能 ====================
    
    /**
     * 检查 APK 更新（从 GitHub Release）
     */
    fun checkApkUpdate(callback: (Boolean, String, String?, String?) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "检查 APK 更新...")
                
                // 获取当前 APK 版本
                val currentVersion = getAppVersion()
                Log.d(TAG, "当前 APK 版本: $currentVersion")
                
                // 从 GitHub API 获取最新 Release 信息
                val connection = URL(GITHUB_API).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                
                val response = connection.inputStream.use { it.bufferedReader().readText() }
                Log.d(TAG, "GitHub API 响应: $response")
                
                // 解析 JSON（简单解析，不使用 Gson）
                val tagName = extractJsonValue(response, "tag_name")
                val releaseName = extractJsonValue(response, "name")
                val releaseNotes = extractJsonValue(response, "body")
                
                Log.d(TAG, "远程版本: $tagName")
                
                val hasUpdate = currentVersion != tagName && tagName.isNotEmpty()
                
                // 获取 APK 下载链接
                val apkUrl = if (hasUpdate) {
                    // 从 assets 中查找 APK
                    extractApkUrl(response)
                } else null
                
                Handler(Looper.getMainLooper()).post {
                    callback(hasUpdate, tagName ?: "未知版本", apkUrl, releaseNotes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查 APK 更新失败", e)
                Handler(Looper.getMainLooper()).post {
                    callback(false, "检查失败: ${e.message}", null, null)
                }
            }
        }
    }
    
    /**
     * 下载 APK 文件
     */
    fun downloadApk(apkUrl: String, progressCallback: (Int) -> Unit, completeCallback: (Boolean, File?) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "开始下载 APK: $apkUrl")
                
                val apkFile = File(context.cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()
                
                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 60000 // APK 较大，设置更长超时
                
                val fileSize = connection.contentLength
                Log.d(TAG, "APK 大小: $fileSize bytes")
                
                var downloaded = 0
                val buffer = ByteArray(8192)
                
                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            if (fileSize > 0) {
                                val progress = (downloaded * 100 / fileSize)
                                Handler(Looper.getMainLooper()).post {
                                    progressCallback(progress)
                                }
                            }
                        }
                    }
                }
                
                Log.d(TAG, "APK 下载完成: ${apkFile.absolutePath}")
                Handler(Looper.getMainLooper()).post {
                    completeCallback(true, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "APK 下载失败", e)
                Handler(Looper.getMainLooper()).post {
                    completeCallback(false, null)
                }
            }
        }
    }
    
    /**
     * 获取应用版本号
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "获取应用版本失败", e)
            "unknown"
        }
    }
    
    /**
     * 从 JSON 字符串中提取值（简单解析）
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\""
        val regex = Regex(pattern)
        val match = regex.find(json)
        return match?.groupValues?.getOrNull(1)
    }
    
    /**
     * 从 Release JSON 中提取 APK 下载链接
     */
    private fun extractApkUrl(json: String): String? {
        // 查找 assets 数组中的 APK 文件
        val assetsPattern = "\"assets\"\\s*:\\s*\\[([^\\]]+)\\]"
        val assetsMatch = Regex(assetsPattern).find(json)
        if (assetsMatch == null) return null
        
        val assetsContent = assetsMatch.groupValues[1]
        
        // 查找 .apk 文件的 browser_download_url
        val urlPattern = "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\""
        val urlMatch = Regex(urlPattern).find(assetsContent)
        return urlMatch?.groupValues?.getOrNull(1)
    }
}
