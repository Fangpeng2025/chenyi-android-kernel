package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * OCR 引擎 - 使用 RapidOCR
 */
class OcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OcrEngine"
        
        // 加载 native 库
        init {
            try {
                System.loadLibrary("rapidocr")
                Log.d(TAG, "RapidOCR 库加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "RapidOCR 库加载失败: ${e.message}")
            }
        }
    }

    // JNI 方法
    private external fun nativeInit(modelPath: String): Boolean
    private external fun nativeOcr(imageData: ByteArray, width: Int, height: Int): String
    private external fun nativeOcrFromPath(imagePath: String): String

    private var initialized = false

    /**
     * 初始化 OCR
     */
    fun init(): Boolean {
        val modelDir = File(context.filesDir, "ocr_models")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        
        initialized = try {
            nativeInit(modelDir.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "OCR 初始化失败: ${e.message}")
            false
        }
        
        Log.d(TAG, "OCR 初始化: $initialized")
        return initialized
    }

    /**
     * 识别图片中的文字
     */
    fun recognize(bitmap: Bitmap): OcrResult {
        if (!initialized) {
            return OcrResult.error("OCR 未初始化")
        }

        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.RGB_565, 100, stream)
            val imageData = stream.toByteArray()

            val json = nativeOcr(imageData, bitmap.width, bitmap.height)
            parseResult(json)
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
            val json = nativeOcrFromPath(imagePath)
            parseResult(json)
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败: ${e.message}")
            OcrResult.error(e.message ?: "识别失败")
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