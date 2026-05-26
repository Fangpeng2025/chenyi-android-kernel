package com.chenyi.agent.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chenyi.agent.data.*
import com.chenyi.agent.ui.theme.*
import kotlinx.coroutines.launch

// ==================== API Key Dialog ====================

/**
 * API Key 输入对话框（增强版）
 * 
 * 新增功能：
 * - 格式验证
 * - 自动检测提供商
 * - 清除功能
 */
@Composable
fun ApiKeyDialog(
    currentValue: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validator: ConfigValidator? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var inputValue by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var isValidating by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BgSecondary,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                // 标题
                Text(
                    text = "API Key 配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 当前状态
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (currentValue != null) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (currentValue != null) AccentGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (currentValue != null) "当前状态：已配置" else "当前状态：未配置",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (currentValue != null) AccentGreen else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 输入框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = BgTertiary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { 
                            inputValue = it
                            // 实时验证
                            if (validator != null && it.isNotBlank()) {
                                scope.launch {
                                    isValidating = true
                                    validationResult = validator.validateApiKey(it)
                                    isValidating = false
                                }
                            } else {
                                validationResult = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text(
                                    text = "请输入 API Key",
                                    fontSize = 14.sp,
                                    color = TextMuted
                                )
                            }
                            innerTextField()
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(AccentCyan),
                        visualTransformation = if (showKey) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation()
                    )
                }

                // 验证结果
                if (validationResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val result = validationResult!!
                    val resultColor = when {
                        result.isError -> Color(0xFFEF4444)
                        result.isWarning -> Color(0xFFF59E0B)
                        else -> AccentGreen
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AccentCyan
                            )
                            Text(
                                text = "验证中...",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    result.isError -> Icons.Default.Close
                                    result.isWarning -> Icons.Default.Warning
                                    else -> Icons.Default.Check
                                },
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = result.message,
                                fontSize = 11.sp,
                                color = resultColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 显示/隐藏密码按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showKey) "隐藏密钥" else "显示密钥",
                            fontSize = 12.sp,
                            color = AccentCyan
                        )
                    }

                    TextButton(onClick = {
                        inputValue = ""
                        validationResult = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "清除",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BgCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            if (inputValue.isNotBlank()) {
                                onConfirm(inputValue.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = inputValue.isNotBlank() && validationResult?.isError != true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存", color = BgPrimary)
                    }
                }
            }
        }
    }
}

// ==================== API Endpoint Dialog ====================

/**
 * API Endpoint 配置对话框（增强版）
 * 
 * 新增功能：
 * - 格式验证
 * - 连接测试按钮
 */
