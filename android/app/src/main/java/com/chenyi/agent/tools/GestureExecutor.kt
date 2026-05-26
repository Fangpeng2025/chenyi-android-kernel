package com.chenyi.agent.tools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 手势执行器 - 处理所有手势操作
 * 
 * 职责：
 * - 执行点击、长按、滑动等手势
 * - 管理手势超时和回调
 * - 提供统一的手势执行接口
 */
class GestureExecutor(
    private val service: AccessibilityService
) {
    companion object {
        private const val TAG = "GestureExecutor"
        private const val DEFAULT_DURATION = 100L
        private const val LONG_PRESS_DURATION = 500L
        private const val SWIPE_DURATION = 300L
        private const val GESTURE_TIMEOUT = 5L
    }
    
    /**
     * 点击
     */
    fun tap(x: Int, y: Int): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Result.error("需要 Android 7.0+")
        }
        
        return performGesture {
            addStroke(GestureDescription.StrokeDescription(
                Path().apply {
                    moveTo(x.toFloat(), y.toFloat())
                },
                0, DEFAULT_DURATION
            ))
        }
    }
    
    /**
     * 长按
     */
    fun longPress(x: Int, y: Int, duration: Long = LONG_PRESS_DURATION): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Result.error("需要 Android 7.0+")
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
    
    /**
     * 滑动
     */
    fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long = SWIPE_DURATION): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Result.error("需要 Android 7.0+")
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
    
    /**
     * 滚动（垂直）
     */
    fun scrollVertical(x: Int, startY: Int, endY: Int, duration: Long = SWIPE_DURATION): Result {
        return swipe(x, startY, x, endY, duration)
    }
    
    /**
     * 滚动（水平）
     */
    fun scrollHorizontal(y: Int, startX: Int, endX: Int, duration: Long = SWIPE_DURATION): Result {
        return swipe(startX, y, endX, y, duration)
    }
    
    /**
     * 双击
     */
    fun doubleTap(x: Int, y: Int): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Result.error("需要 Android 7.0+")
        }
        
        return performGesture {
            // 第一次点击
            addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                0, DEFAULT_DURATION
            ))
            // 第二次点击（延迟 200ms）
            addStroke(GestureDescription.StrokeDescription(
                Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                200, DEFAULT_DURATION
            ))
        }
    }
    
    /**
     * 执行手势（内部方法）
     */
    private inline fun performGesture(buildGesture: GestureDescription.Builder.() -> Unit): Result {
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
        
        latch.await(GESTURE_TIMEOUT, TimeUnit.SECONDS)
        
        return if (success) {
            Result.success()
        } else {
            Result.error("手势执行失败")
        }
    }
}