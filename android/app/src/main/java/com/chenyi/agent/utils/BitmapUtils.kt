package com.chenyi.agent.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Bitmap 优化工具
 * 
 * 职责：
 * - 管理 Bitmap 内存
 * - 提供压缩和转换功能
 * - 避免内存泄漏
 */
object BitmapUtils {
    
    /**
     * 安全的 Bitmap 压缩
     * 
     * @param bitmap 原始 Bitmap
     * @param quality 压缩质量 (0-100)
     * @return Base64 编码字符串
     */
    fun compressToBase64(bitmap: Bitmap, quality: Int = 100): String {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
            android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 安全的 Bitmap 释放
     * 
     * @param bitmap 要释放的 Bitmap
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
    
    /**
     * 计算图片采样大小
     * 
     * @param options BitmapFactory.Options
     * @param reqWidth 要求的宽度
     * @param reqHeight 要求的高度
     * @return 采样大小
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (width: Int, height: Int) = options.outWidth to options.outHeight
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 优化的图片加载
     * 
     * @param path 图片路径
     * @param reqWidth 要求的宽度
     * @param reqHeight 要求的高度
     * @return Bitmap 或 null
     */
    fun loadOptimizedBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)
            
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取 Bitmap 大小（字节）
     */
    fun getBitmapSize(bitmap: Bitmap): Int {
        return bitmap.getByteCount()
    }
    
    /**
     * 检查 Bitmap 是否过大
     * 
     * @param bitmap Bitmap
     * @param maxSize 最大大小（字节）
     * @return 是否过大
     */
    fun isBitmapTooLarge(bitmap: Bitmap, maxSize: Int = Constants.MAX_FILE_SIZE.toInt()): Boolean {
        return getBitmapSize(bitmap) > maxSize
    }
}