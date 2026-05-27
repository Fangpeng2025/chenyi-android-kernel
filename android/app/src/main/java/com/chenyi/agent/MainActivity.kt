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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chenyi.agent.data.ConfigManager
import com.chenyi.agent.data.ConfigValidator
import com.chenyi.agent.data.UserProfile
import com.chenyi.agent.ui.components.*
import com.chenyi.agent.ui.components.TaskStatus
import com.chenyi.agent.ui.theme.*
import com.chenyi.agent.viewmodel.MainViewModel
import com.chenyi.agent.viewmodel.ChatViewModel
import com.chenyi.agent.viewmodel.UpdateViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 晨翼Agent 主 Activity
 * 
 * 特性：
 * - 使用 ChenyiAgentTheme 主题
 * - 粒子背景动画
 * - 现代化底部导航栏
 * - 3 个标签页切换（聊天/技能/设置）
 * - MVVM 架构（ViewModel 管理状态）
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
 * 主应用内容组件 - 使用 ViewModel 管理状态
 */
@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ViewModel 管理所有状态
    val configManager = remember { ConfigManager(context) }
    val mainViewModel: MainViewModel = viewModel {
        MainViewModel(configManager)
    }
    val chatViewModel: ChatViewModel = viewModel()
    val updateViewModel: UpdateViewModel = viewModel {
        UpdateViewModel(context, UpdateManager(context))
    }
    
    // 从 MainViewModel 收集配置状态
    val apiKey by mainViewModel.apiKey.collectAsState()
    val apiEndpoint by mainViewModel.apiEndpoint.collectAsState()
    val modelName by mainViewModel.modelName.collectAsState()
    val userProfile by mainViewModel.userProfile.collectAsState()
    
    // 从 MainViewModel 收集对话框状态
    val showApiKeyDialog by mainViewModel.showApiKeyDialog.collectAsState()
    val showApiEndpointDialog by mainViewModel.showApiEndpointDialog.collectAsState()
    val showModelNameDialog by mainViewModel.showModelNameDialog.collectAsState()
    val showUserProfileDialog by mainViewModel.showUserProfileDialog.collectAsState()
    val showAboutDialog by mainViewModel.showAboutDialog.collectAsState()
    
    // 从 ChatViewModel 收集聊天状态
    val chatMessages by chatViewModel.messages.collectAsState()
    val tokenUsage by chatViewModel.tokenUsage.collectAsState()
    
    // 从 UpdateViewModel 收集更新状态
    val isCheckingUpdate by updateViewModel.isCheckingUpdate.collectAsState()
    val updateAvailable by updateViewModel.updateAvailable.collectAsState()
    val versionInfo by updateViewModel.versionInfo.collectAsState()
    val downloadProgress by updateViewModel.downloadProgress.collectAsState()
    val appVersion by updateViewModel.currentVersion.collectAsState()
    
    // 配置验证器
    val configValidator = remember { ConfigValidator(context) }
    
    // 当前选中的标签页
    var selectedTab by remember { mutableStateOf(0) }
    
    // 压缩配置（暂时保留在本地，后续迁移到 ViewModel）
    var compressionEnabled by remember { mutableStateOf(true) }
    var compressionThreshold by remember { mutableStateOf(4000) }
    var compressionRatio by remember { mutableStateOf(50) }
    var accessibilityEnabled by remember { mutableStateOf(true) }

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
                        maxTokens = ChatViewModel.MAX_TOKENS,
                        onSendMessage = { message ->
                            chatViewModel.sendMessage(message)
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
                            mainViewModel.showApiKeyDialog()
                        },
                        onApiEndpointClick = {
                            mainViewModel.showApiEndpointDialog()
                        },
                        onModelNameClick = {
                            mainViewModel.showModelNameDialog()
                        },
                        onUserProfileClick = {
                            mainViewModel.showUserProfileDialog()
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
                            mainViewModel.showAboutDialog()
                        },
onCheckUpdate = {
                            updateViewModel.checkOrDownload()
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

        // 对话框层（必须在 Box 内部）
        if (showApiKeyDialog) {
            ApiKeyDialog(
                currentValue = apiKey,
                onDismiss = { mainViewModel.hideApiKeyDialog() },
                onConfirm = { newKey ->
                    mainViewModel.saveApiKey(newKey)
                    Toast.makeText(context, "API Key 已保存", Toast.LENGTH_SHORT).show()
                },
                validator = configValidator
            )
        }

        if (showApiEndpointDialog) {
            ApiEndpointDialog(
                currentValue = apiEndpoint,
                onDismiss = { mainViewModel.hideApiEndpointDialog() },
                onConfirm = { newEndpoint ->
                    mainViewModel.saveApiEndpoint(newEndpoint)
                    Toast.makeText(context, "API Endpoint 已保存", Toast.LENGTH_SHORT).show()
                },
                validator = configValidator
            )
        }

        if (showModelNameDialog) {
            ModelNameDialog(
                currentValue = modelName,
                onDismiss = { mainViewModel.hideModelNameDialog() },
                onConfirm = { newModel ->
                    mainViewModel.saveModelName(newModel)
                    Toast.makeText(context, "模型已切换为 $newModel", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showUserProfileDialog) {
            UserProfileDialog(
                initialProfile = userProfile,
                onDismiss = { mainViewModel.hideUserProfileDialog() },
                onSave = { profile ->
                    mainViewModel.saveUserProfile(profile)
                    Toast.makeText(context, "用户画像已保存", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                appVersion = appVersion,
                onDismiss = { mainViewModel.hideAboutDialog() }
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
