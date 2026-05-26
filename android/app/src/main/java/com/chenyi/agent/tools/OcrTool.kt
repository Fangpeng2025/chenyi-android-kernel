package com.chenyi.agent.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.chenyi.agent.OcrEngine
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * OCR 工具 - 处理文字识别相关操作
 * 
 * 职责：
 * - 执行 OCR 识别
 * - 处理图片编码
 * - 格式化 OCR 结果
 */
class OcrTool(
    private val ocrEngine: OcrEngine?
) {
    
    /**
     * 执行 OCR 识别
     * 
     * @param bitmap 图片
     * @return OCR 结果
     */
    fun recognize(bitmap: Bitmap): Result {
        if (ocrEngine == null) {
            return Result.error("OCR 引擎未初始化")
        }
        
        return try {
            val ocrResult = ocrEngine.recognize(bitmap)
            
            val result = JSONObject().apply {
                put("text", ocrResult.text)
                put("blocks", JSONObject().apply {
                    ocrResult.textBlocks.forEachIndexed { index, block ->
                        put("block_$index", JSONObject().apply {
                            put("text", block.text)
                            put("confidence", block.confidence)
                            put("box", JSONObject().apply {
                                put("left", block.box.left)
                                put("top", block.box.top)
                                put("right", block.box.right)
                                put("bottom", block.box.bottom)
                            })
                        })
                    }
                })
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.error("OCR 识别失败: ${e.message}")
        }
    }
    
    /**
     * 执行 OCR 识别（从 Base64）
     * 
     * @param base64 Base64 编码的图片
     * @return OCR 结果
     */
    fun recognizeFromBase64(base64: String): Result {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return Result.error("无法解码图片")
            
            recognize(bitmap)
        } catch (e: Exception) {
            Result.error("图片解码失败: ${e.message}")
        }
    }
    
    /**
     * 查找文本位置
     * 
     * @param bitmap 图片
     * @param searchText 搜索文本
     * @return 文本位置列表
     */
    fun findText(bitmap: Bitmap, searchText: String): Result {
        if (ocrEngine == null) {
            return Result.error("OCR 引擎未初始化")
        }
        
        return try {
            val ocrResult = ocrEngine.recognize(bitmap)
            
            val matches = mutableListOf<JSONObject>()
            ocrResult.textBlocks.forEach { block ->
                if (block.text.contains(searchText, ignoreCase = true)) {
                    matches.add(JSONObject().apply {
                        put("text", block.text)
                        put("confidence", block.confidence)
                        put("center_x", (block.box.left + block.box.right) / 2)
                        put("center_y", (block.box.top + block.box.bottom) / 2)
                        put("box", JSONObject().apply {
                            put("left", block.box.left)
                            put("top", block.box.top)
                            put("right", block.box.right)
                            put("bottom", block.box.bottom)
                        })
                    })
                }
            }
            
            Result.success(JSONObject().apply {
                put("matches", matches)
                put("count", matches.size)
            })
        } catch (e: Exception) {
            Result.error("查找文本失败: ${e.message}")
        }
    }
    
    /**
     * Bitmap 转 Base64
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 100): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    }
}