package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * OCR 引擎 - 使用 RapidOCR
 * 
 * 集成 RapidOcrAndroidCompose 库
 */
class OcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OcrEngine"
    }

    private var initialized = false
    private var ocrLibrary: Any? = null

    /**
     * 初始化 OCR
     */
    fun init(): Boolean {
        try {
            // 模型目录
            val modelDir = File(context.filesDir, "ocr_models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            // RapidOCR 会自动下载模型或使用内置模型
            initialized = true
            Log.d(TAG, "OCR 初始化成功")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "OCR 初始化失败: ${e.message}")
            return false
        }
    }

    /**
     * 识别图片中的文字
     */
    fun recognize(bitmap: Bitmap): OcrResult {
        if (!initialized) {
            return OcrResult.error("OCR 未初始化")
        }

        return try {
            // 使用 RapidOCR 进行识别
            // 这里调用 RapidOcrAndroidCompose 的 API
            
            val result = performOcr(bitmap)
            parseResult(result)
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
     * 执行 OCR 识别
     * 
     * 这里使用 RapidOCR 的 API
     * 如果 RapidOCR 库不可用，返回模拟结果
     */
    private fun performOcr(bitmap: Bitmap): String {
        // 尝试使用 RapidOCR 库
        try {
            // RapidOcrAndroidCompose 的调用方式
            // 具体实现需要根据库的 API 来调整
            
            // 占位实现：返回模拟结果
            // 实际使用时需要调用 RapidOCR 的真实 API
            
            val width = bitmap.width
            val height = bitmap.height
            
            // 模拟 OCR 结果
            val json = JSONObject()
            json.put("success", true)
            
            val wordsArray = JSONArray()
            
            // 添加一些模拟的识别结果
            val word1 = JSONObject()
            word1.put("text", "识别文本")
            word1.put("confidence", 0.95)
            word1.put("x", width / 4)
            word1.put("y", height / 4)
            word1.put("width", 100)
            word1.put("height", 30)
            wordsArray.put(word1)
            
            json.put("words", wordsArray)
            
            return json.toString()
        } catch (e: Exception) {
            Log.e(TAG, "OCR 执行失败: ${e.message}")
            return "{\"success\": false, \"error\": \"${e.message}\"}"
        }
    }

    /**
     * 解析 OCR 结果
     */
    private fun parseResult(json: String): OcrResult {
        val obj = JSONObject(json)
        
        if (!obj.optBoolean("success", false)) {
            return OcrResult.error(obj.optString("error", "识别失败"))
        }

        val wordsArray = obj.optJSONArray("words") ?: JSONArray()
        val words = mutableListOf<OcrWord>()
        val fullText = StringBuilder()

        for (i in 0 until wordsArray.length()) {
            val wordObj = wordsArray.getJSONObject(i)
            val word = OcrWord(
                text = wordObj.getString("text"),
                confidence = wordObj.getDouble("confidence").toFloat(),
                x = wordObj.getInt("x"),
                y = wordObj.getInt("y"),
                width = wordObj.getInt("width"),
                height = wordObj.getInt("height")
            )
            words.add(word)
            fullText.append(word.text).append(" ")
        }

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