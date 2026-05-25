package com.chenyi.agent

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chenyi.agent.ui.components.*
import com.chenyi.agent.ui.components.WeChatBottomBar
import com.chenyi.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 晨翼Agent 主 Activity
 * 
 * 功能：
 * - 微信风格3标签页布局（聊天、技能、设置）
 * - 聊天页面：流式消息、Token 使用进度
 * - 工具页面：20个自动化工具
 * - 设置页面：API配置、压缩配置、自动更新
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ChenyiAgentApp()
        }
    }
}

@Composable
fun ChenyiAgentApp() {
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
    val appVersion = "v1.0.19"
    
    ChenyiAgentTheme {
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
                                
                                // 添加 AI 响应（模拟）
                                val aiMessage = ChatMessage(
                                    content = "收到您的消息：$message",
                                    isUser = false,
                                    timestamp = System.currentTimeMillis()
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
                                Toast.makeText(context, "工具：${tool.name}", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "用户画像功能开发中", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "请前往系统设置开启无障碍服务", Toast.LENGTH_LONG).show()
                            },
                            onAboutClick = {
                                Toast.makeText(context, "晨翼Agent $appVersion", Toast.LENGTH_SHORT).show()
                            },
                            onCheckUpdate = {
                                if (isCheckingUpdate) return@SettingsScreen
                                
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
                            },
                            updateAvailable = updateAvailable,
                            isCheckingUpdate = isCheckingUpdate,
                            downloadProgress = downloadProgress,
                            versionInfo = versionInfo?.let { 
                                UpdateInfo(
                                    versionName = it.versionName,
                                    releaseNotes = it.releaseNotes,
                                    downloadUrl = it.downloadUrl
                                )
                            }
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
}