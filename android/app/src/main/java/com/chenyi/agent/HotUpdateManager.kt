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
        private const val VERSION_FILE = "kernel_version.txt"
        
        // 更新服务器配置（优先级：阿里云 > Gitee > GitHub）
        private const val ALIYUN_BASE_URL = "https://oneapi.xintiandi.online/chenyi-agent"
        private const val GITEE_API = "https://gitee.com/api/v5/repos/Fangpeng2025/chenyi-android-kernel/releases/latest"
        private const val GITHUB_API = "https://api.github.com/repos/Fangpeng2025/chenyi-android-kernel/releases/latest"
        
        // 热更新库下载地址
        private val UPDATE_URLS = listOf(
            "$ALIYUN_BASE_URL/libchenyi.so",  // 阿里云（国内最快）
            "https://gitee.com/Fangpeng2025/chenyi-android-kernel/releases/latest/download/libchenyi.so",  // Gitee
            "https://github.com/Fangpeng2025/chenyi-android-kernel/releases/latest/download/libchenyi.so"  // GitHub
        )
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
        val tempFile = File(context.cacheDir, "$LIB_NAME.tmp")
        
        // 尝试从多个 URL 下载
        for (urlStr in UPDATE_URLS) {
            try {
                Log.d(TAG, "尝试下载: $urlStr")
                val url = URL(urlStr)
                url.openStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                
                if (tempFile.exists() && tempFile.length() > 0) {
                    Log.d(TAG, "下载成功: ${tempFile.length()} bytes")
                    return tempFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "下载失败: $urlStr - ${e.message}")
            }
        }
        
        throw Exception("所有下载源均失败")
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
     * 获取远程版本（带超时）- 优先使用阿里云国内服务器
     */
    private fun getRemoteVersionWithTimeout(): String {
        // 优先尝试阿里云（国内最快）
        try {
            val aliyunUrl = URL("$ALIYUN_BASE_URL/version.txt")
            val connection = aliyunUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000 // 3秒连接超时
            connection.readTimeout = 3000 // 3秒读取超时
            val version = connection.inputStream.use { it.bufferedReader().readText().trim() }
            Log.d(TAG, "从阿里云获取版本: $version")
            return version
        } catch (e: Exception) {
            Log.w(TAG, "阿里云获取失败: ${e.message}，尝试 Gitee")
        }
        
        // 备用：Gitee（国内镜像）
        try {
            val giteeUrl = URL("https://gitee.com/Fangpeng2025/chenyi-android-kernel/raw/master/version.txt")
            val connection = giteeUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000 // 5秒连接超时
            connection.readTimeout = 5000 // 5秒读取超时
            val version = connection.inputStream.use { it.bufferedReader().readText().trim() }
            Log.d(TAG, "从 Gitee 获取版本: $version")
            return version
        } catch (e: Exception) {
            Log.w(TAG, "Gitee 获取失败: ${e.message}，尝试 GitHub")
        }
        
        // 最后备用：GitHub
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
     * 检查 APK 更新（优先使用阿里云国内服务器）
     */
    fun checkApkUpdate(callback: (Boolean, String, String?, String?) -> Unit) {
        executor.execute {
            try {
                Log.d(TAG, "检查 APK 更新...")
                
                // 获取当前 APK 版本
                val currentVersion = getAppVersion()
                Log.d(TAG, "当前 APK 版本: $currentVersion")
                
                // 优先从阿里云获取版本信息
                var tagName: String? = null
                var releaseNotes: String? = null
                var apkUrl: String? = null
                
                try {
                    val aliyunUrl = URL("$ALIYUN_BASE_URL/version.json")
                    val connection = aliyunUrl.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    val response = connection.inputStream.use { it.bufferedReader().readText() }
                    Log.d(TAG, "阿里云响应: $response")
                    
                    tagName = extractJsonValue(response, "versionName")
                    releaseNotes = extractJsonValue(response, "updateLog")
                    apkUrl = extractJsonValue(response, "apkUrl")
                    Log.d(TAG, "从阿里云获取版本: $tagName, APK: $apkUrl")
                } catch (e: Exception) {
                    Log.w(TAG, "阿里云获取失败: ${e.message}，尝试 Gitee")
                }
                
                // 备用：Gitee API
                if (tagName.isNullOrEmpty()) {
                    try {
                        val giteeConnection = URL(GITEE_API).openConnection() as HttpURLConnection
                        giteeConnection.connectTimeout = 5000
                        giteeConnection.readTimeout = 5000
                        val response = giteeConnection.inputStream.use { it.bufferedReader().readText() }
                        Log.d(TAG, "Gitee API 响应: $response")
                        
                        tagName = extractJsonValue(response, "tag_name")
                        releaseNotes = extractJsonValue(response, "body")
                        apkUrl = extractApkUrl(response)
                        Log.d(TAG, "从 Gitee 获取版本: $tagName")
                    } catch (e: Exception) {
                        Log.w(TAG, "Gitee API 失败: ${e.message}，尝试 GitHub")
                    }
                }
                
                // 最后备用：GitHub API
                if (tagName.isNullOrEmpty()) {
                    val connection = URL(GITHUB_API).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    
                    val response = connection.inputStream.use { it.bufferedReader().readText() }
                    Log.d(TAG, "GitHub API 响应: $response")
                    
                    tagName = extractJsonValue(response, "tag_name")
                    releaseNotes = extractJsonValue(response, "body")
                    apkUrl = extractApkUrl(response)
                    Log.d(TAG, "从 GitHub 获取版本: $tagName")
                }
                
                val hasUpdate = currentVersion != tagName && !tagName.isNullOrEmpty()
                
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
                connection.connectTimeout = 30000  // 连接超时 30 秒
                connection.readTimeout = 300000    // 读取超时 5 分钟（APK 较大）
                
                val fileSize = connection.contentLength
                Log.d(TAG, "APK 大小: $fileSize bytes (${fileSize / 1024 / 1024} MB)")
                
                var downloaded = 0
                val buffer = ByteArray(8192)
                var lastProgress = 0
                
                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            if (fileSize > 0) {
                                val progress = (downloaded * 100 / fileSize)
                                // 每下载 1% 才更新进度，避免频繁回调
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    Log.d(TAG, "下载进度: $progress% ($downloaded / $fileSize)")
                                    Handler(Looper.getMainLooper()).post {
                                        progressCallback(progress)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Log.d(TAG, "APK 下载完成: ${apkFile.absolutePath}, 大小: ${apkFile.length()} bytes")
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
        // 直接查找所有 browser_download_url，选择第一个 .apk 文件
        // 优先选择 app-release.apk，如果没有则选择 app-debug.apk
        val urls = mutableListOf<String>()
        
        // 查找所有 browser_download_url
        val urlPattern = "\"browser_download_url\"\\s*:\\s*\"([^\"]+)\""
        Regex(urlPattern).findAll(json).forEach { match ->
            val url = match.groupValues[1]
            if (url.endsWith(".apk")) {
                urls.add(url)
            }
        }
        
        Log.d(TAG, "找到 APK URL: $urls")
        
        // 优先选择 app-release.apk
        return urls.firstOrNull { it.contains("app-release.apk") }
            ?: urls.firstOrNull { it.contains("app-debug.apk") }
            ?: urls.firstOrNull()
    }
}
