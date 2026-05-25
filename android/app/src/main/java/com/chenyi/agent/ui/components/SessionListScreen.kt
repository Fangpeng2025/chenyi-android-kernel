package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ==================== Data Models ====================

/**
 * 会话数据类
 * @param id 会话ID
 * @param title 会话标题
 * @param preview 预览文本
 * @param avatarText 头像文字
 * @param avatarGradient 头像渐变色
 * @param lastMessageTime 最后消息时间
 * @param hasNewMessage 是否有新消息
 */
data class Session(
    val id: String,
    val title: String,
    val preview: String,
    val avatarText: String,
    val avatarGradient: List<Color> = GradientPrimary,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val hasNewMessage: Boolean = false
)

// ==================== Main Session List Screen ====================

/**
 * 会话列表页面主组件
 *
 * 特性：
 * - 搜索框
 * - 会话卡片列表
 * - 头像 + 标题 + 预览 + 时间
 *
 * @param sessions 会话列表
 * @param onSessionClick 会话点击回调
 * @param modifier 修饰符
 */
@Composable
fun SessionListScreen(
    sessions: List<Session>,
    onSessionClick: (Session) -> Unit,
    modifier: Modifier = Modifier,
    onSearchQueryChange: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部标题栏
        SessionListHeader(sessionCount = sessions.size)

        // 搜索框
        SearchBar(
            query = searchQuery,
            onQueryChange = { 
                searchQuery = it
                onSearchQueryChange(it)
            }
        )

        // 会话列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionItem(
                    session = session,
                    onClick = { onSessionClick(session) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ==================== Session List Header ====================

/**
 * 会话列表标题栏
 */
@Composable
private fun SessionListHeader(
    sessionCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgSecondary,
                        BgSecondary.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = "会话管理",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(GradientPrimary)
                )
            )
            Text(
                text = "$sessionCount 个活跃会话",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

// ==================== Search Bar ====================

/**
 * 搜索框组件
 *
 * @param query 搜索关键词
 * @param onQueryChange 搜索关键词变化回调
 * @param modifier 修饰符
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = BgTertiary,
                shape = RoundedCornerShape(14.dp)
            )
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        drawRect(
                            brush = Brush.linearGradient(GradientPrimary),
                            size = this.size
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 搜索图标
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )

            // 输入框
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        color = TextPrimary
                    ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索会话...",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                        innerTextField()
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(AccentCyan)
                )
            }
        }
    }
}

// ==================== Session Item ====================

/**
 * 会话卡片组件
 *
 * @param session 会话数据
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun SessionItem(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.getDefault()) }

    val timeString = remember(session.lastMessageTime) {
        val now = System.currentTimeMillis()
        val diff = now - session.lastMessageTime
        when {
            diff < 24 * 60 * 60 * 1000 -> timeFormat.format(Date(session.lastMessageTime))
            diff < 7 * 24 * 60 * 60 * 1000 -> "昨天"
            else -> dateFormat.format(Date(session.lastMessageTime))
        }
    }

    // 动画效果
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = animatedAlpha }
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 左侧渐变线条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(48.dp)
                    .background(
                        brush = Brush.verticalGradient(session.avatarGradient),
                        shape = RoundedCornerShape(2.dp)
                    )
            ) {
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(session.avatarGradient),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.avatarText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 会话信息
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = session.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = session.preview,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 时间和状态
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    if (session.hasNewMessage) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentCyan, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}

// ==================== Preview ====================

@Composable
fun SessionListScreenPreview() {
    val sampleSessions = listOf(
        Session(
            id = "1",
            title = "截图与OCR识别",
            preview = "帮我截个图，然后识别一下屏幕上的文字",
            avatarText = "截",
            avatarGradient = GradientPrimary,
            lastMessageTime = System.currentTimeMillis() - 60000,
            hasNewMessage = true
        ),
        Session(
            id = "2",
            title = "设置API配置",
            preview = "API Key 和 Endpoint 的配置说明",
            avatarText = "设",
            avatarGradient = listOf(AccentPurple, AccentPink),
            lastMessageTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        ),
        Session(
            id = "3",
            title = "定时任务讨论",
            preview = "如何创建自动化的定时提醒任务",
            avatarText = "任",
            avatarGradient = listOf(AccentGreen, Color(0xFF10B981)),
            lastMessageTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000
        )
    )

    SessionListScreen(
        sessions = sampleSessions,
        onSessionClick = {}
    )
}
