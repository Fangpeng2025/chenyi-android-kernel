package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * OCR 引擎 - 使用 Google ML Kit
 * 
 * 支持中文和英文识别
 */
class OcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OcrEngine"
    }

    private var initialized = false
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /**
     * 初始化 OCR
     */
    fun init(): Boolean {
        initialized = true
        Log.d(TAG, "OCR 初始化成功（Google ML Kit）")
        return true
    }

    /**
     * 识别图片中的文字
     */
    fun recognize(bitmap: Bitmap): OcrResult {
        if (!initialized) {
            return OcrResult.error("OCR 未初始化")
        }

        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // 使用 CountDownLatch 等待异步结果
            val latch = java.util.concurrent.CountDownLatch(1)
            var result: OcrResult? = null
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    result = parseVisionText(visionText)
                    latch.countDown()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "OCR 识别失败", e)
                    result = OcrResult.error(e.message ?: "识别失败")
                    latch.countDown()
                }
            
            // 等待结果（最多 10 秒）
            if (latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                result ?: OcrResult.error("识别失败")
            } else {
                OcrResult.error("识别超时")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别失败", e)
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
            Log.e(TAG, "OCR 识别失败", e)
            OcrResult.error(e.message ?: "识别失败")
        }
    }

    /**
     * 解析 ML Kit 的识别结果
     */
    private fun parseVisionText(visionText: com.google.mlkit.vision.text.Text): OcrResult {
        val words = mutableListOf<OcrWord>()
        val fullText = StringBuilder()

        // 遍历所有文本块
        for (block in visionText.textBlocks) {
            // 遍历块中的每一行
            for (line in block.lines) {
                // 遍历行中的每个元素
                for (element in line.elements) {
                    val boundingBox = element.boundingBox
                    if (boundingBox != null) {
                        val word = OcrWord(
                            text = element.text,
                            confidence = element.confidence ?: 0.9f,
                            x = boundingBox.left,
                            y = boundingBox.top,
                            width = boundingBox.width(),
                            height = boundingBox.height()
                        )
                        words.add(word)
                        fullText.append(element.text).append(" ")
                    }
                }
                fullText.append("\n")
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