@Composable
fun ApiEndpointDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    validator: ConfigValidator? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var inputValue by remember { mutableStateOf(currentValue) }
    var formatValidation by remember { mutableStateOf<ValidationResult?>(null) }
    var connectionTest by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BgSecondary,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "API Endpoint 配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "示例：api.openai.com 或 api.xintiandi.online",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 输入框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = BgTertiary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { 
                            inputValue = it
                            // 实时验证格式
                            if (validator != null) {
                                formatValidation = validator.validateEndpointFormat(it)
                            }
                            connectionTest = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text(
                                    text = "请输入 Endpoint",
                                    fontSize = 14.sp,
                                    color = TextMuted
                                )
                            }
                            innerTextField()
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(AccentCyan)
                    )
                }

                // 验证结果
                if (formatValidation != null || connectionTest != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 格式验证
                        formatValidation?.let { result ->
                            ResultIndicator(result = result)
                        }
                        
                        // 连接测试
                        connectionTest?.let { result ->
                            ResultIndicator(
                                message = result.message,
                                isSuccess = result.isSuccess,
                                isError = result.isError,
                                isWarning = !result.isError && !result.isSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 测试连接按钮
                Button(
                    onClick = {
                        if (validator != null && inputValue.isNotBlank()) {
                            scope.launch {
                                isTesting = true
                                connectionTest = validator.testEndpointConnection(inputValue)
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inputValue.isNotBlank() && !isTesting && formatValidation?.isError != true,
                    colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...", color = TextPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接", color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消", color = TextMuted)
                    }

                    Button(
                        onClick = { onConfirm(inputValue.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = inputValue.isNotBlank() && formatValidation?.isError != true,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存", color = BgPrimary)
                    }
                }
            }
        }
    }
}

// ==================== Model Name Dialog ====================

/**
 * 模型名称选择对话框
 */
@Composable
fun ModelNameDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf(currentValue) }
    
    val presetModels = listOf(
        "glm-5" to "智谱 GLM-5 (推荐)",
        "glm-4" to "智谱 GLM-4",
        "glm-4-flash" to "智谱 GLM-4 Flash (快速)",
        "gpt-4o" to "OpenAI GPT-4o",
        "gpt-4-turbo" to "OpenAI GPT-4 Turbo",
        "gpt-3.5-turbo" to "OpenAI GPT-3.5 Turbo",
        "claude-3-5-sonnet-20241022" to "Anthropic Claude 3.5 Sonnet",
        "claude-3-opus" to "Anthropic Claude 3 Opus",
        "deepseek-chat" to "DeepSeek Chat",
        "deepseek-coder" to "DeepSeek Coder"
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BgSecondary,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "模型选择",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 预设模型列表（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetModels.forEach { (modelId, modelDesc) ->
                        ModelItem(
                            modelId = modelId,
                            modelDesc = modelDesc,
                            isSelected = inputValue == modelId,
                            onClick = { inputValue = modelId }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 自定义输入
                Text(
                    text = "或输入自定义模型名称：",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = BgTertiary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        ),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text("自定义模型名称", fontSize = 14.sp, color = TextMuted)
                            }
                            innerTextField()
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(AccentCyan)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消", color = TextMuted)
                    }

                    Button(
                        onClick = { onConfirm(inputValue.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = inputValue.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确认", color = BgPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    modelId: String,
    modelDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) AccentCyan.copy(alpha = 0.12f) else BgTertiary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = modelId,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) AccentCyan else TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = modelDesc,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== User Profile Dialog (增强版) ====================

/**
 * 用户画像编辑对话框（增强版）
 * 
 * 新增功能：
 * - 头像上传
 * - 语言选择
 * - 响应风格选择
 * - 专业领域
 */
@Composable
fun UserProfileDialog(
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
    initialProfile: UserProfile? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var role by remember { mutableStateOf(initialProfile?.role ?: "") }
    var timezone by remember { mutableStateOf(initialProfile?.timezone ?: "UTC+8") }
    var preferences by remember { mutableStateOf(initialProfile?.preferences ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(initialProfile?.avatarPath?.let { Uri.parse(it) }) }
    var language by remember { mutableStateOf(initialProfile?.language ?: "zh-CN") }
    var responseStyle by remember { mutableStateOf(initialProfile?.responseStyle ?: "concise") }
    var expertise by remember { mutableStateOf(initialProfile?.expertise ?: "") }

    // 头像选择器
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .background(
                    color = BgSecondary,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 标题
                Text(
                    text = "用户画像",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "配置个性化信息，让 Agent 更懂你",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 可滚动内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 头像上传
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = BgTertiary,
                                    shape = CircleShape
                                )
                                .clickable { avatarLauncher.launch("image/*") ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "头像",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "上传头像",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // 用户名
                    ProfileInputField(
                        label = "用户名",
                        value = name,
                        placeholder = "例如：张三",
                        onValueChange = { name = it }
                    )

                    // 角色/职业
                    ProfileInputField(
                        label = "角色/职业",
                        value = role,
                        placeholder = "例如：软件开发工程师",
                        onValueChange = { role = it }
                    )

                    // 时区
                    ProfileInputField(
                        label = "时区",
                        value = timezone,
                        placeholder = "例如：UTC+8",
                        onValueChange = { timezone = it }
                    )

                    // 语言选择
                    Column {
                        Text(
                            text = "语言",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LanguageOption.values().forEach { lang ->
                                LanguageChip(
                                    label = lang.displayName,
                                    selected = language == lang.value,
                                    onClick = { language = lang.value }
                                )
                            }
                        }
                    }

                    // 响应风格
                    Column {
                        Text(
                            text = "响应风格",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResponseStyle.values().forEach { style ->
                                StyleChip(
                                    label = style.displayName,
                                    selected = responseStyle == style.value,
                                    onClick = { responseStyle = style.value }
                                )
                            }
                        }
                    }

                    // 专业领域
                    ProfileInputField(
                        label = "专业领域",
                        value = expertise,
                        placeholder = "例如：人工智能、Web开发、数据科学",
                        onValueChange = { expertise = it }
                    )

                    // 偏好设置
                    ProfileInputField(
                        label = "其他偏好",
                        value = preferences,
                        placeholder = "例如：喜欢简洁的回复风格，偏好代码示例",
                        onValueChange = { preferences = it },
                        singleLine = false,
                        minHeight = 80.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            onSave(UserProfile(
                                name = name.trim(),
                                role = role.trim(),
                                timezone = timezone.trim(),
                                preferences = preferences.trim(),
                                avatarPath = avatarUri?.toString(),
                                language = language,
                                responseStyle = responseStyle,
                                expertise = expertise.trim()
                            ))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存", color = BgPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 52.dp
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .background(
                    color = BgTertiary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = TextMuted)
                    }
                    innerTextField()
                },
                singleLine = singleLine,
                cursorBrush = SolidColor(AccentCyan)
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) AccentCyan else BgCard
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (selected) BgPrimary else TextPrimary
        )
    }
}

@Composable
private fun StyleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) AccentCyan else BgCard
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (selected) BgPrimary else TextPrimary
        )
    }
}

@Composable
private fun ResultIndicator(
    result: ValidationResult? = null,
    message: String? = null,
    isSuccess: Boolean = false,
    isError: Boolean = false,
    isWarning: Boolean = false
) {
    val displayMessage = result?.message ?: message ?: ""
    val success = result?.isSuccess ?: isSuccess
    val error = result?.isError ?: isError
    val warning = result?.isWarning ?: isWarning
    
    val color = when {
        error -> Color(0xFFEF4444)
        warning -> Color(0xFFF59E0B)
        else -> AccentGreen
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = when {
                error -> Icons.Default.Close
                warning -> Icons.Default.Warning
                else -> Icons.Default.Check
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = displayMessage,
            fontSize = 11.sp,
            color = color
        )
    }
}
