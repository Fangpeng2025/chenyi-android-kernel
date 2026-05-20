package com.chenyi.agent

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * OCR 引擎 - 使用 RapidOCR（自动下载模型）
 */
class OcrEngine(private val context: Context) {

    companion object {
        private const val TAG = "OcrEngine"
        
        // 模型文件列表
        private val MODEL_FILES = listOf(
            "ch_PP-OCRv3_det_infer.onnx",
            "ch_ppocr_mobile_v2.0_cls_infer.onnx", 
            "ch_PP-OCRv3_rec_infer.onnx",
            "ppocr_keys_v1.txt"
        )
        
        // 模型下载地址（使用国内镜像）
        private const val MODEL_BASE_URL = "https://hf-mirror.com/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/"
        
        // 备用地址
        private const val MODEL_BACKUP_URL = "https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/"
    }

    private var initialized = false
    private var rapidOcr: com.benjaminwan.ocrlibrary.OcrEngine? = null

    /**
     * 初始化 OCR（同步版本，用于非协程环境）
     */
    fun initSync(): Boolean {
        return try {
            // 检查模型文件
            val modelsDir = File(context.filesDir, "ocr_models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            // 下载缺失的模型文件（同步下载）
            for (modelFile in MODEL_FILES) {
                val file = File(modelsDir, modelFile)
                if (!file.exists()) {
                    Log.i(TAG, "下载模型: $modelFile")
                    if (!downloadModelSync(modelFile, file)) {
                        Log.e(TAG, "模型下载失败: $modelFile")
                        return false
                    }
                }
            }
            
            // 初始化 RapidOCR
            rapidOcr = com.benjaminwan.ocrlibrary.OcrEngine(context)
            
            // 设置参数
            rapidOcr?.padding = 50
            rapidOcr?.boxScoreThresh = 0.5f
            rapidOcr?.boxThresh = 0.3f
            rapidOcr?.unClipRatio = 1.6f
            rapidOcr?.doAngle = true
            rapidOcr?.mostAngle = true
            
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
     * 同步下载模型文件
     */
    private fun downloadModelSync(fileName: String, destFile: File): Boolean {
        return try {
            val url1 = "$MODEL_BASE_URL$fileName"
            if (downloadFile(url1, destFile)) {
                Log.i(TAG, "模型下载成功（主地址）: $fileName")
                return true
            }
            
            val url2 = "$MODEL_BACKUP_URL$fileName"
            if (downloadFile(url2, destFile)) {
                Log.i(TAG, "模型下载成功（备用地址）: $fileName")
                return true
            }
            
            Log.e(TAG, "模型下载失败: $fileName")
            false
        } catch (e: Exception) {
            Log.e(TAG, "下载模型异常: $fileName - ${e.message}")
            false
        }
    }

    /**
     * 初始化 OCR（自动下载模型）
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 检查模型文件
            val modelsDir = File(context.filesDir, "ocr_models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            
            // 2. 下载缺失的模型文件
            for (modelFile in MODEL_FILES) {
                val file = File(modelsDir, modelFile)
                if (!file.exists()) {
                    Log.i(TAG, "下载模型: $modelFile")
                    if (!downloadModel(modelFile, file)) {
                        Log.e(TAG, "模型下载失败: $modelFile")
                        return@withContext false
                    }
                }
            }
            
            // 3. 初始化 RapidOCR
            rapidOcr = com.benjaminwan.ocrlibrary.OcrEngine(context)
            
            // 设置参数
            rapidOcr?.padding = 50
            rapidOcr?.boxScoreThresh = 0.5f
            rapidOcr?.boxThresh = 0.3f
            rapidOcr?.unClipRatio = 1.6f
            rapidOcr?.doAngle = true
            rapidOcr?.mostAngle = true
            
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
     * 下载模型文件
     */
    private fun downloadModel(fileName: String, destFile: File): Boolean {
        return try {
            // 尝试主地址
            val url1 = "$MODEL_BASE_URL$fileName"
            if (downloadFile(url1, destFile)) {
                Log.i(TAG, "模型下载成功（主地址）: $fileName")
                return true
            }
            
            // 尝试备用地址
            val url2 = "$MODEL_BACKUP_URL$fileName"
            if (downloadFile(url2, destFile)) {
                Log.i(TAG, "模型下载成功（备用地址）: $fileName")
                return true
            }
            
            Log.e(TAG, "模型下载失败: $fileName")
            false
        } catch (e: Exception) {
            Log.e(TAG, "下载模型异常: $fileName - ${e.message}")
            false
        }
    }

    /**
     * 下载文件
     */
    private fun downloadFile(urlStr: String, destFile: File): Boolean {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection()
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            
            val input = conn.getInputStream()
            java.io.FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            input.close()
            
            true
        } catch (e: Exception) {
            Log.w(TAG, "下载失败: $urlStr - ${e.message}")
            destFile.delete()
            false
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized

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
    private fun parseRapidOcrResult(result: com.benjaminwan.ocrlibrary.OcrResult?): OcrResult {
        if (result == null) {
            return OcrResult.error("识别结果为空")
        }

        val words = mutableListOf<OcrWord>()
        val fullText = StringBuilder()

        for (textBlock in result.textBlocks) {
            val boxPoints = textBlock.boxPoint
            if (boxPoints.isNotEmpty()) {
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