package com.chenyi.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.chenyi.agent.data.ConfigManager
import com.chenyi.agent.data.ConfigValidator
import com.chenyi.agent.data.UserProfile
import com.chenyi.agent.ui.components.*
import com.chenyi.agent.ui.components.TaskStatus
import com.chenyi.agent.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    
    // 配置管理器
    val configManager = remember { ConfigManager(context) }
    val configValidator = remember { ConfigValidator(context) }
    
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
    
    // 设置状态（从 DataStore 加载）
    var apiKey by remember { mutableStateOf<String?>(null) }
    var apiEndpoint by remember { mutableStateOf("api.openai.com") }
    var modelName by remember { mutableStateOf("glm-5") }
    var compressionEnabled by remember { mutableStateOf(true) }
    var compressionThreshold by remember { mutableStateOf(4000) }
    var compressionRatio by remember { mutableStateOf(50) }
    var accessibilityEnabled by remember { mutableStateOf(true) }
    val appVersion = updateManager.getCurrentVersionName()
    
    // 对话框状态
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiEndpointDialog by remember { mutableStateOf(false) }
    var showModelNameDialog by remember { mutableStateOf(false) }
    var showUserProfileDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    
    // 加载已保存的配置
    LaunchedEffect(Unit) {
        // 加载 API 配置
        apiKey = configManager.getApiKey().first()
        apiEndpoint = configManager.getApiEndpoint().first()
        modelName = configManager.getModelName().first()
        
        // 加载压缩配置
        compressionEnabled = configManager.getCompressionEnabled().first()
        compressionThreshold = configManager.getCompressionThreshold().first()
        compressionRatio = configManager.getCompressionRatio().first()
        
        // 加载用户画像
        userProfile = configManager.getUserProfile().first()
    }
    
    // 检查更新函数
    fun checkForUpdate() {
        if (isCheckingUpdate) return
        
        scope.launch {
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
                
                // 工具页面
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
onApiKeyClick = {
                            showApiKeyDialog = true
                        },
                        onApiEndpointClick = {
                            showApiEndpointDialog = true
                        },
                        onModelNameClick = {
                            showModelNameDialog = true
                        },
                        onUserProfileClick = {
                            showUserProfileDialog = true
                        },
onCompressionToggle = { enabled ->
                            compressionEnabled = enabled
                            scope.launch {
                                configManager.saveCompressionEnabled(enabled)
                            }
                        },
                        onCompressionThresholdChange = { threshold ->
                            compressionThreshold = threshold
                            scope.launch {
                                configManager.saveCompressionThreshold(threshold)
                            }
                        },
                        onCompressionRatioChange = { ratio ->
                            compressionRatio = ratio
                            scope.launch {
                                configManager.saveCompressionRatio(ratio)
                            }
                        },
                        onAccessibilityClick = {
                            // 跳转到系统无障碍服务设置
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开无障碍服务设置", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAboutClick = {
                            showAboutDialog = true
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

// 底部导航栏
            ModernBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    selectedTab = newTab
                }
            )
        }
    }

    // 对话框层
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentValue = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { newKey ->
                apiKey = newKey
                showApiKeyDialog = false
                scope.launch {
                    configManager.saveApiKey(newKey)
                }
                Toast.makeText(context, "API Key 已保存", Toast.LENGTH_SHORT).show()
            },
            validator = configValidator
        )
    }

    if (showApiEndpointDialog) {
        ApiEndpointDialog(
            currentValue = apiEndpoint,
            onDismiss = { showApiEndpointDialog = false },
            onConfirm = { newEndpoint ->
                apiEndpoint = newEndpoint
                showApiEndpointDialog = false
                scope.launch {
                    configManager.saveApiEndpoint(newEndpoint)
                }
                Toast.makeText(context, "API Endpoint 已保存", Toast.LENGTH_SHORT).show()
            },
            validator = configValidator
        )
    }

    if (showModelNameDialog) {
        ModelNameDialog(
            currentValue = modelName,
            onDismiss = { showModelNameDialog = false },
            onConfirm = { newModel ->
                modelName = newModel
                showModelNameDialog = false
                scope.launch {
                    configManager.saveModelName(newModel)
                }
                Toast.makeText(context, "模型已切换为 $newModel", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showUserProfileDialog) {
        UserProfileDialog(
            initialProfile = userProfile,
            onDismiss = { showUserProfileDialog = false },
            onSave = { profile ->
                userProfile = profile
                showUserProfileDialog = false
                scope.launch {
                    configManager.saveUserProfile(profile)
                }
                Toast.makeText(context, "用户画像已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            appVersion = appVersion,
            onDismiss = { showAboutDialog = false }
        )
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
