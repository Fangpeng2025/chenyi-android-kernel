package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*

/**
 * 设置页面 - 带实际功能的API配置
 */
@Composable
fun SettingsScreen(
    apiKey: String,
    apiEndpoint: String,
    modelName: String,
    compressionEnabled: Boolean,
    compressionThreshold: Int,
    compressionRatio: Int,
    accessibilityEnabled: Boolean,
    appVersion: String,
    onApiKeyChange: (String) -> Unit,
    onApiEndpointChange: (String) -> Unit,
    onModelNameChange: (String) -> Unit,
    onUserProfileClick: () -> Unit,
    onCompressionToggle: (Boolean) -> Unit,
    onCompressionThresholdChange: (Int) -> Unit,
    onCompressionRatioChange: (Int) -> Unit,
    onAccessibilityClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    updateAvailable: Boolean,
    isCheckingUpdate: Boolean,
    downloadProgress: Int,
    versionInfo: UpdateInfo?,
    modifier: Modifier = Modifier
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiEndpointDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // API 配置部分
        item {
            SettingsSection(title = "API 配置") {
                SettingsItem(
                    icon = Icons.Default.Key,
                    label = "API Key",
                    value = if (apiKey.isNotEmpty()) "${apiKey.take(8)}..." else "未设置",
                    onClick = { showApiKeyDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    label = "API Endpoint",
                    value = apiEndpoint,
                    onClick = { showApiEndpointDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    label = "模型名称",
                    value = modelName,
                    onClick = { showModelDialog = true }
                )
            }
        }
        
        // 用户画像部分
        item {
            SettingsSection(title = "个人配置") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    label = "用户画像",
                    value = "配置个人信息和偏好",
                    onClick = onUserProfileClick
                )
            }
        }
        
        // 压缩配置部分
        item {
            SettingsSection(title = "上下文压缩") {
                SettingsToggle(
                    icon = Icons.Default.Compress,
                    label = "启用压缩",
                    checked = compressionEnabled,
                    onCheckedChange = onCompressionToggle
                )
                
                if (compressionEnabled) {
                    SettingsSlider(
                        label = "触发阈值 (消息数)",
                        value = compressionThreshold,
                        valueRange = 10f..200f,
                        onValueChange = { onCompressionThresholdChange(it.toInt()) }
                    )
                    
                    SettingsSlider(
                        label = "压缩比例 (%)",
                        value = compressionRatio,
                        valueRange = 30f..90f,
                        onValueChange = { onCompressionRatioChange(it.toInt()) }
                    )
                }
            }
        }
        
        // 服务部分
        item {
            SettingsSection(title = "服务") {
                SettingsItem(
                    icon = Icons.Default.Accessibility,
                    label = "无障碍服务",
                    value = if (accessibilityEnabled) "已启用" else "未启用",
                    onClick = onAccessibilityClick
                )
            }
        }
        
        // 关于部分
        item {
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    label = "版本",
                    value = appVersion,
                    onClick = onAboutClick
                )
                
                SettingsButton(
                    text = if (isCheckingUpdate) "检查中..." else "检查更新",
                    loading = isCheckingUpdate,
                    onClick = onCheckUpdate
                )
                
                if (updateAvailable && versionInfo != null) {
                    UpdateCard(
                        versionInfo = versionInfo,
                        downloadProgress = downloadProgress
                    )
                }
            }
        }
    }
    
    // API Key 输入对话框
    if (showApiKeyDialog) {
        InputDialog(
            title = "API Key",
            initialValue = apiKey,
            isPassword = true,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { newKey ->
                onApiKeyChange(newKey)
                showApiKeyDialog = false
            }
        )
    }
    
    // API Endpoint 输入对话框
    if (showApiEndpointDialog) {
        InputDialog(
            title = "API Endpoint",
            initialValue = apiEndpoint,
            onDismiss = { showApiEndpointDialog = false },
            onConfirm = { newEndpoint ->
                onApiEndpointChange(newEndpoint)
                showApiEndpointDialog = false
            }
        )
    }
    
    // 模型名称输入对话框
    if (showModelDialog) {
        InputDialog(
            title = "模型名称",
            initialValue = modelName,
            onDismiss = { showModelDialog = false },
            onConfirm = { newModel ->
                onModelNameChange(newModel)
                showModelDialog = false
            }
        )
    }
}

/**
 * 设置分组
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgSecondary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = TextMuted
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted
        )
    }
}

/**
 * 设置开关
 */
@Composable
fun SettingsToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * 设置滑块
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = value.toString(),
                fontSize = 14.sp,
                color = AccentCyan
            )
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan
            )
        )
    }
}

/**
 * 设置按钮
 */
@Composable
fun SettingsButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCyan
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = TextPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}

/**
 * 输入对话框
 */
@Composable
fun InputDialog(
    title: String,
    initialValue: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue: String -> value = newValue },
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 更新信息卡片
 */
@Composable
fun UpdateCard(
    versionInfo: UpdateInfo,
    downloadProgress: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = BgSecondary,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "发现新版本 v${versionInfo.versionName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AccentCyan
            )
            
            Text(
                text = versionInfo.releaseNotes,
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            if (downloadProgress > 0) {
                LinearProgressIndicator(
                    progress = downloadProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = AccentCyan
                )
                Text(
                    text = "$downloadProgress%",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * 更新信息数据类
 */
data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String
)
