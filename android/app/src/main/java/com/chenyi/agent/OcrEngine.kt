package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import io.github.hzkitty.RapidOCR
import io.github.hzkitty.entity.OcrResult as RapidOcrResult

/**
 * OCR 引擎 - 使用 RapidOCR4j-Android
 */
class OcrEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "OcrEngine"
    }
    
    private var rapidOCR: RapidOCR? = null
    private var initialized = false
    
    /**
     * 初始化 OCR（同步版本）
     */
    fun initSync(): Boolean {
        return try {
            rapidOCR = RapidOCR.create(context)
            initialized = true
            Log.i(TAG, "RapidOCR 初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RapidOCR 初始化失败: ${e.message}")
            initialized = false
            false
        }
    }
    
    /**
     * 初始化 OCR（协程版本）
     */
    suspend fun init(): Boolean {
        return initSync()
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
    
    /**
     * 识别图片中的文字
     */
    fun recognize(bitmap: Bitmap): ChenyiOcrResult {
        if (!initialized || rapidOCR == null) {
            return ChenyiOcrResult.error("OCR 未初始化")
        }
        
        return try {
            val result = rapidOCR!!.run(bitmap)
            parseRapidOcrResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败: ${e.message}")
            ChenyiOcrResult.error(e.message ?: "识别失败")
        }
    }
    
    /**
     * 识别图片文件中的文字
     */
    fun recognize(imagePath: String): ChenyiOcrResult {
        if (!initialized) {
            return ChenyiOcrResult.error("OCR 未初始化")
        }
        
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                return ChenyiOcrResult.error("无法加载图片")
            }
            
            val result = recognize(bitmap)
            bitmap.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败: ${e.message}")
            ChenyiOcrResult.error(e.message ?: "识别失败")
        }
    }
    
    /**
     * 解析 RapidOCR 结果
     */
    private fun parseRapidOcrResult(result: RapidOcrResult?): ChenyiOcrResult {
        if (result == null) {
            return ChenyiOcrResult.error("识别结果为空")
        }
        
        val words = mutableListOf<ChenyiOcrWord>()
        val fullText = StringBuilder()
        
        result.recRes?.forEach { rec ->
            val text = rec.text
            val confidence = rec.confidence
            
            // 解析边界框
            var x = 0
            var y = 0
            var width = 0
            var height = 0
            
            val boxes = rec.dtBoxes
            if (boxes != null && boxes.size >= 4) {
                // 四个角坐标：左上、右上、右下、左下
                x = boxes[0].x.toInt()
                y = boxes[0].y.toInt()
                width = boxes[2].x.toInt() - x
                height = boxes[2].y.toInt() - y
            }
            
            val word = ChenyiOcrWord(
                text = text,
                confidence = confidence,
                x = x,
                y = y,
                width = width,
                height = height
            )
            words.add(word)
            fullText.append(text).append(" ")
        }
        
        Log.d(TAG, "OCR 识别完成: ${words.size} 个文本块")
        return ChenyiOcrResult(
            success = true,
            words = words,
            fullText = fullText.toString().trim()
        )
    }
}

/**
 * 晨翼 OCR 结果（避免与 RapidOCR 的 OcrResult 冲突）
 */
data class ChenyiOcrResult(
    val success: Boolean,
    val words: List<ChenyiOcrWord>,
    val fullText: String,
    val error: String? = null
) {
    companion object {
        fun error(msg: String) = ChenyiOcrResult(false, emptyList(), "", msg)
    }
}

/**
 * 晨翼 OCR 单词
 */
data class ChenyiOcrWord(
    val text: String,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)