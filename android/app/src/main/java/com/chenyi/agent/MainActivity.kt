package com.chenyi.agent

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主 Activity
 */
class MainActivity : ComponentActivity() {

    private lateinit var kernel: Kernel
    private lateinit var prefs: SharedPreferences
    private lateinit var screenshotManager: ScreenshotManager
    private lateinit var ocrEngine: OcrEngine
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("chenyi_config", Context.MODE_PRIVATE)

        // 初始化内核
        kernel = Kernel(this)
        val initialized = kernel.init()

        // 初始化截图管理器
        screenshotManager = ScreenshotManager(this)
        screenshotManager.init()
        
        // 初始化 OCR 引擎
        ocrEngine = OcrEngine(this)
        
        // 初始化会话管理器
        sessionManager = SessionManager(this)

        setContent {
            ChenYiTheme {
                MainScreen(
                    kernelInitialized = initialized,
                    kernel = kernel,
                    prefs = prefs,
                    screenshotManager = screenshotManager,
                    ocrEngine = ocrEngine,
                    sessionManager = sessionManager,
                    onRequestScreenshotPermission = {
                        screenshotManager.requestPermission(this)
                    },
                    onCheckAccessibility = {
                        checkAccessibilityService()
                    }
                )
            }
        }
    }
    
    /**
     * 检查无障碍服务是否开启
     */
    private fun checkAccessibilityService(): Boolean {
        val service = ChenyiAccessibilityService.getInstance()
        if (service == null) {
            // 引导用户开启无障碍服务
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show()
            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == ScreenshotManager.REQUEST_MEDIA_PROJECTION && data != null) {
            val success = screenshotManager.handlePermissionResult(resultCode, data)
            Toast.makeText(this, if (success) "截图权限已授权" else "截图权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::kernel.isInitialized) {
            kernel.destroy()
        }
        if (::screenshotManager.isInitialized) {
            screenshotManager.destroy()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    kernelInitialized: Boolean,
    kernel: Kernel,
    prefs: SharedPreferences,
    screenshotManager: ScreenshotManager,
    ocrEngine: OcrEngine,
    sessionManager: SessionManager,
    onRequestScreenshotPermission: () -> Unit,
    onCheckAccessibility: () -> Boolean
) {
    val context = LocalContext.current

    // 加载设置
    var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "glm-5") }
    
    // 会话管理
    var sessions by remember { mutableStateOf(sessionManager.getAllSessions()) }
    var currentSession by remember { mutableStateOf(sessionManager.getOrCreateCurrentSession()) }
    var showSessionList by remember { mutableStateOf(false) }
    
    // OCR 初始化状态
    var ocrInitialized by remember { mutableStateOf(false) }
    var ocrStatus by remember { mutableStateOf("初始化 OCR...") }

    // 启动时初始化 OCR
    LaunchedEffect(Unit) {
        ocrStatus = "正在下载 OCR 模型..."
        ocrInitialized = ocrEngine.init()
        ocrStatus = if (ocrInitialized) "OCR 就绪" else "OCR 初始化失败"
    }
    
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("内核已就绪") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 权限状态
    val accessibilityEnabled = remember { mutableStateOf(ChenyiAccessibilityService.getInstance() != null) }
    val screenshotGranted = remember { mutableStateOf(screenshotManager.isAuthorized()) }
    
    // 更新权限状态
    LaunchedEffect(Unit) {
        accessibilityEnabled.value = ChenyiAccessibilityService.getInstance() != null
        screenshotGranted.value = screenshotManager.isAuthorized()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentSession.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = if (kernelInitialized) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = MaterialTheme.shapes.small
                        ) {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // 权限状态指示器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(6.dp),
                            color = if (accessibilityEnabled.value) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = MaterialTheme.shapes.small
                        ) {}
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("无障碍", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(6.dp),
                            color = if (screenshotGranted.value) Color(0xFF4CAF50) else Color(0xFFFFA726),
                            shape = MaterialTheme.shapes.small
                        ) {}
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("截图", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { showSessionList = true }) {
                        Icon(Icons.Default.List, contentDescription = "会话列表")
                    }
                    IconButton(onClick = { showTools = true }) {
                        Icon(Icons.Default.Build, contentDescription = "工具")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") },
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val message = inputText
                                inputText = ""
                                
                                // 添加用户消息到会话
                                val userMessage = Message(role = "user", content = message)
                                currentSession = currentSession.addMessage(userMessage)
                                sessionManager.saveSession(currentSession)
                                
                                isLoading = true
                                status = "正在思考..."

                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            // 使用 chatWithTools 自动处理工具调用
                                            kernel.chatWithTools(message)
                                        }
                                        isLoading = false
                                        if (result.success) {
                                            val response = result.response ?: "无响应"
                                            // 添加助手消息到会话
                                            val assistantMessage = Message(role = "assistant", content = response)
                                            currentSession = currentSession.addMessage(assistantMessage)
                                            sessionManager.saveSession(currentSession)
                                            status = "内核已就绪"
                                        } else {
                                            status = "错误: ${result.error}"
                                            snackbarHostState.showSnackbar(result.error ?: "未知错误")
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        status = "错误: ${e.message}"
                                        snackbarHostState.showSnackbar("错误: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = kernelInitialized && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 状态栏
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (status.contains("错误")) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (status.contains("错误")) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (status.contains("错误")) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 消息列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSession.messages) { message ->
                    MessageCard(message)
                }

                if (currentSession.messages.isEmpty() && !isLoading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "开始对话",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "输入消息与晨翼 Agent 开始对话",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 设置对话框
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("设置", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = apiEndpoint,
                        onValueChange = { apiEndpoint = it },
                        label = { Text("API 端点") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("模型名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Divider()
                    Text("当前配置:", fontWeight = FontWeight.Bold)
                    Text("端点: $apiEndpoint", style = MaterialTheme.typography.bodySmall)
                    Text("模型: $modelName", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 保存到 SharedPreferences
                    prefs.edit()
                        .putString("api_endpoint", apiEndpoint)
                        .putString("api_key", apiKey)
                        .putString("model_name", modelName)
                        .apply()
                    
                    // 更新内核配置
                    val config = KernelConfig(
                        apiEndpoint = apiEndpoint,
                        apiKey = apiKey,
                        modelName = modelName
                    )
                    kernel.updateConfig(config)
                    
                    showSettings = false
                    status = "设置已保存并生效"
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 工具对话框
    if (showTools) {
        AlertDialog(
            onDismissRequest = { showTools = false },
            title = { Text("工具列表", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolItem("截图", "截取当前屏幕") {
                        if (screenshotManager.isAuthorized()) {
                            val path = screenshotManager.captureAndSave()
                            if (path != null) {
                                status = "截图已保存: $path"
                            } else {
                                status = "截图失败"
                            }
                        } else {
                            onRequestScreenshotPermission()
                        }
                        showTools = false
                    }
                    ToolItem("点击", "点击屏幕坐标") { /* TODO */ showTools = false }
                    ToolItem("滑动", "滑动屏幕") { /* TODO */ showTools = false }
                    ToolItem("OCR", "识别屏幕文字") { /* TODO */ showTools = false }
                    ToolItem("打开应用", "打开指定应用") { /* TODO */ showTools = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTools = false }) {
                    Text("关闭")
                }
            }
        )
    }
    
    // 会话列表对话框
    if (showSessionList) {
        AlertDialog(
            onDismissRequest = { showSessionList = false },
            title = { Text("会话列表", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 新建会话按钮
                    OutlinedButton(
                        onClick = {
                            val newSession = sessionManager.createSession()
                            sessionManager.setCurrentSession(newSession.id)
                            currentSession = newSession
                            sessions = sessionManager.getAllSessions()
                            showSessionList = false
                            status = "已创建新会话"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新建会话")
                    }
                    
                    Divider()
                    
                    // 会话列表
                    sessions.forEach { session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (session.id == currentSession.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = {
                                sessionManager.setCurrentSession(session.id)
                                currentSession = session
                                showSessionList = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${session.messages.size} 条消息 · ${formatTime(session.updatedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // 删除按钮
                                IconButton(
                                    onClick = {
                                        sessionManager.deleteSession(session.id)
                                        sessions = sessionManager.getAllSessions()
                                        
                                        // 如果删除的是当前会话，切换到其他会话
                                        if (session.id == currentSession.id) {
                                            val remaining = sessionManager.getAllSessions()
                                            if (remaining.isNotEmpty()) {
                                                currentSession = remaining.first()
                                                sessionManager.setCurrentSession(currentSession.id)
                                            } else {
                                                // 没有会话了，创建新的
                                                val newSession = sessionManager.createSession()
                                                sessionManager.setCurrentSession(newSession.id)
                                                currentSession = newSession
                                            }
                                        }
                                        status = "已删除会话"
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    
                    if (sessions.isEmpty()) {
                        Text(
                            text = "暂无会话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionList = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * 格式化时间
 */
fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> "${diff / 86400_000}天前"
        else -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
fun MessageCard(message: Message) {
    val isUser = message.role == "user"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isUser) "用户" else "助手",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 消息内容
            SelectionContainer {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 工具调用记录
            if (message.toolCalls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "工具执行记录",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                message.toolCalls.forEach { toolCall ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (toolCall.success) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (toolCall.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = toolCall.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (toolCall.result != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "→ ${toolCall.result.take(20)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 操作按钮
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // 复制按钮
                IconButton(
                    onClick = {
                        // TODO: 复制到剪贴板
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 删除按钮
                IconButton(
                    onClick = {
                        // TODO: 删除消息
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                // 重发按钮（仅用户消息）
                if (isUser) {
                    IconButton(
                        onClick = {
                            // TODO: 重发消息
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "重发",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolItem(name: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 使用 Session.kt 中定义的完整 Message 类
// Message 类已在 Session.kt 中定义，包含 id, role, content, timestamp, toolCalls 字段

// ChenYiTheme 已在 Theme.kt 中定义，使用 Material Design 3 完整主题
// 包含动态颜色、深色模式支持等高级功能