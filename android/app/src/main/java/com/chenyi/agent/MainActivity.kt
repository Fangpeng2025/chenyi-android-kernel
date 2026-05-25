package com.chenyi.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chenyi.agent.ui.components.*
import com.chenyi.agent.ui.theme.*

/**
 * 晨翼Agent 主 Activity
 * 
 * 特性：
 * - 使用 ChenyiAgentTheme 主题
 * - 粒子背景动画
 * - 现代化底部导航栏
 * - 5 个标签页切换
 * - 完全仿制 HTML 样式
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChenyiAgentTheme {
                MainAppContent()
            }
        }
    }
}

/**
 * 主应用内容组件
 */
@Composable
fun MainAppContent() {
    // 当前选中的标签页
    var selectedTab by remember { mutableStateOf(0) }

    // 聊天相关状态
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var tokenUsage by remember { mutableStateOf(0) }
    val maxTokens = 4096

    // 会话列表状态
    var sessions by remember { mutableStateOf(listOf<Session>()) }

    // 任务列表状态
    var tasks by remember { mutableStateOf(listOf<Task>()) }

    // 设置状态
    var apiKey by remember { mutableStateOf<String?>(null) }
    var apiEndpoint by remember { mutableStateOf("api.openai.com") }
    var modelName by remember { mutableStateOf("glm-5") }
    var compressionEnabled by remember { mutableStateOf(true) }
    var compressionThreshold by remember { mutableStateOf(4000) }
    var compressionRatio by remember { mutableStateOf(50) }
    var accessibilityEnabled by remember { mutableStateOf(true) }
    val appVersion = "v1.0.16"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 粒子背景
        ParticleBackgroundSimple(
            modifier = Modifier.fillMaxSize()
        )

        // 主内容
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标签页内容（占据剩余空间）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 聊天页面
                if (selectedTab == 0) {
                    ChatScreen(
                        messages = chatMessages,
                        currentTokenUsage = tokenUsage,
                        maxTokens = maxTokens,
                        onSendMessage = { message ->
                            // 添加用户消息
                            val userMessage = ChatMessage(
                                content = message,
                                isUser = true,
                                timestamp = System.currentTimeMillis()
                            )
                            chatMessages = chatMessages + userMessage

                            // 模拟 AI 回复
                            val aiMessage = ChatMessage(
                                content = "收到！我正在处理你的请求...",
                                isUser = false,
                                timestamp = System.currentTimeMillis(),
                                isStreaming = true
                            )
                            chatMessages = chatMessages + aiMessage

                            // 更新 token 使用量
                            tokenUsage = (tokenUsage + message.length).coerceAtMost(maxTokens)
                        }
                    )
                }

                // 会话列表页面
                if (selectedTab == 1) {
                    SessionListScreen(
                        sessions = sessions,
                        onSessionClick = { session ->
                            // TODO: 切换到该会话
                        },
                        onSearchQueryChange = { query ->
                            // TODO: 搜索会话
                        }
                    )
                }

                // 工具页面
                if (selectedTab == 2) {
                    ToolsScreen(
                        onToolClick = { tool ->
                            // TODO: 执行工具操作
                            when (tool.id) {
                                "screenshot" -> {
                                    // 调用 Kernel.screenshot()
                                }
                                "ocr" -> {
                                    // 调用 OcrEngine.recognize()
                                }
                                "click" -> {
                                    // 调用 Kernel.tap()
                                }
                                "swipe" -> {
                                    // 调用 Kernel.swipe()
                                }
                                "input" -> {
                                    // 调用 Kernel.input()
                                }
                                "open_app" -> {
                                    // 调用 Kernel.openApp()
                                }
                                "home" -> {
                                    // 调用 Kernel.home()
                                }
                                "current_app" -> {
                                    // 调用 Kernel.getCurrentApp()
                                }
                            }
                        }
                    )
                }

                // 任务页面
                if (selectedTab == 3) {
                    TaskScreen(
                        tasks = tasks,
                        onTaskClick = { task ->
                            // TODO: 显示任务详情
                        },
                        onCreateTask = { name, description, schedule ->
                            // 创建新任务
                            val newTask = Task(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                description = description,
                                status = TaskStatus.RUNNING,
                                schedule = schedule
                            )
                            tasks = tasks + newTask
                        },
                        onPauseTask = { task ->
                            // 切换任务状态
                            tasks = tasks.map {
                                if (it.id == task.id) {
                                    it.copy(
                                        status = if (it.status == TaskStatus.RUNNING) {
                                            TaskStatus.PAUSED
                                        } else {
                                            TaskStatus.RUNNING
                                        }
                                    )
                                } else it
                            }
                        },
                        onDeleteTask = { task ->
                            // 删除任务
                            tasks = tasks.filter { it.id != task.id }
                        }
                    )
                }

                // 设置页面
                if (selectedTab == 4) {
                    SettingsScreen(
                        apiKey = apiKey,
                        apiEndpoint = apiEndpoint,
                        modelName = modelName,
                        compressionEnabled = compressionEnabled,
                        compressionThreshold = compressionThreshold,
                        compressionRatio = compressionRatio,
                        accessibilityEnabled = accessibilityEnabled,
                        appVersion = appVersion,
                        onApiKeyClick = {
                            // TODO: 显示 API Key 输入对话框
                        },
                        onApiEndpointClick = {
                            // TODO: 显示 API Endpoint 输入对话框
                        },
                        onModelNameClick = {
                            // TODO: 显示模型选择对话框
                        },
                        onUserProfileClick = {
                            // TODO: 打开用户画像编辑页面
                        },
                        onCompressionToggle = { enabled ->
                            compressionEnabled = enabled
                        },
                        onCompressionThresholdChange = { threshold ->
                            compressionThreshold = threshold
                        },
                        onCompressionRatioChange = { ratio ->
                            compressionRatio = ratio
                        },
                        onAccessibilityClick = {
                            // TODO: 打开无障碍服务设置
                        },
                        onAboutClick = {
                            // TODO: 显示关于对话框
                        },
                        onCheckUpdate = {
                            // TODO: 检查更新
                        }
                    )
                }
            }

            // 底部导航栏
            ModernBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    selectedTab = newTab
                }
            )
        }
    }
}

// ==================== Extension Functions ====================

/**
 * 扩展函数：创建渐变文字样式
 */
@Composable
fun Modifier.gradientText(
    colors: List<Color> = GradientPrimary
): Modifier = this.then(
    Modifier.background(Brush.linearGradient(colors))
)
