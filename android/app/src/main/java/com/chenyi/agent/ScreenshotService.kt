package com.chenyi.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 截图前台服务 - Android 14+ 要求 MediaProjection 必须在前台服务中使用
 */
class ScreenshotService : Service() {

    companion object {
        private const val TAG = "ScreenshotService"
        private const val CHANNEL_ID = "screenshot_service"
        private const val NOTIFICATION_ID = 1001

        var instance: ScreenshotService? = null
            private set
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

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        Log.d(TAG, "截图服务创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "截图服务启动")

        // 启动前台服务
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 需要指定服务类型
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 初始化
        if (!initialized) {
            init()
            initialized = true
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        destroy()
        instance = null
        Log.d(TAG, "截图服务销毁")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "截图服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "晨翼Agent 截图功能需要的前台服务"
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("晨翼Agent")
            .setContentText("截图服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun init() {
        try {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val metrics = DisplayMetrics()
            windowManager?.defaultDisplay?.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi

            Log.d(TAG, "屏幕尺寸: ${screenWidth}x${screenHeight}, 密度: $screenDensity")
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
        }
    }

    /**
     * 设置 MediaProjection
     */
    fun setMediaProjection(resultCode: Int, data: Intent): Boolean {
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection 创建失败")
            return false
        }

        Log.d(TAG, "MediaProjection 创建成功")
        return true
    }

    /**
     * 检查是否有 MediaProjection
     */
    fun hasProjection(): Boolean = mediaProjection != null

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
                Handler(Looper.getMainLooper())
            )
            Log.d(TAG, "VirtualDisplay 创建成功")

            // 等待图像
            Thread.sleep(100)

            // 获取最新图像
            val image: Image? = imageReader?.acquireLatestImage()
            if (image == null) {
                Log.e(TAG, "获取图像失败")
                return null
            }

            // 转换为 Bitmap
            val bitmap = imageToBitmap(image)
            image.close()

            Log.d(TAG, "截图成功: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "截图失败", e)
            null
        } finally {
            // 清理资源
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // 裁剪到正确尺寸
        val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        bitmap.recycle()

        return finalBitmap
    }

    /**
     * 保存截图到文件
     */
    fun captureToFile(): String? {
        val bitmap = capture() ?: return null

        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_$timeStamp.png"
            val file = File(filesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            Log.d(TAG, "截图保存: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存截图失败", e)
            bitmap.recycle()
            null
        }
    }

    /**
     * 清理资源
     */
    fun destroy() {
        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        Log.d(TAG, "资源已清理")
    }
}
