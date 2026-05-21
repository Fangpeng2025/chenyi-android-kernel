package com.chenyi.agent

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 截图辅助接口 - 统一处理 Android 14+ 和旧版本的差异
 */
interface ScreenshotHelper {
    fun isAuthorized(): Boolean
    fun requestPermission(activity: ComponentActivity)
    fun capture(): android.graphics.Bitmap?
}

/**
 * 微信风格主界面
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
        kernel = Kernel(this)
        val initialized = kernel.init()
        screenshotManager = ScreenshotManager(this)
        screenshotManager.init()
        ocrEngine = OcrEngine(this)
        sessionManager = SessionManager(this)

        // 注意：Android 14+ 不允许从后台启动前台服务
        // 截图服务将在用户点击截图按钮时启动（此时应用在前台）

        setContent {
            MaterialTheme {
                WeChatStyleApp(
                    kernelInitialized = initialized,
                    kernel = kernel,
                    prefs = prefs,
                    screenshotManager = screenshotManager,
                    ocrEngine = ocrEngine,
                    sessionManager = sessionManager
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ScreenshotManager.REQUEST_MEDIA_PROJECTION && data != null) {
            // Android 14+ 使用 ScreenshotService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val service = ScreenshotService.instance
                if (service != null) {
                    val success = service.setMediaProjection(resultCode, data)
                    Toast.makeText(this, if (success) "截图权限已授权" else "截图权限被拒绝", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "截图服务未启动", Toast.LENGTH_SHORT).show()
                }
            } else {
                val success = screenshotManager.handlePermissionResult(resultCode, data)
                Toast.makeText(this, if (success) "截图权限已授权" else "截图权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::kernel.isInitialized) kernel.destroy()
        if (::screenshotManager.isInitialized) screenshotManager.destroy()
        
        // 停止截图服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            stopService(Intent(this, ScreenshotService::class.java))
        }
    }
}

/**
 * 微信风格主应用
 */
@Composable
fun WeChatStyleApp(
    kernelInitialized: Boolean,
    kernel: Kernel,
    prefs: SharedPreferences,
    screenshotManager: ScreenshotManager,
    ocrEngine: OcrEngine,
    sessionManager: SessionManager
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // 截图辅助对象 - 根据 Android 版本选择使用 ScreenshotService 或 ScreenshotManager
    val screenshotHelper = remember {
        object : ScreenshotHelper {
            override fun isAuthorized(): Boolean {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ScreenshotService.instance?.hasProjection() ?: false
                } else {
                    screenshotManager.isAuthorized()
                }
            }

            override fun requestPermission(activity: ComponentActivity) {
                // Android 14+ 需要先启动前台服务（应用在前台时）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val serviceIntent = Intent(activity, ScreenshotService::class.java)
                    activity.startForegroundService(serviceIntent)
                }
                screenshotManager.requestPermission(activity)
            }

            override fun capture(): android.graphics.Bitmap? {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ScreenshotService.instance?.capture()
                } else {
                    screenshotManager.capture()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFEDEDED),
        bottomBar = {
            WeChatBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ChatScreen(kernel, prefs, sessionManager, kernelInitialized)
                1 -> ToolsScreen(screenshotManager, ocrEngine, kernel, screenshotHelper)
                2 -> SettingsScreen(prefs, kernel, screenshotManager, screenshotHelper)
                3 -> ProfileScreen(kernelInitialized, screenshotManager, screenshotHelper)
            }
        }
    }
}

/**
 * 微信底部导航栏
 */
@Composable
fun WeChatBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "聊天" to Icons.Default.Chat,
        "工具" to Icons.Default.Build,
        "设置" to Icons.Default.Settings,
        "我" to Icons.Default.Person
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
                        color = if (index == selectedTab) Color(0xFF07C160) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

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
    var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: "") }
    var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "") }

    // 会话
    var currentSession by remember { mutableStateOf(sessionManager.getOrCreateCurrentSession()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // API Key 未设置提示 - 只在首次进入聊天页面时显示
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 标题栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSession.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                // 状态指示
                Surface(
                    modifier = Modifier.size(8.dp),
                    color = if (kernelInitialized) Color(0xFF07C160) else Color.Red,
                    shape = CircleShape
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (kernelInitialized) "就绪" else "未连接",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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
        }

        HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)

        // 输入区域
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 输入框
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp),
                    placeholder = { Text("输入消息", color = Color.Gray) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF07C160),
                        unfocusedBorderColor = Color(0xFFE5E5E5)
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 发送按钮
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading && apiKey.isNotBlank()) {
                            val message = inputText
                            inputText = ""

                            // 添加用户消息
                            val userMessage = Message(role = "user", content = message)
                            currentSession = currentSession.addMessage(userMessage)
                            sessionManager.saveSession(currentSession)
                            isLoading = true

                            scope.launch {
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        // 使用 chatWithTools 自动处理工具调用
                                        kernel.chatWithTools(message)
                                    }
                                    // 更清晰的错误显示
                                    val content = when {
                                        response.response != null && response.response.isNotBlank() -> response.response
                                        response.error != null && response.error.isNotBlank() -> "❌ 错误: ${response.error}"
                                        else -> "⚠️ 无响应"
                                    }
                                    val assistantMessage = Message(role = "assistant", content = content)
                                    currentSession = currentSession.addMessage(assistantMessage)
                                    sessionManager.saveSession(currentSession)
                                } catch (e: Exception) {
                                    // 显示详细错误信息
                                    val errorMsg = "发送失败: ${e.message}\n类型: ${e.javaClass.simpleName}"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    // 也在消息中显示错误
                                    val errorMessage = Message(role = "assistant", content = "❌ 发送失败: ${e.message}")
                                    currentSession = currentSession.addMessage(errorMessage)
                                    sessionManager.saveSession(currentSession)
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else if (apiKey.isBlank()) {
                            Toast.makeText(context, "请先配置 API Key", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF07C160)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White)
                    }
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

