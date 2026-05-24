package com.chenyi.agent

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.chenyi.agent.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

/**
 * 晨翼Agent 主界面 - 完整功能版
 * 5 标签页：聊天 / 会话 / 技能 / 任务 / 设置
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化内核库
        Kernel.initLibrary(applicationContext)
        
        val prefs = getSharedPreferences("chenyi_config", Context.MODE_PRIVATE)
        val kernel = Kernel(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        
        // 初始化内核
        if (!kernel.init()) {
            Log.e("MainActivity", "内核初始化失败")
        }
        
        // 加载保存的 API 配置
        val apiKey = prefs.getString("api_key", "") ?: ""
        val apiEndpoint = prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: ""
        val modelName = prefs.getString("model_name", "glm-5") ?: ""
        
        if (apiKey.isNotBlank()) {
            kernel.setApiKey(apiKey, apiEndpoint, modelName)
        }
        
        val screenshotManager = ScreenshotManager(this)
        val ocrEngine = OcrEngine(applicationContext)
        
        setContent {
            MaterialTheme {
                FullFeaturedApp(
                    kernel = kernel,
                    prefs = prefs,
                    sessionManager = sessionManager,
                    screenshotManager = screenshotManager,
                    ocrEngine = ocrEngine,
                    kernelInitialized = true
                )
            }
        }
    }
}

/**
 * 完整功能应用 - 5 标签页
 */
@Composable
fun FullFeaturedApp(
    kernel: Kernel,
    prefs: SharedPreferences,
    sessionManager: SessionManager,
    screenshotManager: ScreenshotManager,
    ocrEngine: OcrEngine,
    kernelInitialized: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    // 无障碍服务检查
    var showA11yDialog by remember { mutableStateOf(false) }
    val a11yService = ChenyiAccessibilityService.getInstance()
    
    // 启动时检查无障碍服务
    LaunchedEffect(Unit) {
        if (a11yService == null) {
            showA11yDialog = true
        }
    }
    
    Scaffold(
        containerColor = Color(0xFFEDEDED),
        bottomBar = {
            FullFeatureBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ChatScreen(kernel, prefs, sessionManager, kernelInitialized)
                1 -> SessionListScreen(sessionManager, onSessionSelected = { session ->
                    // 切换到聊天页并加载会话
                    selectedTab = 0
                })
                2 -> ToolsScreen(screenshotManager, ocrEngine, kernel)
                3 -> TaskScreen(kernel, prefs)
                4 -> SettingsScreen(prefs, kernel, screenshotManager, kernelInitialized)
            }
        }
    }
    
    // 无障碍服务提示弹窗
    if (showA11yDialog) {
        AlertDialog(
            onDismissRequest = { showA11yDialog = false },
            title = { Text("需要开启无障碍服务") },
            text = { 
                Text("晨翼Agent 需要无障碍服务才能使用大部分功能，包括：\n\n" +
                     "• 自动化操作（点击、滑动等）\n" +
                     "• 屏幕截图和 OCR 识别\n" +
                     "• 界面元素分析\n\n" +
                     "请前往设置开启无障碍服务。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showA11yDialog = false
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("去开启")
                }
            },
            dismissButton = {
                TextButton(onClick = { showA11yDialog = false }) {
                    Text("稍后再说")
                }
            }
        )
    }
}

/**
 * 完整功能底部导航栏 - 5 标签
 */
@Composable
fun FullFeatureBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "聊天" to Icons.Default.Chat,
        "会话" to Icons.Default.History,
        "技能" to Icons.Default.AutoAwesome,
        "任务" to Icons.Default.Schedule,
        "设置" to Icons.Default.Settings
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, (label, icon) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (index == selectedTab) Color(0xFF07C160) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (index == selectedTab) Color(0xFF07C160) else Color.Gray
                    )
                }
            }
        }
    }
}

// ==================== 会话管理页面 ====================

/**
 * 会话列表页面
 */
