package com.chenyi.agent

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.chenyi.agent.ui.components.*
import com.chenyi.agent.ui.components.WeChatBottomBar
import com.chenyi.agent.ui.theme.*
import com.chenyi.agent.UpdateManager

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 更新管理器
    val updateManager = remember { UpdateManager(context) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var versionInfo by remember { mutableStateOf<UpdateManager.VersionInfo?>(null) }
    var downloadProgress by remember { mutableStateOf(0) }
    
    // 当前选中的标签页
    var selectedTab by remember { mutableStateOf(0) }

// 聊天相关状态
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var tokenUsage by remember { mutableStateOf(0) }
    val maxTokens = 4096
    
    // API 配置状态
    var apiKey by remember { mutableStateOf("") }
    var apiEndpoint by remember { mutableStateOf("https://api.openai.com/v1") }
    var modelName by remember { mutableStateOf("gpt-4") }
    
    // 压缩配置状态
    var compressionEnabled by remember { mutableStateOf(true) }
    var compressionThreshold by remember { mutableStateOf(100) }
    var compressionRatio by remember { mutableStateOf(70) }
    
    // 无障碍服务状态
    var accessibilityEnabled by remember { mutableStateOf(false) }
    
    // 应用版本
    val appVersion = "v1.0.18"
            isCheckingUpdate = true
            downloadProgress = 0
            
            try {
                val info = updateManager.checkForUpdate()
                
                if (info != null) {
                    versionInfo = info
                    updateAvailable = true
                    Toast.makeText(context, "发现新版本 v${info.versionName}", Toast.LENGTH_LONG).show()
                } else {
                    updateAvailable = false
                    Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            
            isCheckingUpdate = false
        }
    }
    
    // 下载并安装更新函数
    fun downloadAndInstall() {
        val info = versionInfo ?: return
        
        scope.launch {
            try {
                val apkPath = updateManager.downloadApk(info) { progress ->
                    downloadProgress = progress
                }
                
                downloadProgress = 100
                Toast.makeText(context, "下载完成，正在安装...", Toast.LENGTH_SHORT).show()
                
                // 安装 APK
                updateManager.installApk(apkPath)
            } catch (e: Exception) {
                Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                downloadProgress = 0
            }
        }
    }

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
                
                // 工具页面（技能）
                if (selectedTab == 1) {
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
                
                // 设置页面
                // 设置页面
                if (selectedTab == 2) {
                    SettingsScreen(
                        apiKey = apiKey,
                        apiEndpoint = apiEndpoint,
                        modelName = modelName,
                        compressionEnabled = compressionEnabled,
                        compressionThreshold = compressionThreshold,
                        compressionRatio = compressionRatio,
                        accessibilityEnabled = accessibilityEnabled,
                        appVersion = appVersion,
                        onApiKeyChange = { newKey ->
                            apiKey = newKey
                        },
                        onApiEndpointChange = { newEndpoint ->
                            apiEndpoint = newEndpoint
                        },
                        onModelNameChange = { newModel ->
                            modelName = newModel
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
                            // TODO: 跳转到无障碍服务设置
                        },
                        onAboutClick = {
                            // TODO: 显示关于对话框
                        },
                        },
                        onCheckUpdate = {
                            if (updateAvailable && versionInfo != null) {
                                // 已有新版本，直接下载
                                downloadAndInstall()
                            } else {
                                // 检查更新
                                checkForUpdate()
                            }
                        },
                        isCheckingUpdate = isCheckingUpdate,
                        updateAvailable = updateAvailable,
                        versionInfo = versionInfo,
                        downloadProgress = downloadProgress
                    )
                }
            }

            // 底部导航栏 - 微信风格3个标签
            WeChatBottomBar(
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