/**
 * 工具界面
 */
@Composable
fun ToolsScreen(
    screenshotManager: ScreenshotManager,
    ocrEngine: OcrEngine,
    kernel: Kernel,
    screenshotHelper: ScreenshotHelper
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 标题
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Text(
                text = "工具",
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
            // 截图工具
            item {
                WeChatToolItem(
                    icon = Icons.Default.Screenshot,
                    title = "截图",
                    subtitle = if (screenshotHelper.isAuthorized()) "已授权 - 点击截图" else "未授权 - 点击授权",
                    onClick = {
                        if (!screenshotHelper.isAuthorized()) {
                            if (activity != null) {
                                Toast.makeText(context, "请授权截图权限", Toast.LENGTH_SHORT).show()
                                screenshotHelper.requestPermission(activity)
                            } else {
                                Toast.makeText(context, "无法获取Activity，请从主界面操作", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            scope.launch {
                                try {
                                    val bitmap = screenshotHelper.capture()
                                    if (bitmap != null) {
                                        Toast.makeText(context, "截图成功", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "截图失败", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "截图失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            // OCR 工具
            item {
                WeChatToolItem(
                    icon = Icons.Default.DocumentScanner,
                    title = "OCR 识别",
                    subtitle = if (ocrEngine.isInitialized()) "已就绪 - 点击识别" else "初始化中...",
                    onClick = {
                        if (!ocrEngine.isInitialized()) {
                            Toast.makeText(context, "OCR 正在初始化，请稍后再试", Toast.LENGTH_SHORT).show()
                        } else if (!screenshotHelper.isAuthorized()) {
                            Toast.makeText(context, "请先授权截图权限", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                try {
                                    val bitmap = screenshotHelper.capture()
                                    if (bitmap != null) {
                                        val result = ocrEngine.recognize(bitmap)
                                        bitmap.recycle()
                                        if (result.success) {
                                            Toast.makeText(context, "识别成功: ${result.fullText.take(50)}...", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "识别失败: ${result.error}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "截图失败", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "OCR 失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            // 执行 Shell
            item {
                WeChatToolItem(
                    icon = Icons.Default.Terminal,
                    title = "执行命令",
                    subtitle = "执行 Shell 命令",
                    onClick = {
                        Toast.makeText(context, "功能开发中", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/**
 * 微信风格工具项
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
        color = Color.White
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
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * 设置界面 - 微信风格
 */
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    kernel: Kernel,
    screenshotManager: ScreenshotManager,
    screenshotHelper: ScreenshotHelper
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // 配置
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: "") }
    var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "") }
    
    var showApiKeyEditor by remember { mutableStateOf(false) }
    var showEndpointEditor by remember { mutableStateOf(false) }
    var showModelEditor by remember { mutableStateOf(false) }
    
    // 热更新状态
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    val hotUpdateManager = remember { HotUpdateManager(context) }
    
    var tempApiKey by remember { mutableStateOf(apiKey) }
    var tempEndpoint by remember { mutableStateOf(apiEndpoint) }
    var tempModel by remember { mutableStateOf(modelName) }
    var showPassword by remember { mutableStateOf(false) }

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
                text = "设置",
                modifier = Modifier.padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API 配置组
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                WeChatSettingItem(
                    title = "API Key",
                    subtitle = if (apiKey.isBlank()) "未设置" else "${apiKey.take(10)}...",
                    onClick = { 
                        tempApiKey = apiKey
                        showApiKeyEditor = true 
                    }
                )
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                WeChatSettingItem(
                    title = "API Endpoint",
                    subtitle = apiEndpoint,
                    onClick = { 
                        tempEndpoint = apiEndpoint
                        showEndpointEditor = true 
                    }
                )
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                WeChatSettingItem(
                    title = "模型名称",
                    subtitle = modelName,
                    onClick = { 
                        tempModel = modelName
                        showModelEditor = true 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 权限配置组
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                WeChatSettingItem(
                    title = "无障碍服务",
                    subtitle = if (ChenyiAccessibilityService.getInstance() != null) "已开启" else "未开启 - 点击开启",
                    onClick = {
                        Toast.makeText(context, "请开启无障碍服务", Toast.LENGTH_SHORT).show()
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                WeChatSettingItem(
                    title = "截图权限",
                    subtitle = if (screenshotHelper.isAuthorized()) "已授权" else "未授权 - 点击授权",
                    onClick = {
                        if (activity != null) {
                            Toast.makeText(context, "请授权截图权限", Toast.LENGTH_SHORT).show()
                            screenshotHelper.requestPermission(activity)
                        } else {
                            Toast.makeText(context, "无法获取Activity，请从主界面操作", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 热更新组
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                WeChatSettingItem(
                    title = "检查更新",
                    subtitle = "检查并下载最新内核",
                    onClick = {
                        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                        hotUpdateManager.checkUpdate { hasUpdate, message ->
                            if (hasUpdate) {
                                updateMessage = message
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // 更新确认对话框
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showUpdateDialog = false },
            title = { Text("发现新版本") },
            text = { 
                Column {
                    Text(updateMessage)
                    if (isUpdating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("正在下载更新...")
                        }
                    }
                }
            },
            confirmButton = {
                if (!isUpdating) {
                    TextButton(onClick = {
                        isUpdating = true
                        hotUpdateManager.downloadAndUpdate { success, msg ->
                            isUpdating = false
                            showUpdateDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text("下载并安装")
                    }
                }
            },
            dismissButton = {
                if (!isUpdating) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("稍后再说")
                    }
                }
            }
        )
    }

    // API Key 编辑对话框
    if (showApiKeyEditor) {
        AlertDialog(
            onDismissRequest = { showApiKeyEditor = false },
            title = { Text("API Key") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("输入 API Key") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "隐藏" else "显示"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apiKey = tempApiKey
                        prefs.edit().putString("api_key", tempApiKey).apply()
                        // 更新内核配置
                        val config = KernelConfig(
                            apiEndpoint = apiEndpoint,
                            apiKey = tempApiKey,
                            modelName = modelName
                        )
                        kernel.updateConfig(config)
                        Toast.makeText(context, "API Key 已保存", Toast.LENGTH_SHORT).show()
                        showApiKeyEditor = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyEditor = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Endpoint 编辑对话框
    if (showEndpointEditor) {
        AlertDialog(
            onDismissRequest = { showEndpointEditor = false },
            title = { Text("API Endpoint") },
            text = {
                OutlinedTextField(
                    value = tempEndpoint,
                    onValueChange = { tempEndpoint = it },
                    label = { Text("Endpoint") },
                    placeholder = { Text("https://api.example.com/v1") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        apiEndpoint = tempEndpoint
                        prefs.edit().putString("api_endpoint", tempEndpoint).apply()
                        // 更新内核配置
                        val config = KernelConfig(
                            apiEndpoint = tempEndpoint,
                            apiKey = apiKey,
                            modelName = modelName
                        )
                        kernel.updateConfig(config)
                        Toast.makeText(context, "Endpoint 已保存", Toast.LENGTH_SHORT).show()
                        showEndpointEditor = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndpointEditor = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Model 编辑对话框
    if (showModelEditor) {
        AlertDialog(
            onDismissRequest = { showModelEditor = false },
            title = { Text("模型名称") },
            text = {
                OutlinedTextField(
                    value = tempModel,
                    onValueChange = { tempModel = it },
                    label = { Text("模型") },
                    placeholder = { Text("glm-4-flash") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        modelName = tempModel
                        prefs.edit().putString("model_name", tempModel).apply()
                        // 更新内核配置
                        val config = KernelConfig(
                            apiEndpoint = apiEndpoint,
                            apiKey = apiKey,
                            modelName = tempModel
                        )
                        kernel.updateConfig(config)
                        Toast.makeText(context, "模型名称已保存", Toast.LENGTH_SHORT).show()
                        showModelEditor = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelEditor = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 微信风格设置项
 */
@Composable
fun WeChatSettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp)
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "进入",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 个人中心界面
 */
@Composable
fun ProfileScreen(
    kernelInitialized: Boolean,
    screenshotManager: ScreenshotManager,
    screenshotHelper: ScreenshotHelper
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 头像和名称
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    color = Color(0xFF07C160),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "头像",
                        tint = Color.White,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "晨翼 Agent",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "版本 1.0.0",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 状态信息
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                WeChatSettingItem(
                    title = "内核状态",
                    subtitle = if (kernelInitialized) "正常运行" else "未初始化",
                    onClick = {}
                )
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                WeChatSettingItem(
                    title = "无障碍服务",
                    subtitle = if (ChenyiAccessibilityService.getInstance() != null) "已开启" else "未开启",
                    onClick = {
                        Toast.makeText(context, "请开启无障碍服务", Toast.LENGTH_SHORT).show()
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                WeChatSettingItem(
                    title = "截图权限",
                    subtitle = if (screenshotHelper.isAuthorized()) "已授权" else "未授权",
                    onClick = {
                        if (activity != null) {
                            Toast.makeText(context, "请授权截图权限", Toast.LENGTH_SHORT).show()
                            screenshotHelper.requestPermission(activity)
                        } else {
                            Toast.makeText(context, "无法获取Activity，请从主界面操作", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