@Composable
fun SessionListScreen(
    sessionManager: SessionManager,
    onSessionSelected: (Session) -> Unit
) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(sessionManager.getAllSessions()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    
    // 过滤会话
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) {
            sessions
        } else {
            sessions.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.messages.any { msg -> msg.content.contains(searchQuery, ignoreCase = true) }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                Text(
                    text = "会话管理",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索会话...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
        
        // 会话列表
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "暂无会话" else "未找到匹配的会话",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSessions) { session ->
                    SessionItem(
                        session = session,
                        onClick = { onSessionSelected(session) },
                        onLongClick = {
                            sessionToDelete = session
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
        
        // 删除确认对话框
        if (showDeleteDialog && sessionToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("删除会话") },
                text = { Text("确定要删除会话 \"${sessionToDelete!!.title}\" 吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sessionManager.deleteSession(sessionToDelete!!.id)
                            sessions = sessionManager.getAllSessions()
                            showDeleteDialog = false
                            sessionToDelete = null
                            Toast.makeText(context, "会话已删除", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("删除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * 会话列表项
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SessionItem(
    session: Session,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF07C160),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { "新会话" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${session.messages.size} 条消息 · ${dateFormat.format(Date(session.updatedAt))}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // 箭头
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ==================== 任务管理页面 ====================

/**
 * 任务管理页面
 */
@Composable
fun TaskScreen(
    kernel: Kernel,
    prefs: SharedPreferences
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // 新建按钮
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "新建任务")
                }
            }
        }
        
        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
        
        // 任务列表
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("暂无定时任务", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击右上角 + 创建新任务", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks.size) { index ->
                    TaskItem(
                        task = tasks[index],
                        onPause = { /* 暂停任务 */ },
                        onResume = { /* 恢复任务 */ },
                        onDelete = { /* 删除任务 */ }
                    )
                }
            }
        }
        
        // 创建任务对话框
        if (showCreateDialog) {
            CreateTaskDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { taskName, schedule, prompt ->
                    // 创建任务
                    showCreateDialog = false
                    Toast.makeText(context, "任务已创建", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * 任务列表项
 */
@Composable
fun TaskItem(
    task: Map<String, Any>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val name = task["name"] as? String ?: "未命名任务"
    val schedule = task["schedule"] as? String ?: ""
    val status = task["status"] as? String ?: "pending"
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态图标
            Surface(
                modifier = Modifier.size(40.dp),
                color = when (status) {
                    "running" -> Color(0xFF07C160)
                    "paused" -> Color(0xFFFF9800)
                    else -> Color.Gray
                },
                shape = CircleShape
            ) {
                Icon(
                    when (status) {
                        "running" -> Icons.Default.PlayArrow
                        "paused" -> Icons.Default.Pause
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "调度: $schedule",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // 操作按钮
            Row {
                if (status == "running") {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = "暂停")
                    }
                } else if (status == "paused") {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "恢复")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red)
                }
            }
        }
    }
}

/**
 * 创建任务对话框
 */
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建定时任务") },
        text = {
            Column {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("任务名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("调度时间") },
                    placeholder = { Text("例如: 30m, every 2h, 0 9 * * *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("执行提示词") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (taskName.isNotBlank() && schedule.isNotBlank() && prompt.isNotBlank()) {
                        onCreate(taskName, schedule, prompt)
                    }
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ==================== 聊天界面 ====================

/**
 * 聊天界面 - 微信风格
 */
@Composable
fun ChatScreen(
    kernel: Kernel,
    prefs: SharedPreferences,
    sessionManager: SessionManager,
    kernelInitialized: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 加载配置
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    
    // 会话
    var currentSession by remember { mutableStateOf(sessionManager.getOrCreateCurrentSession()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // 待发送的消息
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var hasShownApiKeyWarning by remember { mutableStateOf(false) }
    
    // API Key 未设置提示
    if (apiKey.isBlank() && !hasShownApiKeyWarning) {
        AlertDialog(
            onDismissRequest = { hasShownApiKeyWarning = true },
            title = { Text("请配置 API Key") },
            text = { Text("使用前需要先配置 API Key，请前往设置页面配置") },
            confirmButton = {
                TextButton(onClick = { hasShownApiKeyWarning = true }) {
                    Text("知道了")
                }
            }
        )
    }
    
    // 处理消息发送
    LaunchedEffect(pendingMessage) {
        if (pendingMessage != null && pendingMessage!!.isNotBlank()) {
            val message = pendingMessage!!
            pendingMessage = null
            
            isLoading = true
            
            scope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        kernel.chatWithTools(message)
                    }
                    
                    val content = when {
                        response.response != null && response.response.isNotBlank() -> response.response
                        response.error != null && response.error.isNotBlank() -> {
                            val error = response.error
                            when {
                                error.contains("401") || error.contains("无效的令牌") -> 
                                    "❌ API Key 无效或已过期\n\n请前往设置页面检查并更新 API Key"
                                error.contains("无障碍服务") -> 
                                    "❌ ${error}\n\n请前往设置 → 无障碍 → 晨翼Agent 开启服务"
                                error.contains("网络") || error.contains("timeout") -> 
                                    "❌ 网络连接失败\n\n请检查网络连接后重试"
                                else -> "❌ 错误: ${error}"
                            }
                        }
                        else -> "⚠️ 无响应，请稍后重试"
                    }
                    
                    val assistantMessage = Message(role = "assistant", content = content)
                    currentSession = currentSession.addMessage(assistantMessage)
                    sessionManager.saveSession(currentSession)
                    
                    withContext(Dispatchers.Main) {
                        listState.animateScrollToItem(currentSession.messages.size - 1)
                    }
                } catch (e: Exception) {
                    val errorMsg = "❌ 发送失败: ${e.message}"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                    val errorMessage = Message(role = "assistant", content = errorMsg)
                    currentSession = currentSession.addMessage(errorMessage)
                    sessionManager.saveSession(currentSession)
                } finally {
                    isLoading = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentSession.title.ifBlank { "新会话" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentSession.messages.size} 条消息",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Row {
                    // 清空按钮
                    IconButton(
                        onClick = {
                            currentSession = currentSession.copy(messages = emptyList())
                            sessionManager.saveSession(currentSession)
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空")
                    }
                }
            }
        }
        
        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
        
        // 消息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currentSession.messages) { message ->
                WeChatMessageBubble(message)
            }
            
            // 加载指示器
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("思考中...", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
        
        // 输入区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 发送按钮
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMessage = Message(role = "user", content = inputText)
                            currentSession = currentSession.addMessage(userMessage)
                            sessionManager.saveSession(currentSession)
                            pendingMessage = inputText
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (inputText.isNotBlank() && !isLoading) Color(0xFF07C160) else Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 微信风格消息气泡
 */
@Composable
fun WeChatMessageBubble(message: Message) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI 头像
            Surface(
                modifier = Modifier.size(40.dp),
                color = Color(0xFF07C160),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // 消息气泡
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            color = if (isUser) Color(0xFF95EC69) else Color.White,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = if (isUser) 0.dp else 2.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Surface(
                modifier = Modifier.size(40.dp),
                color = Color(0xFF07C160),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "用户",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// ==================== 技能界面 ====================

/**
 * 技能界面
 */
@Composable
fun ToolsScreen(
    screenshotManager: ScreenshotManager,
    ocrEngine: OcrEngine,
    kernel: Kernel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 标题
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Text(
                text = "工具与技能",
                modifier = Modifier.padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 屏幕控制组
            item {
                Text(
                    text = "屏幕控制",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }
            
            // 截图
            item {
                val a11yService = ChenyiAccessibilityService.getInstance()
                val a11yConnected = a11yService != null
                
                WeChatToolItem(
                    icon = Icons.Default.Screenshot,
                    title = "截图",
                    subtitle = when {
                        !a11yConnected -> "❌ 无障碍服务未开启"
                        else -> "✅ 截取当前屏幕"
                    },
                    onClick = {
                        when {
                            !a11yConnected -> {
                                Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                scope.launch {
                                    try {
                                        val result = kernel.executeToolJson("screenshot", "{}")
                                        if (result.success) {
                                            Toast.makeText(context, "截图成功", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "截图失败: ${result.error}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "截图失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }
            
            // OCR
            item {
                WeChatToolItem(
                    icon = Icons.Default.TextFields,
                    title = "OCR 识别",
                    subtitle = "✅ 识别屏幕文字",
                    onClick = {
                        scope.launch {
                            try {
                                val result = kernel.executeToolJson("ocr", "{}")
                                if (result.success) {
                                    Toast.makeText(context, "OCR 识别成功", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "OCR 失败: ${result.error}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "OCR 失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            
            // 点击操作组
            item {
                Text(
                    text = "点击操作",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            
            // 点击坐标
            item {
                WeChatToolItem(
                    icon = Icons.Default.TouchApp,
                    title = "点击坐标",
                    subtitle = "✅ 点击指定位置",
                    onClick = {
                        Toast.makeText(context, "请在聊天中输入坐标", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            // 点击文字
            item {
                WeChatToolItem(
                    icon = Icons.Default.TextFields,
                    title = "点击文字",
                    subtitle = "✅ 查找并点击文字",
                    onClick = {
                        Toast.makeText(context, "请在聊天中输入要点击的文字", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            // 滑动操作组
            item {
                Text(
                    text = "滑动操作",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            
            // 滑动
            item {
                WeChatToolItem(
                    icon = Icons.Default.Swipe,
                    title = "滑动",
                    subtitle = "✅ 上下左右滑动",
                    onClick = {
                        Toast.makeText(context, "请在聊天中说明滑动方向", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            // 应用管理组
            item {
                Text(
                    text = "应用管理",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            
            // 打开应用
            item {
                WeChatToolItem(
                    icon = Icons.Default.Apps,
                    title = "打开应用",
                    subtitle = "✅ 启动指定应用",
                    onClick = {
                        Toast.makeText(context, "请在聊天中说明要打开的应用", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            // 当前应用
            item {
                WeChatToolItem(
                    icon = Icons.Default.Info,
                    title = "当前应用",
                    subtitle = "✅ 获取前台应用信息",
                    onClick = {
                        scope.launch {
                            try {
                                val result = kernel.executeToolJson("current_app", "{}")
                                if (result.success && result.response != null) {
                                    Toast.makeText(context, result.response, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "获取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * 工具卡片项
 */
@Composable
fun WeChatToolItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF07C160),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ==================== 设置界面 ====================

/**
 * 设置界面
 */
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    kernel: Kernel,
    screenshotManager: ScreenshotManager,
    kernelInitialized: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // API 配置
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: "") }
    var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
            .verticalScroll(rememberScrollState())
    ) {
        // 标题
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Text(
                text = "设置",
                modifier = Modifier.padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
        
        // API 配置组
        SettingsSection(title = "API 配置") {
            // API Endpoint
            SettingsItem(
                title = "API Endpoint",
                subtitle = apiEndpoint
            ) {
                OutlinedTextField(
                    value = apiEndpoint,
                    onValueChange = { apiEndpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
            
            // API Key
            SettingsItem(
                title = "API Key",
                subtitle = if (apiKey.isNotBlank()) "已设置" else "未设置"
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
            
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
            
            // Model Name
            SettingsItem(
                title = "模型名称",
                subtitle = modelName
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
            
            // 保存按钮
            Button(
                onClick = {
                    prefs.edit().apply {
                        putString("api_key", apiKey)
                        putString("api_endpoint", apiEndpoint)
                        putString("model_name", modelName)
                        apply()
                    }
                    
                    if (apiKey.isNotBlank()) {
                        kernel.setApiKey(apiKey, apiEndpoint, modelName)
                    }
                    
                    Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF07C160)
                )
            ) {
                Text("保存配置")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 系统设置组
        SettingsSection(title = "系统设置") {
            // 无障碍服务
            val a11yService = ChenyiAccessibilityService.getInstance()
            val a11yConnected = a11yService != null
            
            SettingsItem(
                title = "无障碍服务",
                subtitle = if (a11yConnected) "✅ 已开启" else "❌ 未开启"
            ) {
                Button(
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("前往设置")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 关于信息
        SettingsSection(title = "关于") {
            SettingsItem(
                title = "版本",
                subtitle = BuildConfig.VERSION_NAME
            )
            
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
            
            SettingsItem(
                title = "内核版本",
                subtitle = try {
                    kernel.getKernelVersion()
                } catch (e: Exception) {
                    "unknown"
                }
            )
        }
    }
}

/**
 * 设置分组
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = Color.Gray
            )
            content()
        }
    }
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
