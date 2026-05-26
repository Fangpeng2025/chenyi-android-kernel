package com.chenyi.agent.utils

import android.content.Context
import android.util.Log
import com.chenyi.agent.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OCR 初始化管理器
 * 
 * 职责：
 * - 异步初始化 OCR 引擎
 * - 单例模式管理 OCR 实例
 * - 提供初始化状态
 */
object OcrInitializer {
    private const val TAG = "OcrInitializer"
    
    @Volatile
    private var instance: OcrEngine? = null
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var isInitializing = false
    
    /**
     * 获取 OCR 实例
     */
    fun getInstance(): OcrEngine? = instance
    
    /**
     * 是否已初始化
     */
    fun isReady(): Boolean = isInitialized && instance != null
    
    /**
     * 是否正在初始化
     */
    fun isInitializing(): Boolean = isInitializing
    
    /**
     * 异步初始化
     * 
     * @param context 上下文
     * @return 是否成功
     */
    suspend fun initializeAsync(context: Context): Boolean {
        if (isInitialized && instance != null) {
            return true
        }
        
        if (isInitializing) {
            // 等待初始化完成
            while (isInitializing) {
                Thread.sleep(100)
            }
            return isInitialized
        }
        
        isInitializing = true
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始初始化 OCR 引擎...")
                
                val engine = OcrEngine(context)
                engine.initSync()
                
                instance = engine
                isInitialized = true
                
                Log.d(TAG, "OCR 引擎初始化成功")
                true
            } catch (e: Exception) {
                Log.e(TAG, "OCR 引擎初始化失败", e)
                instance = null
                isInitialized = false
                false
            } finally {
                isInitializing = false
            }
        }
    }
    
    /**
     * 同步初始化（在后台线程调用）
     */
    fun initializeSync(context: Context): Boolean {
        if (isInitialized && instance != null) {
            return true
        }
        
        return try {
            Log.d(TAG, "开始同步初始化 OCR 引擎...")
            
            val engine = OcrEngine(context)
            engine.initSync()
            
            instance = engine
            isInitialized = true
            
            Log.d(TAG, "OCR 引擎初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "OCR 引擎初始化失败", e)
            instance = null
            isInitialized = false
            false
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        instance = null
        isInitialized = false
        isInitializing = false
        Log.d(TAG, "OCR 引擎已释放")
    }
}
