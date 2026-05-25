package com.chenyi.agent

import androidx.compose.ui.graphics.Color
import com.chenyi.agent.ui.theme.*

/**
 * Session 扩展函数 - 为 UI 显示计算所需字段
 */

/**
 * 获取会话预览文本（最后一条消息的内容）
 */
fun Session.getPreview(): String {
    return if (messages.isNotEmpty()) {
        messages.last().content.take(50) + if (messages.last().content.length > 50) "..." else ""
    } else {
        "暂无消息"
    }
}

/**
 * 获取头像文字（标题首字符）
 */
fun Session.getAvatarText(): String {
    return title.take(1).ifEmpty { "新" }
}

/**
 * 获取头像渐变色（基于 ID 哈希生成稳定颜色）
 */
fun Session.getAvatarGradient(): List<Color> {
    val gradients = listOf(
        GradientPrimary,
        listOf(AccentPurple, AccentPink),
        listOf(AccentGreen, Color(0xFF10B981)),
        listOf(AccentYellow, Color(0xFFF59E0B)),
        listOf(AccentRed, Color(0xFFEF4444)),
        listOf(AccentCyan, Color(0xFF06B6D4))
    )
    
    val index = Math.abs(id.hashCode()) % gradients.size
    return gradients[index]
}

/**
 * 获取最后消息时间
 */
fun Session.getLastMessageTime(): Long {
    return if (messages.isNotEmpty()) {
        messages.maxOf { it.timestamp }
    } else {
        updatedAt
    }
}

/**
 * 判断是否有新消息（简化实现：始终返回 false）
 * TODO: 实现真实的新消息判断逻辑
 */
fun Session.hasNewMessage(): Boolean {
    return false
}
