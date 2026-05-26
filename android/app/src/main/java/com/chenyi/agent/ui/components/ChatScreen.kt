package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ==================== Data Models ====================

/**
 * 聊天消息数据类
 * @param id 消息ID
 * @param content 消息内容
 * @param isUser 是否是用户消息
 * @param timestamp 时间戳
 * @param isStreaming 是否正在流式输出
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

// ==================== Main Chat Screen ====================

/**
 * 聊天页面主组件
 *
 * 特性：
 * - Token 使用进度条
 * - 消息列表（用户/AI 不同样式）
 * - 输入框 + 发送按钮
 * - 流式消息支持
 *
 * @param messages 消息列表
 * @param currentTokenUsage 当前 Token 使用量
 * @param maxTokens 最大 Token 数
 * @param onSendMessage 发送消息回调
 * @param modifier 修饰符
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentTokenUsage: Int,
    maxTokens: Int,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部标题栏
        ChatHeader()

        // Token 使用进度条
        TokenUsageProgressBar(
            currentUsage = currentTokenUsage,
            maxUsage = maxTokens
        )

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.isStreaming) {
                    StreamingMessageBubble(message = message)
                } else {
                    MessageBubble(message = message)
                }
            }
        }

        // 输入区域
        ChatInputArea(onSendMessage = onSendMessage)
    }
}

// ==================== Chat Header ====================

/**
 * 聊天页面标题栏
 */
@Composable
private fun ChatHeader() {
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
                text = "晨翼Agent",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(GradientPrimary)
                )
            )
            Text(
                text = "v1.0.20 • Kernel Ready",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

// ==================== Token Usage Progress Bar ====================

/**
 * Token 使用进度条组件
 *
 * @param currentUsage 当前使用量
 * @param maxUsage 最大使用量
 */
@Composable
fun TokenUsageProgressBar(
    currentUsage: Int,
    maxUsage: Int,
    modifier: Modifier = Modifier
) {
    val percentage = (currentUsage.toFloat() / maxUsage.toFloat()).coerceIn(0f, 1f)
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
        label = "tokenProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                BgGlass.copy(alpha = 0.85f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            // 标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Token Usage",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Text(
                    text = "${String.format("%,d", currentUsage)} / ${String.format("%,d", maxUsage)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 进度条背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = BgTertiary,
                        shape = RoundedCornerShape(2.dp)
                    )
            ) {
                // 进度条填充
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPercentage)
                        .background(
                            brush = Brush.linearGradient(GradientPrimary),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .drawBehind {
                            // Glow 效果
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = GradientPrimary.map { it.copy(alpha = 0.3f) }
                                ),
                                size = this.size
                            )
                        }
                )
            }
        }
    }
}

// ==================== Message Bubble ====================

/**
 * 消息气泡组件
 *
 * @param message 消息数据
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    // 动画效果
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    val animatedOffset by animateIntAsState(
        targetValue = 0,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isUser) 48.dp else 0.dp,
                end = if (message.isUser) 0.dp else 48.dp
            ),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    alpha = animatedAlpha
                    translationY = animatedOffset.toFloat()
                }
                .background(
                    brush = if (message.isUser) {
                        Brush.linearGradient(GradientPrimary)
                    } else {
                        Brush.linearGradient(listOf(BgCard, BgCard))
                    },
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (message.isUser) Color.White else TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (message.isUser) Color.White.copy(alpha = 0.75f) else TextMuted
                )
            }
        }
    }
}

// ==================== Streaming Message Bubble ====================

/**
 * 流式消息气泡组件
 * 显示正在输入的 AI 消息，带有加载动画
 *
 * @param message 消息数据
 */
@Composable
fun StreamingMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    // 加载动画
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dotAlpha0 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot0"
    )
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = BgCard,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 20.dp
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column {
                if (message.content.isNotEmpty()) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 加载点
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentCyan.copy(alpha = dotAlpha0), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentCyan.copy(alpha = dotAlpha1), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentCyan.copy(alpha = dotAlpha2), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

// ==================== Chat Input Area ====================

/**
 * 聊天输入区域组件
 *
 * @param onSendMessage 发送消息回调
 */
@Composable
fun ChatInputArea(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgSecondary,
                        BgSecondary.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp)
                    .background(
                        color = BgTertiary,
                        shape = RoundedCornerShape(20.dp)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        color = TextPrimary
                    ),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = "输入消息...",
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                        innerTextField()
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank()) {
                                onSendMessage(text.trim())
                                text = ""
                            }
                        }
                    ),
                    singleLine = false,
                    maxLines = 4,
                    cursorBrush = SolidColor(AccentCyan),
                    onTextLayout = { /* Handle layout changes if needed */ }
                )
            }

            // 发送按钮
            FloatingActionButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text.trim())
                        text = ""
                    }
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(GradientPrimary),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==================== Preview ====================

@Composable
fun ChatScreenPreview() {
    val sampleMessages = listOf(
        ChatMessage(
            content = "你好！我是晨翼Agent，你的AI助手。我可以帮你截图、OCR识别、控制手机操作，或者只是聊聊天。有什么我可以帮你的吗？",
            isUser = false,
            timestamp = System.currentTimeMillis() - 60000
        ),
        ChatMessage(
            content = "帮我截个图，然后识别一下屏幕上的文字",
            isUser = true,
            timestamp = System.currentTimeMillis() - 30000
        ),
        ChatMessage(
            content = "好的，正在执行截图和OCR识别...",
            isUser = false,
            isStreaming = true,
            timestamp = System.currentTimeMillis()
        )
    )

    ChatScreen(
        messages = sampleMessages,
        currentTokenUsage = 2847,
        maxTokens = 4096,
        onSendMessage = {}
    )
}
