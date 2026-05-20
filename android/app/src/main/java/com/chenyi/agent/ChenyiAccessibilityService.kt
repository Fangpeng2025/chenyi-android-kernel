package com.chenyi.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject

/**
 * 晨翼无障碍服务
 * 
 * 提供屏幕操作能力：点击、滑动、输入、截图、OCR 等
 */
class ChenyiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ChenyiService"
        private var instance: ChenyiAccessibilityService? = null

        fun getInstance(): ChenyiAccessibilityService? = instance
    }

    private var ocrEngine: OcrEngine? = null
    private var screenshotManager: ScreenshotManager? = null
    private val ocrInitialized = java.util.concurrent.atomic.AtomicBoolean(false)
    private val ocrInitFailed = java.util.concurrent.atomic.AtomicBoolean(false)
    private var ocrInitError: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        // 初始化截图管理器（同步）
        screenshotManager = ScreenshotManager(applicationContext)
        screenshotManager?.init()
        
        // 初始化 OCR（后台线程，避免阻塞）
        Thread {
            try {
                val engine = OcrEngine(applicationContext)
                engine.initSync()
                ocrEngine = engine
                ocrInitialized.set(true)
                Log.d(TAG, "OCR 初始化完成")
            } catch (e: Exception) {
                ocrInitFailed.set(true)
                ocrInitError = e.message
                Log.e(TAG, "OCR 初始化失败: ${e.message}", e)
            }
        }.start()
        
        Log.d(TAG, "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotManager?.destroy()
        instance = null
        Log.d(TAG, "无障碍服务已销毁")
    }

    // ============ 工具方法 ============

    /**
     * 执行工具
     */
    fun executeTool(tool: String, paramsJson: String): String {
        val params = if (paramsJson.isNotEmpty()) JSONObject(paramsJson) else JSONObject()

        val result = when (tool) {
            "screenshot" -> screenshot(params)
            "tap" -> tap(params)
            "long_press" -> longPress(params)
            "swipe" -> swipe(params)
            "type_text" -> typeText(params)
            "press_key" -> pressKey(params)
            "open_app" -> openApp(params)
            "close_app" -> closeApp()
            "current_app" -> currentApp()
            "list_apps" -> listApps()
            "ocr" -> ocr(params)
            "find_text" -> findText(params)
            "wait" -> wait(params)
            "get_screen_size" -> getScreenSize()
            "scroll" -> scroll(params)
            else -> error("未知工具: $tool")
        }

        return result.toJson()
    }

    // ============ 截图 ============

    private fun screenshot(params: JSONObject): Result {
        return try {
            // 检查截图管理器是否初始化
            if (screenshotManager == null || !screenshotManager!!.isInitialized()) {
                return Result.error("截图管理器未初始化")
            }
            
            // 检查截图权限
            if (!screenshotManager!!.hasPermission()) {
                return Result.error("未获取截图权限，请在设置中授权")
            }
            
            val region = params.optJSONObject("region")
            
            val bitmap = if (region != null) {
                val x = region.getInt("x")
                val y = region.getInt("y")
                val width = region.getInt("width")
                val height = region.getInt("height")
                screenshotManager?.captureRegion(x, y, width, height)
            } else {
                screenshotManager?.capture()
            }

            if (bitmap != null) {
                // 保存到文件
                val path = saveBitmap(bitmap)
                bitmap.recycle()
                
                Result.ok(mapOf(
                    "success" to true,
                    "path" to path,
                    "width" to (if (region != null) region.getInt("width") else screenshotManager?.screenWidth ?: 0),
                    "height" to (if (region != null) region.getInt("height") else screenshotManager?.screenHeight ?: 0)
                ))
            } else {
                Result.error("截图失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "截图失败", e)
            Result.error(e.message ?: "截图失败")
        }
    }

    private fun saveBitmap(bitmap: Bitmap): String {
        val file = java.io.File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    // ============ 点击 ============

    private fun tap(params: JSONObject): Result {
        return try {
            // 参数验证
            if (!params.has("x") || !params.has("y")) {
                return Result.error("缺少必需参数: x 或 y")
            }
            
            val x = params.getInt("x")
            val y = params.getInt("y")
            
            // 坐标范围检查
            val screenWidth = screenshotManager?.screenWidth ?: 1080
            val screenHeight = screenshotManager?.screenHeight ?: 1920
            if (x < 0 || x > screenWidth || y < 0 || y > screenHeight) {
                return Result.error("坐标超出屏幕范围: ($x, $y), 屏幕尺寸: ${screenWidth}x${screenHeight}")
            }

            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Log.d(TAG, "点击: ($x, $y) -> $success")
            
            if (success) {
                Result.ok(mapOf("x" to x, "y" to y, "success" to true))
            } else {
                Result.error("手势分发失败，可能无障碍服务未正确配置")
            }
        } catch (e: Exception) {
            Log.e(TAG, "点击失败: ${e.message}", e)
            Result.error("点击失败: ${e.message}\n堆栈: ${e.stackTrace.take(3).joinToString()}")
        }
    }

    // ============ 长按 ============

    private fun longPress(params: JSONObject): Result {
        val x = params.getInt("x")
        val y = params.getInt("y")
        val duration = params.optLong("duration", 500)

        return try {
            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Log.d(TAG, "长按: ($x, $y) ${duration}ms -> $success")
            Result.ok(mapOf("x" to x, "y" to y, "duration" to duration, "success" to success))
        } catch (e: Exception) {
            Log.e(TAG, "长按失败", e)
            Result.error(e.message ?: "长按失败")
        }
    }

    // ============ 滑动 ============

private fun swipe(params: JSONObject): Result {
        return try {
            // 参数验证
            val requiredParams = listOf("start_x", "start_y", "end_x", "end_y")
            for (param in requiredParams) {
                if (!params.has(param)) {
                    return Result.error("缺少必需参数: $param")
                }
            }
            
            val startX = params.getInt("start_x")
            val startY = params.getInt("start_y")
            val endX = params.getInt("end_x")
            val endY = params.getInt("end_y")
            val duration = params.optLong("duration", 300)
            
            // 坐标范围检查
            val screenWidth = screenshotManager?.screenWidth ?: 1080
            val screenHeight = screenshotManager?.screenHeight ?: 1920
            val coords = listOf(startX, endX, startY, endY)
            if (coords.any { it < 0 || it > maxOf(screenWidth, screenHeight) }) {
                return Result.error("坐标超出屏幕范围: 起点($startX,$startY) 终点($endX,$endY), 屏幕: ${screenWidth}x${screenHeight}")
            }

            val path = Path()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Log.d(TAG, "滑动: ($startX,$startY) -> ($endX,$endY) ${duration}ms -> $success")
            
            if (success) {
                Result.ok(mapOf(
                    "start_x" to startX,
                    "start_y" to startY,
                    "end_x" to endX,
                    "end_y" to endY,
                    "duration" to duration,
                    "success" to true
                ))
            } else {
                Result.error("手势分发失败，可能无障碍服务未正确配置")
            }
        } catch (e: Exception) {
            Log.e(TAG, "滑动失败: ${e.message}", e)
            Result.error("滑动失败: ${e.message}\n堆栈: ${e.stackTrace.take(3).joinToString()}")
        }
    }

    private fun typeText(params: JSONObject): Result {
        val text = params.getString("text")

        return try {
            val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focusedNode != null) {
                val arguments = Bundle()
                arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
                val success = focusedNode.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )
                focusedNode.recycle()
                Log.d(TAG, "输入文本: $text -> $success")
                Result.ok(mapOf("text" to text, "success" to success))
            } else {
                Result.error("未找到输入框")
            }
        } catch (e: Exception) {
            Log.e(TAG, "输入失败", e)
            Result.error(e.message ?: "输入失败")
        }
    }

    // ============ 按键 ============

    private fun pressKey(params: JSONObject): Result {
        return try {
            val keycode = when {
                params.has("keycode") -> params.getInt("keycode")
                params.has("key") -> when (params.getString("key")) {
                    "home" -> GLOBAL_ACTION_HOME
                    "back" -> GLOBAL_ACTION_BACK
                    "recent" -> GLOBAL_ACTION_RECENTS
                    "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                    "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
                    "power_dialog" -> {
                        // GLOBAL_ACTION_POWER_DIALOGS 需要 API 28+
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            GLOBAL_ACTION_POWER_DIALOGS
                        } else {
                            return Result.error("电源菜单需要 Android 9.0+")
                        }
                    }
                    "lock_screen" -> GLOBAL_ACTION_LOCK_SCREEN
                    "take_screenshot" -> GLOBAL_ACTION_TAKE_SCREENSHOT
                    else -> return Result.error("未知按键: ${params.getString("key")}")
                }
                else -> return Result.error("缺少按键参数")
            }

            val success = performGlobalAction(keycode)
            Log.d(TAG, "按键: $keycode -> $success")
            Result.ok(mapOf("keycode" to keycode, "success" to success))
        } catch (e: Exception) {
            Log.e(TAG, "按键失败", e)
            Result.error(e.message ?: "按键失败")
        }
    }

    // ============ 打开应用 ============

    private fun openApp(params: JSONObject): Result {
        val packageName = params.getString("package")

        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Log.d(TAG, "打开应用: $packageName")
                Result.ok(mapOf("package" to packageName))
            } else {
                Result.error("应用不存在: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开应用失败", e)
            Result.error(e.message ?: "打开应用失败")
        }
    }

    // ============ 关闭应用 ============

    private fun closeApp(): Result {
        return try {
            val success = performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d(TAG, "返回桌面 -> $success")
            Result.ok(mapOf("action" to "home", "success" to success))
        } catch (e: Exception) {
            Log.e(TAG, "关闭应用失败", e)
            Result.error(e.message ?: "关闭应用失败")
        }
    }

    // ============ 获取当前应用 ============

    private fun currentApp(): Result {
        return try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val packageName = rootNode.packageName?.toString() ?: "unknown"
                rootNode.recycle()
                Log.d(TAG, "当前应用: $packageName")
                Result.ok(mapOf("package" to packageName))
            } else {
                Result.error("无法获取当前应用")
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取当前应用失败", e)
            Result.error(e.message ?: "获取当前应用失败")
        }
    }

    // ============ 列出应用 ============

    private fun listApps(): Result {
        return try {
            val apps = packageManager.getInstalledApplications(0)
            val list = apps.map { app ->
                mapOf(
                    "package" to app.packageName,
                    "name" to app.loadLabel(packageManager).toString()
                )
            }
            Log.d(TAG, "列出应用: ${list.size} 个")
            Result.ok(mapOf("apps" to list, "count" to list.size))
        } catch (e: Exception) {
            Log.e(TAG, "列出应用失败", e)
            Result.error(e.message ?: "列出应用失败")
        }
    }

    // ============ OCR ============

    private fun ocr(params: JSONObject): Result {
        // 检查 OCR 是否初始化失败
        if (ocrInitFailed.get()) {
            return Result.error("OCR 初始化失败: ${ocrInitError ?: "未知错误"}")
        }
        
        // 检查 OCR 是否初始化完成
        if (!ocrInitialized.get()) {
            return Result.error("OCR 正在初始化，请稍后再试")
        }
        
        return try {
            // 先截图
            val bitmap = screenshotManager?.capture()
            if (bitmap == null) {
                return Result.error("截图失败")
            }

            // OCR 识别
            val result = ocrEngine?.recognize(bitmap)
            bitmap.recycle()

            if (result != null && result.success) {
                val words = result.words.map { word ->
                    mapOf(
                        "text" to word.text,
                        "confidence" to word.confidence,
                        "x" to word.x,
                        "y" to word.y,
                        "width" to word.width,
                        "height" to word.height
                    )
                }
                Log.d(TAG, "OCR 识别: ${result.fullText}")
                Result.ok(mapOf(
                    "words" to words,
                    "full_text" to result.fullText
                ))
            } else {
                Result.error(result?.error ?: "OCR 识别失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR 失败", e)
            Result.error(e.message ?: "OCR 失败")
        }
    }

    // ============ 查找文本 ============

    private fun findText(params: JSONObject): Result {
        val searchText = params.getString("text")

        return try {
            // 先 OCR
            val ocrResult = ocr(JSONObject())
            if (!ocrResult.success) {
                return ocrResult
            }

            // 查找文本
            val words = ocrResult.data?.get("words") as? List<Map<String, Any>> ?: emptyList()
            for (word in words) {
                val text = word["text"] as? String ?: ""
                if (text.contains(searchText, ignoreCase = true)) {
                    val x = (word["x"] as? Number)?.toInt() ?: 0
                    val y = (word["y"] as? Number)?.toInt() ?: 0
                    val width = (word["width"] as? Number)?.toInt() ?: 0
                    val height = (word["height"] as? Number)?.toInt() ?: 0
                    
                    Log.d(TAG, "找到文本: $text at ($x, $y)")
                    return Result.ok(mapOf(
                        "found" to true,
                        "text" to text,
                        "x" to x,
                        "y" to y,
                        "width" to width,
                        "height" to height,
                        "center_x" to x + width / 2,
                        "center_y" to y + height / 2
                    ))
                }
            }

            Result.ok(mapOf("found" to false, "text" to searchText))
        } catch (e: Exception) {
            Log.e(TAG, "查找文本失败", e)
            Result.error(e.message ?: "查找文本失败")
        }
    }

    // ============ 等待 ============

    private fun wait(params: JSONObject): Result {
        val duration = params.optLong("duration", 1000)

        return try {
            Thread.sleep(duration)
            Log.d(TAG, "等待: ${duration}ms")
            Result.ok(mapOf("duration" to duration, "success" to true))
        } catch (e: Exception) {
            Log.e(TAG, "等待失败", e)
            Result.error(e.message ?: "等待失败")
        }
    }

    // ============ 获取屏幕尺寸 ============

    private fun getScreenSize(): Result {
        return try {
            val width = screenshotManager?.screenWidth ?: 0
            val height = screenshotManager?.screenHeight ?: 0
            Log.d(TAG, "屏幕尺寸: ${width}x${height}")
            Result.ok(mapOf("width" to width, "height" to height))
        } catch (e: Exception) {
            Log.e(TAG, "获取屏幕尺寸失败", e)
            Result.error(e.message ?: "获取屏幕尺寸失败")
        }
    }

    // ============ 滚动 ============

    private fun scroll(params: JSONObject): Result {
        val direction = params.getString("direction")
        val distance = params.optInt("distance", 500)

        return try {
            val width = screenshotManager?.screenWidth ?: 1080
            val height = screenshotManager?.screenHeight ?: 1920
            val centerX = width / 2
            val centerY = height / 2

            val (startX, startY, endX, endY) = when (direction) {
                "up" -> listOf(centerX, centerY + distance / 2, centerX, centerY - distance / 2)
                "down" -> listOf(centerX, centerY - distance / 2, centerX, centerY + distance / 2)
                "left" -> listOf(centerX + distance / 2, centerY, centerX - distance / 2, centerY)
                "right" -> listOf(centerX - distance / 2, centerY, centerX + distance / 2, centerY)
                else -> return Result.error("无效方向: $direction")
            }

            val path = Path()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Log.d(TAG, "滚动: $direction ${distance}px -> $success")
            Result.ok(mapOf(
                "direction" to direction,
                "distance" to distance,
                "success" to success
            ))
        } catch (e: Exception) {
            Log.e(TAG, "滚动失败", e)
            Result.error(e.message ?: "滚动失败")
        }
    }

    // ============ 辅助方法 ============

    private fun error(msg: String): Result = Result.error(msg)
}