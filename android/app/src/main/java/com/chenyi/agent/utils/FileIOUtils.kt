package com.chenyi.agent.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 文件 IO 工具 - 异步文件操作
 * 
 * 职责：
 * - 异步读写文件
 * - 避免阻塞主线程
 * - 提供安全的文件操作
 */
object FileIOUtils {
    
    /**
     * 异步读取文件内容
     * 
     * @param path 文件路径
     * @return 文件内容或 null
     */
    suspend fun readTextAsync(path: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                File(path).readText()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 异步写入文件内容
     * 
     * @param path 文件路径
     * @param content 内容
     * @return 是否成功
     */
    suspend fun writeTextAsync(path: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(path).writeText(content)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 异步读取字节数组
     * 
     * @param path 文件路径
     * @return 字节数组或 null
     */
    suspend fun readBytesAsync(path: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                File(path).readBytes()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 异步写入字节数组
     * 
     * @param path 文件路径
     * @param bytes 字节数组
     * @return 是否成功
     */
    suspend fun writeBytesAsync(path: String, bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(path).writeBytes(bytes)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 异步复制文件
     * 
     * @param source 源文件
     * @param dest 目标文件
     * @return 是否成功
     */
    suspend fun copyAsync(source: String, dest: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                FileInputStream(source).use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 异步删除文件
     * 
     * @param path 文件路径
     * @return 是否成功
     */
    suspend fun deleteAsync(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(path).delete()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 异步检查文件是否存在
     * 
     * @param path 文件路径
     * @return 是否存在
     */
    suspend fun existsAsync(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            File(path).exists()
        }
    }
    
    /**
     * 异步获取文件大小
     * 
     * @param path 文件路径
     * @return 文件大小（字节）或 -1
     */
    suspend fun sizeAsync(path: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                File(path).length()
            } catch (e: Exception) {
                -1L
            }
        }
    }
    
    /**
     * 异步创建目录
     * 
     * @param path 目录路径
     * @return 是否成功
     */
    suspend fun mkdirsAsync(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                File(path).mkdirs()
            } catch (e: Exception) {
                false
            }
        }
    }
}