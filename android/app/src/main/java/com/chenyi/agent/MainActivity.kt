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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("chenyi_config", Context.MODE_PRIVATE)

        // 初始化内核
        kernel = Kernel(this)
        val initialized = kernel.init()

        // 初始化截图管理器
        screenshotManager = ScreenshotManager(this)
        screenshotManager.init()

        setContent {
            ChenYiTheme {
                MainScreen(
                    kernelInitialized = initialized,
                    kernel = kernel,
                    prefs = prefs,
                    screenshotManager = screenshotManager,
                    onRequestScreenshotPermission = {
                        screenshotManager.requestPermission(this)
                    }
                )
            }
        }
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
    onRequestScreenshotPermission: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("内核已就绪") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 加载设置
    var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "https://oneapi.xintiandi.online/v1") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "sk-fsy2yLugW1SPt3ZKEfA4B4133f7c42Dd890cD3F582C120C2") ?: "") }
    var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "glm-5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("晨翼 Agent", fontWeight = FontWeight.Bold)
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
                                messages = messages + Message("user", message, System.currentTimeMillis())
                                isLoading = true
                                status = "正在思考..."

                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            kernel.chat(message)
                                        }
                                        isLoading = false
                                        if (result.success) {
                                            val response = result.data?.get("response") as? String ?: "无响应"
                                            messages = messages + Message("assistant", response, System.currentTimeMillis())
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
        }
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
                items(messages) { message ->
                    MessageCard(message)
                }

                if (messages.isEmpty() && !isLoading) {
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
                    prefs.edit()
                        .putString("api_endpoint", apiEndpoint)
                        .putString("api_key", apiKey)
                        .putString("model_name", modelName)
                        .apply()
                    showSettings = false
                    status = "设置已保存"
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
                Text(
                    text = if (isUser) "你" else "晨翼",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            SelectionContainer {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
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

data class Message(
    val role: String,
    val content: String,
    val timestamp: Long
)

@Composable
fun ChenYiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2196F3),
            primaryContainer = Color(0xFFBBDEFB),
            secondary = Color(0xFF03DAC6),
            error = Color(0xFFF44336)
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 16.sp),
            titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ),
        content = content
    )
}