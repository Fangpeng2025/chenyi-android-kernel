package com.chenyi.agent

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.core.content.FileProvider
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
import java.io.File
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
        
        // 先初始化内核库（加载 Rust native library）
        Kernel.initLibrary(this)
        
        kernel = Kernel(this)
        val initialized = kernel.init()
        
        // 如果有保存的 API Key，立即设置到内核
        val savedApiKey = prefs.getString("api_key", "") ?: ""
        if (savedApiKey.isNotBlank() && initialized) {
            kernel.setApiKey(savedApiKey)
            Log.d("MainActivity", "已从 SharedPreferences 加载 API Key")
        }
        
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
    
    // 无障碍服务检查
    var showA11yDialog by remember { mutableStateOf(false) }
    val a11yService = ChenyiAccessibilityService.getInstance()
    
    // 启动时检查无障碍服务
    LaunchedEffect(Unit) {
        if (a11yService == null) {
            showA11yDialog = true
        }
    }

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
                2 -> SettingsScreen(prefs, kernel, screenshotManager, screenshotHelper, kernelInitialized)
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
 * 微信底部导航栏
 */
@Composable
fun WeChatBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "聊天" to Icons.Default.Chat,
        "技能" to Icons.Default.AutoAwesome,
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
    
    // 待发送的消息（用于触发 LaunchedEffect）
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    
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
    
    // 处理消息发送（使用 LaunchedEffect 避免协程作用域问题）
    LaunchedEffect(pendingMessage) {
        if (pendingMessage != null && pendingMessage!!.isNotBlank()) {
            val message = pendingMessage!!
            pendingMessage = null  // 清除待发送消息
            
            Log.d("ChatScreen", "开始处理消息: $message")
            
            try {
                val response = withContext(Dispatchers.IO) {
                    // 使用 chatWithTools 自动处理工具调用
                    kernel.chatWithTools(message)
                }
                
                Log.d("ChatScreen", "收到响应: success=${response.success}, error=${response.error}")
                
                // 更友好的错误显示
                val content = when {
                    response.response != null && response.response.isNotBlank() -> response.response
                    response.error != null && response.error.isNotBlank() -> {
                        // 解析错误类型，提供友好提示
                        val error = response.error
                        when {
                            error.contains("401") || error.contains("无效的令牌") || error.contains("Unauthorized") -> 
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
                
                Log.d("ChatScreen", "显示内容: $content")
                
                val assistantMessage = Message(role = "assistant", content = content)
                currentSession = currentSession.addMessage(assistantMessage)
                sessionManager.saveSession(currentSession)
            } catch (e: Exception) {
                Log.e("ChatScreen", "发送失败", e)
                // 显示详细错误信息
                val errorMsg = "发送失败: ${e.message}\n类型: ${e.javaClass.simpleName}"
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                // 也在消息中显示错误
                val errorMessage = Message(role = "assistant", content = "❌ 发送失败: ${e.message}")
                currentSession = currentSession.addMessage(errorMessage)
                sessionManager.saveSession(currentSession)
            } finally {
                Log.d("ChatScreen", "处理完成，isLoading = false")
                isLoading = false
            }
        }
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

                            // 设置待发送消息，触发 LaunchedEffect
                            pendingMessage = message
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
                val a11yService = ChenyiAccessibilityService.getInstance()
                val a11yConnected = a11yService != null
                val screenshotAuth = screenshotHelper.isAuthorized()
                
                WeChatToolItem(
                    icon = Icons.Default.Screenshot,
                    title = "截图",
                    subtitle = when {
                        !a11yConnected -> "❌ 无障碍服务未开启"
                        !screenshotAuth -> "⚠️ 截图权限未授权"
                        else -> "✅ 已就绪 - 点击截图"
                    },
                    onClick = {
                        when {
                            !a11yConnected -> {
                                Toast.makeText(context, "请先开启无障碍服务\\n设置 → 无障碍 → 晨翼Agent", Toast.LENGTH_LONG).show()
                            }
                            !screenshotAuth -> {
                                if (activity != null) {
                                    Toast.makeText(context, "请授权截图权限", Toast.LENGTH_SHORT).show()
                                    screenshotHelper.requestPermission(activity)
                                }
                            }
                            else -> {
                                scope.launch {
                                    try {
                                        // 通过 Kernel 执行截图工具（无障碍服务可以截取任何 App）
                                        val result = kernel.executeToolJson("screenshot", "{}")
                                        if (result.success && result.response != null) {
                                            Toast.makeText(context, "截图成功\\n保存至: ${result.response}", Toast.LENGTH_LONG).show()
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

            // OCR 工具
            item {
                val a11yService = ChenyiAccessibilityService.getInstance()
                val a11yConnected = a11yService != null
                val screenshotAuth = screenshotHelper.isAuthorized()
                val ocrReady = ocrEngine.isInitialized()
                
                WeChatToolItem(
                    icon = Icons.Default.DocumentScanner,
                    title = "OCR 识别",
                    subtitle = when {
                        !a11yConnected -> "❌ 无障碍服务未开启"
                        !screenshotAuth -> "⚠️ 截图权限未授权"
                        !ocrReady -> "⏳ OCR 初始化中..."
                        else -> "✅ 已就绪 - 点击识别"
                    },
                    onClick = {
                        when {
                            !a11yConnected -> {
                                Toast.makeText(context, "请先开启无障碍服务\\n设置 → 无障碍 → 晨翼Agent", Toast.LENGTH_LONG).show()
                            }
                            !screenshotAuth -> {
                                if (activity != null) {
                                    Toast.makeText(context, "请授权截图权限", Toast.LENGTH_SHORT).show()
                                    screenshotHelper.requestPermission(activity)
                                }
                            }
                            !ocrReady -> {
                                Toast.makeText(context, "OCR 正在初始化，请稍候...", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                scope.launch {
                                    try {
                                        // 通过 Kernel 执行截图 + OCR 工具（无障碍服务可以截取任何 App）
                                        val screenshotResult = kernel.executeToolJson("screenshot", "{}")
                                        if (screenshotResult.success && screenshotResult.response != null) {
                                            // 解析截图路径
                                            val pathJson = org.json.JSONObject(screenshotResult.response)
                                            val imagePath = pathJson.getString("path")
                                            
                                            // 加载图片并 OCR
                                            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                                            if (bitmap != null) {
                                                val ocrResult = ocrEngine.recognize(bitmap)
                                                bitmap.recycle()
                                                if (ocrResult.success) {
                                                    Toast.makeText(context, "识别成功:\\n${ocrResult.fullText.take(100)}", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "识别失败: ${ocrResult.error}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "加载截图失败", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "截图失败: ${screenshotResult.error}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "OCR 失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
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
    screenshotHelper: ScreenshotHelper,
    kernelInitialized: Boolean
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
    
    // APK 更新状态
    var showApkUpdateDialog by remember { mutableStateOf(false) }
    var apkUpdateVersion by remember { mutableStateOf("") }
    var apkUpdateUrl by remember { mutableStateOf<String?>(null) }
    var apkUpdateNotes by remember { mutableStateOf<String?>(null) }
    var apkDownloadProgress by remember { mutableStateOf(0) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    
    var tempApiKey by remember { mutableStateOf(apiKey) }
    var tempEndpoint by remember { mutableStateOf(apiEndpoint) }
    var tempModel by remember { mutableStateOf(modelName) }
    var showPassword by remember { mutableStateOf(false) }
    
    // 安装 APK 的函数
    fun installApk(apkFile: File) {
        try {
            Log.d("Settings", "准备安装 APK: ${apkFile.absolutePath}")
            
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK 文件不存在", Toast.LENGTH_LONG).show()
                Log.e("Settings", "APK 文件不存在: ${apkFile.absolutePath}")
                return
            }
            
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            Log.d("Settings", "FileProvider URI: $uri")
            
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            Log.d("Settings", "启动安装界面...")
            context.startActivity(intent)
            Log.d("Settings", "安装界面已启动")
        } catch (e: Exception) {
            Log.e("Settings", "安装失败", e)
            Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // 头像和名称（合并"我"页面）
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
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
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

        // 状态信息（合并"我"页面的内核状态）
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
                    subtitle = if (ChenyiAccessibilityService.getInstance() != null) "已开启" else "未开启 - 点击开启",
                    onClick = {
                        Toast.makeText(context, "请开启无障碍服务", Toast.LENGTH_SHORT).show()
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 更新管理组（微信风格卡片）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column {
                // 内核热更新
                WeChatSettingItem(
                    title = "内核热更新",
                    subtitle = "检查并下载最新 Rust 内核",
                    onClick = {
                        Toast.makeText(context, "正在检查内核更新...", Toast.LENGTH_SHORT).show()
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
                HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.5.dp)
                // APK 更新（显示当前版本）
                WeChatSettingItem(
                    title = "应用更新",
                    subtitle = "当前版本: ${BuildConfig.VERSION_NAME} - 点击检查更新",
                    onClick = {
                        Toast.makeText(context, "正在检查应用更新...", Toast.LENGTH_SHORT).show()
                        hotUpdateManager.checkApkUpdate { hasUpdate, version, url, notes ->
                            if (hasUpdate && url != null) {
                                apkUpdateVersion = version
                                apkUpdateUrl = url
                                apkUpdateNotes = notes
                                showApkUpdateDialog = true
                            } else {
                                Toast.makeText(context, "已是最新版本: $version", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // 关于信息
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "晨翼Agent",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF07C160)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "版本: ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "内核: Rust v${kernel.getKernelVersion()}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "官网: xintiandi.online",
                    fontSize = 14.sp,
                    color = Color(0xFF07C160)
                )
            }
        }
    }

    // 内核更新确认对话框
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdating) showUpdateDialog = false },
            title = { Text("发现新内核版本") },
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
                        Text("取消")
                    }
                }
            }
        )
    }

    // APK 更新确认对话框
    if (showApkUpdateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDownloadingApk) showApkUpdateDialog = false },
            title = { Text("发现新版本: $apkUpdateVersion") },
            text = { 
                Column {
                    if (apkUpdateNotes != null) {
                        Text(apkUpdateNotes!!, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    if (isDownloadingApk) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { apkDownloadProgress.toFloat() / 100 },
                                modifier = Modifier.size(20.dp)
                            )
                            Text("下载进度: $apkDownloadProgress%")
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDownloadingApk && apkUpdateUrl != null) {
                    TextButton(onClick = {
                        isDownloadingApk = true
                        apkDownloadProgress = 0
                        hotUpdateManager.downloadApk(
                            apkUpdateUrl!!,
                            { progress -> apkDownloadProgress = progress },
                            { success, apkFile ->
                                isDownloadingApk = false
                                showApkUpdateDialog = false
                                if (success && apkFile != null) {
                                    Log.d("Settings", "APK 下载成功: ${apkFile.absolutePath}, 大小: ${apkFile.length()}")
                                    Toast.makeText(context, "下载完成，正在打开安装界面...", Toast.LENGTH_SHORT).show()
                                    // 延迟一下，让 Toast 显示
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        installApk(apkFile)
                                    }, 500)
                                } else {
                                    Toast.makeText(context, "下载失败，请重试", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }) {
                        Text("下载并安装")
                    }
                }
            },
            dismissButton = {
                if (!isDownloadingApk) {
                    TextButton(onClick = { showApkUpdateDialog = false }) {
                        Text("取消")
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
                        // 同时更新 Rust 内核的 API Key
                        kernel.setApiKey(tempApiKey, apiEndpoint, modelName)
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
