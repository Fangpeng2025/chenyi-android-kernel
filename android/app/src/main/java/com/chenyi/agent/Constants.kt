package com.chenyi.agent

/**
 * 应用常量定义
 * 
 * 集中管理所有魔法数字和硬编码值
 */
object Constants {
    
    // ========== Token 相关 ==========
    
    /** 最大 Token 数量 */
    const val MAX_TOKENS = 4096
    
    /** Token 压缩阈值 */
    const val COMPRESSION_THRESHOLD = 4000
    
    /** 默认压缩比例 (%) */
    const val DEFAULT_COMPRESSION_RATIO = 50
    
    // ========== 网络相关 ==========
    
    /** 网络请求超时时间 (ms) */
    const val NETWORK_TIMEOUT_MS = 120000L
    
    /** 连接超时时间 (ms) */
    const val CONNECT_TIMEOUT_MS = 30000L
    
    /** 读取超时时间 (ms) */
    const val READ_TIMEOUT_MS = 60000L
    
    // ========== UI 相关 ==========
    
    /** 动画时长 (ms) */
    const val ANIMATION_DURATION_MS = 300
    
    /** 粒子数量 */
    const val PARTICLE_COUNT = 50
    
    /** 语音按钮大小 (dp) */
    const val VOICE_BUTTON_SIZE = 80
    
    // ========== 文件相关 ==========
    
    /** 缓冲区大小 */
    const val BUFFER_SIZE = 8192
    
    /** 最大文件大小 (10MB) */
    const val MAX_FILE_SIZE = 10 * 1024 * 1024L
    
    // ========== 时间相关 ==========
    
    /** 点击间隔 (ms) - 防止快速点击 */
    const val CLICK_INTERVAL_MS = 500L
    
    /** 长按时间 (ms) */
    const val LONG_PRESS_DURATION_MS = 500L
    
    /** 滑动时间 (ms) */
    const val SWIPE_DURATION_MS = 300L
    
    /** 等待时间 (ms) */
    const val WAIT_DURATION_MS = 1000L
    
    // ========== 更新相关 ==========
    
    /** APK 文件名 */
    const val APK_FILE_NAME = "chenyi-agent-update.apk"
    
    /** 服务器基础 URL */
    const val BASE_URL = "https://oneapi.xintiandi.online/chenyi-agent"
    
    /** 版本信息 URL */
    const val VERSION_URL = "$BASE_URL/version.json"
    
    /** Release APK URL */
    const val APK_RELEASE_URL = "$BASE_URL/chenyi-agent-release.apk"
    
    /** Debug APK URL */
    const val APK_DEBUG_URL = "$BASE_URL/chenyi-agent-debug.apk"
}
