package com.chenyi.agent.tools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 工具执行器 - 管理所有工具的执行逻辑
 * 
 * 职责：
 * - 执行具体的工具操作
 * - 封装复杂的 UI 自动化逻辑
 * - 提供统一的错误处理
 */
class ToolExecutor(
    private val service: AccessibilityService
) {
    companion object {
        private const val TAG = "ToolExecutor"
    }
    
    // ============ 截图工具 ============
    
    fun screenshot(params: JSONObject): Result {
        return try {
            val root = service.rootInActiveWindow ?: return Result.error("无法获取根节点")
            
            val bitmap = captureScreen(root)
            val base64 = bitmapToBase64(bitmap)
            
            Result.success(JSONObject().apply {
                put("image", base64)
                put("width", bitmap.width)
                put("height", bitmap.height)
            })
        } catch (e: Exception) {
            Log.e(TAG, "截图失败", e)
            Result.error("截图失败: ${e.message}")
        }
    }
    
    private fun captureScreen(node: AccessibilityNodeInfo): Bitmap {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        // 使用无障碍服务的截图 API
        // 注意：需要 API 21+
        throw NotImplementedError("截图功能需要实现")
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
    
    // ============ 点击工具 ============
    
    fun tap(params: JSONObject): Result {
        val x = params.optInt("x", -1)
        val y = params.optInt("y", -1)
        
        if (x < 0 || y < 0) {
            return Result.error("坐标无效: x=$x, y=$y")
        }
        
        return performGesture {
            addStroke(GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(x.toFloat(), y.toFloat())
                },
                0, 100
            ))
        }
    }
    
    fun longPress(params: JSONObject): Result {
        val x = params.optInt("x", -1)
        val y = params.optInt("y", -1)
        val duration = params.optLong("duration", 500)
        
        if (x < 0 || y < 0) {
            return Result.error("坐标无效: x=$x, y=$y")
        }
        
        return performGesture {
            addStroke(GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(x.toFloat(), y.toFloat())
                },
                0, duration
            ))
        }
    }
    
    // ============ 滑动工具 ============
    
    fun swipe(params: JSONObject): Result {
        val startX = params.optInt("start_x", -1)
        val startY = params.optInt("start_y", -1)
        val endX = params.optInt("end_x", -1)
        val endY = params.optInt("end_y", -1)
        val duration = params.optLong("duration", 300)
        
        if (startX < 0 || startY < 0 || endX < 0 || endY < 0) {
            return Result.error("坐标无效")
        }
        
        return performGesture {
            addStroke(GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(startX.toFloat(), startY.toFloat())
                    lineTo(endX.toFloat(), endY.toFloat())
                },
                0, duration
            ))
        }
    }
    
    // ============ 手势执行辅助 ============
    
    private inline fun performGesture(buildGesture: GestureDescription.Builder.() -> Unit): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Result.error("需要 Android 7.0+")
        }
        
        val builder = GestureDescription.Builder()
        builder.buildGesture()
        val gesture = builder.build()
        
        val latch = CountDownLatch(1)
        var success = false
        
        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                success = true
                latch.countDown()
            }
            
            override fun onCancelled(gestureDescription: GestureDescription?) {
                latch.countDown()
            }
        }, null)
        
        latch.await(5, TimeUnit.SECONDS)
        
        return if (success) Result.success() else Result.error("手势执行失败")
    }
    
    // ============ 文本输入工具 ============
    
    fun typeText(params: JSONObject): Result {
        val text = params.optString("text", "")
        if (text.isEmpty()) {
            return Result.error("文本不能为空")
        }
        
        // 查找当前焦点的节点
        val root = service.rootInActiveWindow ?: return Result.error("无法获取根节点")
        val focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return Result.error("未找到输入框")
        
        // 输入文本
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        
        return if (focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            Result.success()
        } else {
            Result.error("文本输入失败")
        }
    }
    
    // ============ 按键工具 ============
    
    fun pressKey(params: JSONObject): Result {
        val keyCode = params.optInt("key_code", -1)
        if (keyCode < 0) {
            return Result.error("按键代码无效")
        }
        
        return try {
            service.performGlobalAction(keyCode)
            Result.success()
        } catch (e: Exception) {
            Result.error("按键执行失败: ${e.message}")
        }
    }
    
    // ============ 应用操作工具 ============
    
    fun openApp(params: JSONObject): Result {
        val packageName = params.optString("package_name", "")
        if (packageName.isEmpty()) {
            return Result.error("包名不能为空")
        }
        
        return try {
            val intent = service.packageManager.getLaunchIntentForPackage(packageName)
                ?: return Result.error("未找到应用: $packageName")
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
            
            Result.success()
        } catch (e: Exception) {
            Result.error("打开应用失败: ${e.message}")
        }
    }
    
    fun currentApp(params: JSONObject): Result {
        val root = service.rootInActiveWindow ?: return Result.error("无法获取根节点")
        val packageName = root.packageName?.toString() ?: return Result.error("无法获取包名")
        
        return Result.success(JSONObject().apply {
            put("package_name", packageName)
        })
    }
    
    // ============ 屏幕信息工具 ============
    
    fun getScreenSize(params: JSONObject): Result {
        val root = service.rootInActiveWindow ?: return Result.error("无法获取根节点")
        val rect = Rect()
        root.getBoundsInScreen(rect)
        
        return Result.success(JSONObject().apply {
            put("width", rect.width())
            put("height", rect.height())
        })
    }
    
    // ============ 等待工具 ============
    
    fun wait(params: JSONObject): Result {
        val duration = params.optLong("duration", 1000)
        Thread.sleep(duration)
        return Result.success()
    }
}
