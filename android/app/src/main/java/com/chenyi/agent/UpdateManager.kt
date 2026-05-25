package com.chenyi.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 应用更新管理器
 * 
 * 功能：
 * - 检查服务器上的最新版本
 * - 下载 APK 文件
 * - 安装更新
 * 
 * 服务器信息：
 * - 官网: https://xintiandi.online
 * - 下载地址: https://oneapi.xintiandi.online/chenyi-agent/
 * - 版本信息: https://oneapi.xintiandi.online/chenyi-agent/version.json
 */
class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        
        // 服务器地址
        private const val BASE_URL = "https://oneapi.xintiandi.online/chenyi-agent"
        private const val VERSION_URL = "$BASE_URL/version.json"
        private const val APK_RELEASE_URL = "$BASE_URL/chenyi-agent-release.apk"
        private const val APK_DEBUG_URL = "$BASE_URL/chenyi-agent-debug.apk"
        
        // 本地存储路径
        private const val APK_FILE_NAME = "chenyi-agent-update.apk"
    }
    
    /**
     * 版本信息数据类
     */
    data class VersionInfo(
        val versionName: String,        // 例如 "1.0.16"
        val versionCode: Int,           // 例如 10016
        val releaseNotes: String,       // 更新日志
        val minAndroidVersion: Int,     // 最低 Android 版本
        val apkSize: Long,              // APK 文件大小（字节）
        val downloadUrl: String,        // 下载地址
        val forceUpdate: Boolean = false // 是否强制更新
    )
    
    /**
     * 更新状态密封类
     */
    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Checking : UpdateStatus()
        data class Available(val versionInfo: VersionInfo) : UpdateStatus()
        data class Downloading(val progress: Int) : UpdateStatus()
        data class Downloaded(val apkPath: String) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }
    
    /**
     * 获取当前应用版本名称
     */
    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get current version name", e)
            "unknown"
        }
    }
    
    /**
     * 获取当前应用版本号
     */
    fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get current version code", e)
            0
        }
    }
    
    /**
     * 检查服务器上的最新版本
     * 
     * @return VersionInfo 如果有新版本，否则返回 null
     */
    suspend fun checkForUpdate(): Result<VersionInfo?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from $VERSION_URL")
            
            val url = URL(VERSION_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            Log.d(TAG, "Version info response: $json")
            
            val jsonObject = JSONObject(json)
            
            val latestVersionName = jsonObject.getString("versionName")
            val latestVersionCode = jsonObject.getInt("versionCode")
            val releaseNotes = jsonObject.optString("releaseNotes", "性能优化和 Bug 修复")
            val minAndroidVersion = jsonObject.optInt("minAndroidVersion", 21)
            val apkSize = jsonObject.optLong("apkSize", 0)
            val downloadUrl = jsonObject.optString("downloadUrl", APK_RELEASE_URL)
            val forceUpdate = jsonObject.optBoolean("forceUpdate", false)
            
            val versionInfo = VersionInfo(
                versionName = latestVersionName,
                versionCode = latestVersionCode,
                releaseNotes = releaseNotes,
                minAndroidVersion = minAndroidVersion,
                apkSize = apkSize,
                downloadUrl = downloadUrl,
                forceUpdate = forceUpdate
            )
            
            val currentVersionCode = getCurrentVersionCode()
            Log.d(TAG, "Current version: $currentVersionCode, Latest version: ${versionInfo.versionCode}")
            
            // 比较版本号
            if (versionInfo.versionCode > currentVersionCode) {
                Log.i(TAG, "New version available: ${versionInfo.versionName}")
                return@withContext Result.success(versionInfo)
            } else {
                Log.i(TAG, "Already up to date")
                return@withContext Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 下载 APK 文件
     * 
     * @param versionInfo 版本信息
     * @param onProgress 进度回调 (0-100)
     * @return 下载完成的 APK 文件路径
     */
    suspend fun downloadApk(
        versionInfo: VersionInfo,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading APK from ${versionInfo.downloadUrl}")
            
            val url = URL(versionInfo.downloadUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            val fileLength = connection.contentLengthLong
            Log.d(TAG, "APK size: $fileLength bytes")
            
            // 创建临时文件
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            Log.d(TAG, "Downloading to ${apkFile.absolutePath}")
            
            var downloadedBytes = 0L
            var lastProgress = 0
            
            connection.getInputStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 计算进度
                        val progress = if (fileLength > 0) {
                            ((downloadedBytes * 100) / fileLength).toInt()
                        } else {
                            -1 // 未知大小
                        }
                        
                        // 避免频繁回调
                        if (progress != lastProgress && progress % 5 == 0) {
                            lastProgress = progress
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                            Log.d(TAG, "Download progress: $progress%")
                        }
                    }
                }
            }
            
            Log.i(TAG, "Download completed: ${apkFile.absolutePath}")
            return@withContext Result.success(apkFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download APK", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 安装 APK
     * 
     * @param apkPath APK 文件路径
     */
    fun installApk(apkPath: String): Result<Unit> {
        return try {
            val apkFile = File(apkPath)
            
            if (!apkFile.exists()) {
                return Result.failure(Exception("APK file not found: $apkPath"))
            }
            
            Log.d(TAG, "Installing APK from $apkPath")
            
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 需要使用 FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                // Android 7.0 以下直接使用 file:// URI
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            context.startActivity(intent)
            Log.i(TAG, "Install intent started")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            return Result.failure(e)
        }
    }
    
    /**
     * 清理已下载的 APK 文件
     */
    fun cleanupDownloadedApk() {
        try {
            val apkFile = File(context.cacheDir, APK_FILE_NAME)
            if (apkFile.exists()) {
                apkFile.delete()
                Log.d(TAG, "Deleted downloaded APK file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup APK file", e)
        }
    }
    
    /**
     * 检查并安装更新（完整流程）
     * 
     * @param onStatusUpdate 状态更新回调
     * @return 是否成功启动安装
     */
    suspend fun checkAndInstallUpdate(
        onStatusUpdate: (UpdateStatus) -> Unit = {}
    ): Result<Boolean> {
        return try {
            // 1. 检查更新
            onStatusUpdate(UpdateStatus.Checking)
            val checkResult = checkForUpdate()
            
            val versionInfo = checkResult.getOrNull()
                ?: return Result.success(false) // 已是最新版本
            
            // 2. 有新版本
            onStatusUpdate(UpdateStatus.Available(versionInfo))
            
            // 3. 下载 APK
            val downloadResult = downloadApk(versionInfo) { progress ->
                onStatusUpdate(UpdateStatus.Downloading(progress))
            }
            
            val apkPath = downloadResult.getOrNull()
                ?: return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            
            // 4. 下载完成
            onStatusUpdate(UpdateStatus.Downloaded(apkPath))
            
            // 5. 安装
            val installResult = installApk(apkPath)
            
            Result.success(installResult.isSuccess)
        } catch (e: Exception) {
            onStatusUpdate(UpdateStatus.Error(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }
}
