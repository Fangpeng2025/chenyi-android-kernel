package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.benjaminwan.ocrlibrary.OcrEngine as RapidOcrEngine
import com.benjaminwan.ocrlibrary.OcrResult as RapidOcrResult
import com.benjaminwan.ocrlibrary.TextBlock

/**
 * OCR 引擎 - 使用 RapidOCR
 */
class OcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OcrEngine"
    }

    private var initialized = false
    private var rapidOcr: RapidOcrEngine? = null

    /**
     * 初始化 OCR
     */
    fun init(): Boolean {
        return try {
            rapidOcr = RapidOcrEngine(context)
            
            // 设置参数
            rapidOcr?.padding = 50
            rapidOcr?.boxScoreThresh = 0.5f
            rapidOcr?.boxThresh = 0.3f
            rapidOcr?.unClipRatio = 1.6f
            rapidOcr?.doAngle = true
            rapidOcr?.mostAngle = true
            
            initialized = true
            Log.d(TAG, "RapidOCR 初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RapidOCR 初始化失败: ${e.message}")
            initialized = false
            false
        }
    }

    /**
     * 识别图片中的文字
     */
    fun recognize(bitmap: Bitmap): OcrResult {
        if (!initialized || rapidOcr == null) {
            return OcrResult.error("OCR 未初始化")
        }

        return try {
            // 创建输出 Bitmap
            val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            
            // 执行 OCR
            val result = rapidOcr?.detect(bitmap, output, 1024)
            
            // 解析结果
            parseRapidOcrResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败: ${e.message}")
            OcrResult.error(e.message ?: "识别失败")
        }
    }

    /**
     * 识别图片文件中的文字
     */
    fun recognize(imagePath: String): OcrResult {
        if (!initialized) {
            return OcrResult.error("OCR 未初始化")
        }

        return try {
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                return OcrResult.error("无法加载图片")
            }
            
            val result = recognize(bitmap)
            bitmap.recycle()
            result
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败: ${e.message}")
            OcrResult.error(e.message ?: "识别失败")
        }
    }

    /**
     * 解析 RapidOCR 结果
     */
    private fun parseRapidOcrResult(result: RapidOcrResult?): OcrResult {
        if (result == null) {
            return OcrResult.error("识别结果为空")
        }

        val words = mutableListOf<OcrWord>()
        val fullText = StringBuilder()

        for (textBlock in result.textBlocks) {
            // 获取文本框的坐标
            val boxPoints = textBlock.boxPoint
            if (boxPoints.isNotEmpty()) {
                // 计算边界框
                var minX = Int.MAX_VALUE
                var minY = Int.MAX_VALUE
                var maxX = Int.MIN_VALUE
                var maxY = Int.MIN_VALUE
                
                for (point in boxPoints) {
                    minX = minOf(minX, point.x)
                    minY = minOf(minY, point.y)
                    maxX = maxOf(maxX, point.x)
                    maxY = maxOf(maxY, point.y)
                }
                
                val word = OcrWord(
                    text = textBlock.text,
                    confidence = textBlock.boxScore,
                    x = minX,
                    y = minY,
                    width = maxX - minX,
                    height = maxY - minY
                )
                words.add(word)
                fullText.append(textBlock.text).append(" ")
            }
        }

        Log.d(TAG, "OCR 识别完成: ${words.size} 个文本块")
        return OcrResult(
            success = true,
            words = words,
            fullText = fullText.toString().trim()
        )
    }
}

/**
 * OCR 结果
 */
data class OcrResult(
    val success: Boolean,
    val words: List<OcrWord>,
    val fullText: String,
    val error: String? = null
) {
    companion object {
        fun error(msg: String) = OcrResult(false, emptyList(), "", msg)
    }
}

/**
 * OCR 单词
 */
data class OcrWord(
    val text: String,
    val confidence: Float,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)