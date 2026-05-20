package com.chenyi.agent

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 截图管理器 - 使用 MediaProjection API
 */
class ScreenshotManager(private val context: Context) {

    companion object {
        private const val TAG = "ScreenshotManager"
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    var screenWidth = 0
    var screenHeight = 0
    private var screenDensity = 0
    private var initialized = false
    private var permissionGranted = false

    /**
     * 初始化
     */
    fun init() {
        try {
            mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val metrics = DisplayMetrics()
            windowManager?.defaultDisplay?.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi
            
            initialized = true
            Log.d(TAG, "屏幕尺寸: ${screenWidth}x${screenHeight}, 密度: $screenDensity")
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            initialized = false
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
    
    /**
     * 检查是否有截图权限
     */
    fun hasPermission(): Boolean = permissionGranted && mediaProjection != null

    /**
     * 请求截图权限
     */
    fun requestPermission(activity: Activity) {
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        if (intent != null) {
            activity.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
        }
    }

    /**
     * 处理权限结果
     */
    fun handlePermissionResult(resultCode: Int, data: Intent): Boolean {
        if (resultCode != Activity.RESULT_OK) {
            Log.e(TAG, "用户拒绝截图权限")
            permissionGranted = false
            return false
        }

        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection 创建失败")
            permissionGranted = false
            return false
        }

        permissionGranted = true
        Log.d(TAG, "MediaProjection 创建成功")
        return true
    }

    /**
     * 截取全屏
     */
    fun capture(): Bitmap? {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection 未初始化")
            return null
        }

        return try {
            Log.d(TAG, "开始截图: ${screenWidth}x${screenHeight}")
            
            // 创建 ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )
            Log.d(TAG, "ImageReader 创建成功")

            // 创建 VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            Log.d(TAG, "VirtualDisplay 创建成功")

            // 等待一帧（最多等待 1 秒）
            var image: Image? = null
            for (i in 0..10) {
                Thread.sleep(100)
                image = imageReader?.acquireLatestImage()
                if (image != null) {
                    Log.d(TAG, "获取图像成功 (尝试 $i 次)")
                    break
                }
            }

            if (image == null) {
                Log.e(TAG, "获取图像失败：等待超时")
                return null
            }

            // 转换为 Bitmap
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // 裁剪掉多余部分
            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            bitmap.recycle()

            Log.d(TAG, "截图成功: ${screenWidth}x${screenHeight}")
            finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "截图失败: ${e.message}", e)
            null
        } finally {
            release()
        }
    }

    /**
     * 截取指定区域
     */
    fun captureRegion(x: Int, y: Int, width: Int, height: Int): Bitmap? {
        val fullBitmap = capture() ?: return null
        
        return try {
            val regionBitmap = Bitmap.createBitmap(fullBitmap, x, y, width, height)
            fullBitmap.recycle()
            regionBitmap
        } catch (e: Exception) {
            Log.e(TAG, "截取区域失败: ${e.message}")
            null
        }
    }

    /**
     * 保存截图到文件
     */
    fun captureAndSave(): String? {
        val bitmap = capture() ?: return null
        
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "screenshot_$timestamp.png")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            bitmap.recycle()
            Log.d(TAG, "截图已保存: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存截图失败: ${e.message}")
            null
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }

    /**
     * 销毁
     */
    fun destroy() {
        release()
        mediaProjection?.stop()
        mediaProjection = null
    }

    /**
     * 是否已授权
     */
    fun isAuthorized(): Boolean = mediaProjection != null
}