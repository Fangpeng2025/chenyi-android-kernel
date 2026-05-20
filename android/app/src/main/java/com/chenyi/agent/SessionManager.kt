package com.chenyi.agent

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * 会话管理器
 * 负责会话的持久化和管理
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSIONS_DIR = "sessions"
        private const val CURRENT_SESSION_FILE = "current_session.json"
    }
    
    private val sessionsDir = File(context.filesDir, SESSIONS_DIR).apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * 获取所有会话
     */
    fun getAllSessions(): List<Session> {
        return try {
            sessionsDir.listFiles()
                ?.filter { it.extension == "json" && it.name != CURRENT_SESSION_FILE }
                ?.map { file ->
                    Session.fromJson(file.readText())
                }
                ?.sortedByDescending { it.updatedAt }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "加载会话列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 创建新会话
     */
    fun createSession(title: String = "新会话"): Session {
        val session = Session(title = title)
        saveSession(session)
        Log.d(TAG, "创建新会话: ${session.id}")
        return session
    }
    
    /**
     * 保存会话
     */
    fun saveSession(session: Session) {
        try {
            val file = File(sessionsDir, "${session.id}.json")
            file.writeText(session.toJson().toString())
            Log.d(TAG, "保存会话: ${session.id}")
        } catch (e: Exception) {
            Log.e(TAG, "保存会话失败", e)
        }
    }
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String) {
        try {
            val file = File(sessionsDir, "$sessionId.json")
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "删除会话: $sessionId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除会话失败", e)
        }
    }
    
    /**
     * 获取会话
     */
    fun getSession(sessionId: String): Session? {
        return try {
            val file = File(sessionsDir, "$sessionId.json")
            if (file.exists()) {
                Session.fromJson(file.readText())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载会话失败", e)
            null
        }
    }
    
    /**
     * 设置当前会话
     */
    fun setCurrentSession(sessionId: String) {
        try {
            val file = File(sessionsDir, CURRENT_SESSION_FILE)
            file.writeText(sessionId)
            Log.d(TAG, "设置当前会话: $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "设置当前会话失败", e)
        }
    }
    
    /**
     * 获取当前会话 ID
     */
    fun getCurrentSessionId(): String? {
        return try {
            val file = File(sessionsDir, CURRENT_SESSION_FILE)
            if (file.exists()) {
                file.readText().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取当前会话ID失败", e)
            null
        }
    }
    
    /**
     * 获取或创建当前会话
     */
    fun getOrCreateCurrentSession(): Session {
        val currentId = getCurrentSessionId()
        
        // 尝试加载当前会话
        if (currentId != null) {
            val session = getSession(currentId)
            if (session != null) {
                return session
            }
        }
        
        // 创建新会话
        val newSession = createSession()
        setCurrentSession(newSession.id)
        return newSession
    }
    
    /**
     * 添加消息到当前会话
     */
    fun addMessageToSession(sessionId: String, message: Message): Session? {
        val session = getSession(sessionId) ?: return null
        val updatedSession = session.addMessage(message)
        saveSession(updatedSession)
        return updatedSession
    }
    
    /**
     * 清空所有会话
     */
    fun clearAllSessions() {
        try {
            sessionsDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "清空所有会话")
        } catch (e: Exception) {
            Log.e(TAG, "清空会话失败", e)
        }
    }
    
    /**
     * 导出会话
     */
    fun exportSession(sessionId: String): String? {
        return try {
            val session = getSession(sessionId) ?: return null
            session.toJson().toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "导出会话失败", e)
            null
        }
    }
}
