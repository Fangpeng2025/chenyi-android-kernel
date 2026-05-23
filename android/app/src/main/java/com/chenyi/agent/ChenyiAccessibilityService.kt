package com.chenyi.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 晨翼Agent 无障碍服务 - 整合 Peekaboo 能力
 */
class ChenyiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ChenyiA11y"
        private var instance: ChenyiAccessibilityService? = null
        
        fun getInstance(): ChenyiAccessibilityService? = instance
        fun isRunning(): Boolean = instance != null
    }

    private var ocrEngine: OcrEngine? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        // 初始化 OCR
        Thread {
            try {
                ocrEngine = OcrEngine(applicationContext)
                ocrEngine?.initSync()
                Log.d(TAG, "OCR 初始化完成")
            } catch (e: Exception) {
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
        instance = null
        Log.d(TAG, "无障碍服务销毁")
    }

    // ============ 工具执行入口 ============
    
    fun executeTool(name: String, params: JSONObject): Result {
        return try {
            when (name) {
                "screenshot" -> screenshot(params)
                "tap" -> tap(params)
                "long_press" -> longPress(params)
                "swipe" -> swipe(params)
                "type_text" -> typeText(params)
                "press_key" -> pressKey(params)
                "open_app" -> openApp(params)
                "close_app" -> closeApp(params)
                "current_app" -> currentApp(params)
                "ocr" -> ocr(params)
                "find_text" -> findText(params)
                "click_text" -> clickText(params)
                "see" -> see(params)
                "get_ui_tree" -> getUITree(params)
                "find_element" -> findElement(params)
                "click_element" -> clickElement(params)
                "scroll" -> scroll(params)
                "wait" -> wait(params)
                "get_screen_size" -> getScreenSize(params)
                else -> Result.error("未知工具: $name")
            }
        } catch (e: Exception) {
            Log.e(TAG, "工具执行失败: $name", e)
            Result.error(e.message ?: "执行失败")
        }
    }

    // ============ See 功能（核心） ============
    
    private fun see(params: JSONObject): Result {
        try {
            val compact = params.optBoolean("compact", false)
            val withOcr = params.optBoolean("ocr", true)
            
            // 1. 截图
            val screenshotResult = screenshot(JSONObject())
            if (!screenshotResult.success) {
                return Result.error("截图失败: ${screenshotResult.error}")
            }
            
            val result = mutableMapOf<String, Any?>()
            
            // 截图信息
            screenshotResult.data?.let { result["screenshot"] = it }
            
            // 2. UI 层级
            val uiTree = getUITree(JSONObject().apply {
                put("maxDepth", if (compact) 5 else 15)
            })
            if (uiTree.success && uiTree.data != null) {
                result["elements"] = uiTree.data!!["tree"]
            }
            
            // 3. OCR
            if (withOcr) {
                val ocrResult = ocr(JSONObject())
                if (ocrResult.success && ocrResult.data != null) {
                    result["ocr"] = ocrResult.data!!
                }
            }
            
            return Result.ok(result)
        } catch (e: Exception) {
            return Result.error("see 失败: ${e.message}")
        }
    }

    // ============ UI 层级 ============
    
    private fun getUITree(params: JSONObject): Result {
        try {
            val maxDepth = params.optInt("maxDepth", 15)
            val root = rootInActiveWindow ?: return Result.error("无活动窗口")
            
            val tree = buildUITree(root, 0, maxDepth)
            root.recycle()
            
            return Result.ok(mapOf("tree" to tree))
        } catch (e: Exception) {
            return Result.error("获取 UI 树失败: ${e.message}")
        }
    }
    
    private fun buildUITree(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        
        // 基本信息
        map["className"] = node.className?.toString() ?: ""
        map["text"] = node.text?.toString() ?: ""
        map["contentDescription"] = node.contentDescription?.toString() ?: ""
        map["viewIdResourceName"] = node.viewIdResourceName ?: ""
        
        // 状态
        map["isClickable"] = node.isClickable
        map["isScrollable"] = node.isScrollable
        map["isEditable"] = node.isEditable
        map["isEnabled"] = node.isEnabled
        map["isFocused"] = node.isFocused
        map["isSelected"] = node.isSelected
        map["isChecked"] = node.isChecked
        
        // 边界
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        map["bounds"] = mapOf(
            "left" to bounds.left,
            "top" to bounds.top,
            "right" to bounds.right,
            "bottom" to bounds.bottom
        )
        
        // 递归子节点
        if (depth < maxDepth && node.childCount > 0) {
            val children = mutableListOf<Map<String, Any?>>()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    children.add(buildUITree(child, depth + 1, maxDepth))
                    child.recycle()
                }
            }
            map["children"] = children
        }
        
        return map
    }
    
    private fun findElement(params: JSONObject): Result {
        try {
            val root = rootInActiveWindow ?: return Result.error("无活动窗口")
            
            val text = params.optString("text", null)
            val id = params.optString("id", null)
            val contentDesc = params.optString("contentDescription", null)
            val maxResults = params.optInt("maxResults", 10)
            
            val results = mutableListOf<Map<String, Any?>>()
            findElementsRecursive(root, text, id, contentDesc, results, maxResults)
            root.recycle()
            
            return Result.ok(mapOf(
                "elements" to results,
                "count" to results.size
            ))
        } catch (e: Exception) {
            return Result.error("查找元素失败: ${e.message}")
        }
    }
    
    private fun findElementsRecursive(
        node: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        contentDesc: String?,
        results: MutableList<Map<String, Any?>>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        
        var match = true
        
        if (text != null && node.text?.toString()?.contains(text, ignoreCase = true) != true) {
            match = false
        }
        if (id != null && node.viewIdResourceName?.contains(id, ignoreCase = true) != true) {
            match = false
        }
        if (contentDesc != null && node.contentDescription?.toString()?.contains(contentDesc, ignoreCase = true) != true) {
            match = false
        }
        
        if (match) {
            results.add(nodeToMap(node))
        }
        
        // 递归子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findElementsRecursive(child, text, id, contentDesc, results, maxResults)
                child.recycle()
            }
        }
    }
    
    private fun clickElement(params: JSONObject): Result {
        try {
            val root = rootInActiveWindow ?: return Result.error("无活动窗口")
            
            val text = params.optString("text", null)
            val id = params.optString("id", null)
            val contentDesc = params.optString("contentDescription", null)
            
            val node = findFirstElement(root, text, id, contentDesc)
            root.recycle()
            
            if (node == null) {
                return Result.error("未找到元素")
            }
            
            // 查找可点击的父节点
            var target = node
            while (target != null && !target.isClickable) {
                val parent = target.parent
                if (target != node) target.recycle()
                target = parent
            }
            
            if (target == null) {
                node.recycle()
                return Result.error("未找到可点击元素")
            }
            
            val result = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val desc = node.text?.toString() ?: node.contentDescription?.toString() ?: "元素"
            if (target != node) target.recycle()
            node.recycle()
            
            return if (result) {
                Result.ok(mapOf("clicked" to desc))
            } else {
                Result.error("点击失败")
            }
        } catch (e: Exception) {
            return Result.error("点击元素失败: ${e.message}")
        }
    }
    
    private fun findFirstElement(
        root: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        contentDesc: String?
    ): AccessibilityNodeInfo? {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findFirstElementRecursive(root, text, id, contentDesc, results)
        return results.firstOrNull()
    }
    
    private fun findFirstElementRecursive(
        node: AccessibilityNodeInfo,
        text: String?,
        id: String?,
        contentDesc: String?,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (results.isNotEmpty()) return
        
        var match = true
        
        if (text != null && node.text?.toString()?.contains(text, ignoreCase = true) != true) {
            match = false
        }
        if (id != null && node.viewIdResourceName?.contains(id, ignoreCase = true) != true) {
            match = false
        }
        if (contentDesc != null && node.contentDescription?.toString()?.contains(contentDesc, ignoreCase = true) != true) {
            match = false
        }
        
        if (match) {
            results.add(node)
            return
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findFirstElementRecursive(child, text, id, contentDesc, results)
                if (results.isEmpty()) {
                    child.recycle()
                }
            }
        }
    }
    
    private fun nodeToMap(node: AccessibilityNodeInfo): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        map["className"] = node.className?.toString() ?: ""
        map["text"] = node.text?.toString() ?: ""
        map["contentDescription"] = node.contentDescription?.toString() ?: ""
        map["viewIdResourceName"] = node.viewIdResourceName ?: ""
        map["isClickable"] = node.isClickable
        map["isScrollable"] = node.isScrollable
        map["isEditable"] = node.isEditable
        
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        map["bounds"] = mapOf(
            "left" to bounds.left,
            "top" to bounds.top,
            "right" to bounds.right,
            "bottom" to bounds.bottom
        )
        
        return map
    }

    // ============ 文本点击（OCR + 坐标） ============
    
    private fun clickText(params: JSONObject): Result {
        try {
            val text = params.getString("text")
            val fuzzy = params.optBoolean("fuzzy", true)
            
            // 方法1：通过无障碍服务查找
            val root = rootInActiveWindow
            if (root != null) {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                val match = nodes.firstOrNull { node ->
                    if (fuzzy) {
                        node.text?.toString()?.contains(text, ignoreCase = true) == true
                    } else {
                        node.text?.toString() == text
                    }
                }
                
                if (match != null) {
                    val clicked = if (match.isClickable) {
                        match.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else {
                        val bounds = Rect()
                        match.getBoundsInScreen(bounds)
                        tap(JSONObject().apply {
                            put("x", (bounds.left + bounds.right) / 2)
                            put("y", (bounds.top + bounds.bottom) / 2)
                        }).success
                    }
                    root.recycle()
                    match.recycle()
                    
                    return if (clicked) {
                        Result.ok(mapOf("clicked" to text))
                    } else {
                        Result.error("点击失败")
                    }
                }
                root.recycle()
            }
            
            // 方法2：通过 OCR 查找
            val ocrResult = ocr(JSONObject())
            if (!ocrResult.success || ocrResult.data == null) {
                return Result.error("OCR 失败")
            }
            
            @Suppress("UNCHECKED_CAST")
            val words = ocrResult.data!!["words"] as? List<Map<String, Any?>> ?: return Result.error("无 OCR 结果")
            
            for (word in words) {
                val wordText = word["text"] as? String ?: continue
                val isMatch = if (fuzzy) {
                    wordText.contains(text, ignoreCase = true)
                } else {
                    wordText == text
                }
                
                if (isMatch) {
                    val x = (word["x"] as? Number)?.toInt()?.let { it + (word["width"] as? Number)?.toInt()?.div(2) } ?: continue
                    val y = (word["y"] as? Number)?.toInt()?.let { it + (word["height"] as? Number)?.toInt()?.div(2) } ?: continue
                    
                    return tap(JSONObject().apply {
                        put("x", x)
                        put("y", y)
                    })
                }
            }
            
            return Result.error("未找到文本: $text")
        } catch (e: Exception) {
            return Result.error("点击文本失败: ${e.message}")
        }
    }
    
    private fun findText(params: JSONObject): Result {
        try {
            val text = params.getString("text")
            val fuzzy = params.optBoolean("fuzzy", true)
            
            val ocrResult = ocr(JSONObject())
            if (!ocrResult.success || ocrResult.data == null) {
                return Result.error("OCR 失败")
            }
            
            @Suppress("UNCHECKED_CAST")
            val words = ocrResult.data!!["words"] as? List<Map<String, Any?>> ?: return Result.error("无 OCR 结果")
            
            val matches = mutableListOf<Map<String, Any?>>()
            
            for (word in words) {
                val wordText = word["text"] as? String ?: continue
                val isMatch = if (fuzzy) {
                    wordText.contains(text, ignoreCase = true)
                } else {
                    wordText == text
                }
                
                if (isMatch) {
                    matches.add(word)
                }
            }
            
            return Result.ok(mapOf(
                "found" to (matches.isNotEmpty()),
                "matches" to matches,
                "count" to matches.size
            ))
        } catch (e: Exception) {
            return Result.error("查找文本失败: ${e.message}")
        }
    }

    // ============ OCR ============
    
    private fun ocr(params: JSONObject): Result {
        try {
            val engine = ocrEngine ?: return Result.error("OCR 未初始化")
            
            val screenshotResult = screenshot(JSONObject())
            if (!screenshotResult.success) {
                return Result.error("截图失败: ${screenshotResult.error}")
            }
            
            val imageData = screenshotResult.data?.get("image") as? String
            if (imageData.isNullOrEmpty()) {
                return Result.error("无图像数据")
            }
            
            val bytes = Base64.decode(imageData, Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return Result.error("解码图像失败")
            
            val ocrResult = engine.recognize(bitmap)
            bitmap.recycle()
            
            if (!ocrResult.success) {
                return Result.error(ocrResult.error ?: "OCR 失败")
            }
            
            val wordsList = ocrResult.words.map { word ->
                mapOf<String, Any?>(
                    "text" to word.text,
                    "confidence" to word.confidence,
                    "x" to word.x,
                    "y" to word.y,
                    "width" to word.width,
                    "height" to word.height
                )
            }
            
            return Result.ok(mapOf(
                "text" to ocrResult.fullText,
                "words" to wordsList,
                "wordCount" to ocrResult.words.size
            ))
        } catch (e: Exception) {
            return Result.error("OCR 失败: ${e.message}")
        }
    }

    // ============ 手势操作 ============
    
    private fun tap(params: JSONObject): Result {
        return try {
            val x = params.getDouble("x").toFloat()
            val y = params.getDouble("y").toFloat()
            val duration = params.optLong("duration", 100L)
            
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            
            dispatchGestureSync(gesture, "tap($x, $y)")
        } catch (e: Exception) {
            Result.error("点击失败: ${e.message}")
        }
    }
    
    private fun longPress(params: JSONObject): Result {
        return try {
            val x = params.getDouble("x").toFloat()
            val y = params.getDouble("y").toFloat()
            val duration = params.optLong("duration", 1000L)
            
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            
            dispatchGestureSync(gesture, "longPress($x, $y)")
        } catch (e: Exception) {
            Result.error("长按失败: ${e.message}")
        }
    }
    
    private fun swipe(params: JSONObject): Result {
        return try {
            val startX = params.getDouble("start_x").toFloat()
            val startY = params.getDouble("start_y").toFloat()
            val endX = params.getDouble("end_x").toFloat()
            val endY = params.getDouble("end_y").toFloat()
            val duration = params.optLong("duration", 300L)
            
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            
            dispatchGestureSync(gesture, "swipe($startX,$startY -> $endX,$endY)")
        } catch (e: Exception) {
            Result.error("滑动失败: ${e.message}")
        }
    }
    
    private fun scroll(params: JSONObject): Result {
        return try {
            val direction = params.optString("direction", "down")
            val distance = params.optDouble("distance", 500.0).toFloat()
            
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            val centerX = screenWidth / 2
            val centerY = screenHeight / 2
            
            val (startX, startY, endX, endY) = when (direction) {
                "up" -> listOf(centerX, centerY + distance / 2, centerX, centerY - distance / 2)
                "down" -> listOf(centerX, centerY - distance / 2, centerX, centerY + distance / 2)
                "left" -> listOf(centerX + distance / 2, centerY, centerX - distance / 2, centerY)
                "right" -> listOf(centerX - distance / 2, centerY, centerX + distance / 2, centerY)
                else -> return Result.error("未知方向: $direction")
            }
            
            return swipe(JSONObject().apply {
                put("start_x", startX)
                put("start_y", startY)
                put("end_x", endX)
                put("end_y", endY)
            })
        } catch (e: Exception) {
            Result.error("滚动失败: ${e.message}")
        }
    }
    
    private fun dispatchGestureSync(gesture: GestureDescription, label: String): Result {
        val latch = CountDownLatch(1)
        var success = false
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                success = true
                latch.countDown()
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                success = false
                latch.countDown()
            }
        }, null)
        
        latch.await(5, TimeUnit.SECONDS)
        
        return if (success) {
            Result.ok(mapOf("action" to label))
        } else {
            Result.error("手势 $label 失败或超时")
        }
    }

    // ============ 文本输入 ============
    
    private fun typeText(params: JSONObject): Result {
        return try {
            val text = params.getString("text")
            val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: return Result.error("无焦点输入框")
            
            val args = android.os.Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    (focused.text?.toString() ?: "") + text
                )
            }
            val result = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focused.recycle()
            
            return if (result) {
                Result.ok(mapOf("typed" to text))
            } else {
                Result.error("输入文本失败")
            }
        } catch (e: Exception) {
            Result.error("输入失败: ${e.message}")
        }
    }

    // ============ 按键操作 ============
    
    private fun pressKey(params: JSONObject): Result {
        return try {
            val key = params.getString("key")
            val action = when (key.lowercase()) {
                "back" -> GLOBAL_ACTION_BACK
                "home" -> GLOBAL_ACTION_HOME
                "recents", "recent" -> GLOBAL_ACTION_RECENTS
                "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
                "power_dialog" -> GLOBAL_ACTION_POWER_DIALOG
                "lock_screen" -> if (Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_LOCK_SCREEN else return Result.error("lock_screen 需要 API 28+")
                "take_screenshot" -> if (Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_TAKE_SCREENSHOT else return Result.error("take_screenshot 需要 API 28+")
                else -> return Result.error("未知按键: $key")
            }
            
            val result = performGlobalAction(action)
            return if (result) {
                Result.ok(mapOf("key" to key))
            } else {
                Result.error("按键失败: $key")
            }
        } catch (e: Exception) {
            Result.error("按键失败: ${e.message}")
        }
    }

    // ============ 应用管理 ============
    
    private fun openApp(params: JSONObject): Result {
        return try {
            val packageName = params.getString("package")
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.error("应用未安装: $packageName")
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            Result.ok(mapOf("opened" to packageName))
        } catch (e: Exception) {
            Result.error("打开应用失败: ${e.message}")
        }
    }
    
    private fun closeApp(params: JSONObject): Result {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Result.ok(mapOf("closed" to true))
        } catch (e: Exception) {
            Result.error("关闭应用失败: ${e.message}")
        }
    }
    
    private fun currentApp(params: JSONObject): Result {
        return try {
            val root = rootInActiveWindow ?: return Result.error("无活动窗口")
            val packageName = root.packageName?.toString() ?: "未知"
            root.recycle()
            
            Result.ok(mapOf("package" to packageName))
        } catch (e: Exception) {
            Result.error("获取当前应用失败: ${e.message}")
        }
    }

    // ============ 截图 ============
    
    private fun screenshot(params: JSONObject): Result {
        return try {
            if (Build.VERSION.SDK_INT < 30) {
                return Result.error("截图需要 API 30+")
            }
            
            val quality = params.optInt("quality", 80)
            val latch = CountDownLatch(1)
            var resultBitmap: Bitmap? = null
            
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                    val hwBitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer, screenshot.colorSpace
                    )
                    resultBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                    hwBitmap?.recycle()
                    screenshot.hardwareBuffer.close()
                    latch.countDown()
                }
                
                override fun onFailure(errorCode: Int) {
                    latch.countDown()
                }
            })
            
            latch.await(5, TimeUnit.SECONDS)
            
            val bitmap = resultBitmap ?: return Result.error("截图失败")
            
            // 转换为 Base64
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
            
            val width = bitmap.width
            val height = bitmap.height
            bitmap.recycle()
            
            Result.ok(mapOf(
                "image" to base64,
                "width" to width,
                "height" to height
            ))
        } catch (e: Exception) {
            Result.error("截图失败: ${e.message}")
        }
    }

    // ============ 辅助功能 ============
    
    private fun wait(params: JSONObject): Result {
        return try {
            val duration = params.getLong("duration")
            Thread.sleep(duration)
            Result.ok(mapOf("waited" to duration))
        } catch (e: Exception) {
            Result.error("等待失败: ${e.message}")
        }
    }
    
    private fun getScreenSize(params: JSONObject): Result {
        return try {
            val displayMetrics = resources.displayMetrics
            Result.ok(mapOf(
                "width" to displayMetrics.widthPixels,
                "height" to displayMetrics.heightPixels,
                "density" to displayMetrics.density
            ))
        } catch (e: Exception) {
            Result.error("获取屏幕尺寸失败: ${e.message}")
        }
    }
}
