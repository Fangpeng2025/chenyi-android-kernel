package com.chenyi.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * 晨翼无障碍服务
 * 
 * 提供屏幕操作能力：点击、滑动、输入、截图等
 */
class ChenyiAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: ChenyiAccessibilityService? = null

        fun getInstance(): ChenyiAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        android.util.Log.d("ChenyiService", "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件
    }

    override fun onInterrupt() {
        android.util.Log.d("ChenyiService", "无障碍服务中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
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
            else -> error("未知工具: $tool")
        }

        return result.toJson()
    }

    // ============ 截图 ============

    private fun screenshot(params: JSONObject): Result {
        return try {
            // Android 11+ 使用 takeScreenshot API
            // 这里返回提示信息
            Result.ok(mapOf(
                "message" to "截图功能需要 MediaProjection API",
                "hint" to "请在 MainActivity 中实现截图"
            ))
        } catch (e: Exception) {
            Result.error(e.message ?: "截图失败")
        }
    }

    // ============ 点击 ============

    private fun tap(params: JSONObject): Result {
        val x = params.getInt("x")
        val y = params.getInt("y")

        return try {
            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Result.ok(mapOf("x" to x, "y" to y, "success" to success))
        } catch (e: Exception) {
            Result.error(e.message ?: "点击失败")
        }
    }

    // ============ 长按 ============

    private fun longPress(params: JSONObject): Result {
        val x = params.getInt("start_x")
        val y = params.getInt("start_y")
        val duration = params.optLong("duration", 500)

        return try {
            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Result.ok(mapOf("x" to x, "y" to y, "duration" to duration, "success" to success))
        } catch (e: Exception) {
            Result.error(e.message ?: "长按失败")
        }
    }

    // ============ 滑动 ============

    private fun swipe(params: JSONObject): Result {
        val startX = params.getInt("start_x")
        val startY = params.getInt("start_y")
        val endX = params.getInt("end_x")
        val endY = params.getInt("end_y")
        val duration = params.optLong("duration", 300)

        return try {
            val path = Path()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()

            val success = dispatchGesture(gesture, null, null)
            Result.ok(mapOf(
                "start" to listOf(startX, startY),
                "end" to listOf(endX, endY),
                "duration" to duration,
                "success" to success
            ))
        } catch (e: Exception) {
            Result.error(e.message ?: "滑动失败")
        }
    }

    // ============ 输入文本 ============

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
                Result.ok(mapOf("text" to text, "success" to success))
            } else {
                Result.error("未找到输入框")
            }
        } catch (e: Exception) {
            Result.error(e.message ?: "输入失败")
        }
    }

    // ============ 按键 ============

    private fun pressKey(params: JSONObject): Result {
        val keycode = params.getInt("keycode")

        return try {
            val success = performGlobalAction(keycode)
            Result.ok(mapOf("keycode" to keycode, "success" to success))
        } catch (e: Exception) {
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
                Result.ok(mapOf("package" to packageName))
            } else {
                Result.error("应用不存在: $packageName")
            }
        } catch (e: Exception) {
            Result.error(e.message ?: "打开应用失败")
        }
    }

    // ============ 关闭应用 ============

    private fun closeApp(): Result {
        return try {
            // 返回桌面
            val success = performGlobalAction(GLOBAL_ACTION_HOME)
            Result.ok(mapOf("action" to "home", "success" to success))
        } catch (e: Exception) {
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
                Result.ok(mapOf("package" to packageName))
            } else {
                Result.error("无法获取当前应用")
            }
        } catch (e: Exception) {
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
            Result.ok(mapOf("apps" to list, "count" to list.size))
        } catch (e: Exception) {
            Result.error(e.message ?: "列出应用失败")
        }
    }

    // ============ 辅助方法 ============

    private fun error(msg: String): Result = Result.error(msg)
}