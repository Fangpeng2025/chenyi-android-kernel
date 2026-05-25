package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*
import com.chenyi.agent.UpdateManager

// ==================== Data Models ====================

/**
 * 设置项数据类
 * @param icon 图标
 * @param label 标签
 * @param value 值（可选）
 * @param badge 徽章文字（可选）
 * @param onClick 点击回调
 */
data class SettingsItem(
    val icon: ImageVector,
    val label: String,
    val value: String? = null,
    val badge: String? = null,
    val onClick: () -> Unit = {}
)

/**
 * 设置分组数据类
 * @param title 分组标题
 * @param items 设置项列表
 */
data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

// ==================== Main Settings Screen ====================

/**
 * 设置页面主组件
 *
 * 特性：
 * - API 配置
 * - 用户画像
 * - 压缩配置（开关 + 滑块）
 * - 无障碍服务
 * - 自动更新按钮
 * - 关于
 *
 * @param apiKey API Key
 * @param apiEndpoint API Endpoint
 * @param modelName 模型名称
 * @param compressionEnabled 压缩是否启用
 * @param compressionThreshold 压缩阈值
 * @param compressionRatio 压缩比例
 * @param accessibilityEnabled 无障碍服务是否启用
 * @param appVersion 应用版本
 * @param onApiKeyClick API Key 点击回调
 * @param onApiEndpointClick API Endpoint 点击回调
 * @param onModelNameClick 模型名称点击回调
 * @param onUserProfileClick 用户画像点击回调
 * @param onCompressionToggle 压缩开关切换回调
 * @param onCompressionThresholdChange 压缩阈值变化回调
 * @param onCompressionRatioChange 压缩比例变化回调
 * @param onAccessibilityClick 无障碍服务点击回调
 * @param onAboutClick 关于点击回调
 * @param onCheckUpdate 检查更新回调
 * @param modifier 修饰符
 */
@Composable
fun SettingsScreen(
    apiKey: String?,
    apiEndpoint: String,
    modelName: String,
    compressionEnabled: Boolean,
    compressionThreshold: Int,
    compressionRatio: Int,
    accessibilityEnabled: Boolean,
    appVersion: String,
    onApiKeyClick: () -> Unit,
    onApiEndpointClick: () -> Unit,
    onModelNameClick: () -> Unit,
    onUserProfileClick: () -> Unit,
    onCompressionToggle: (Boolean) -> Unit,
    onCompressionThresholdChange: (Int) -> Unit,
    onCompressionRatioChange: (Int) -> Unit,
    onAccessibilityClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    isCheckingUpdate: Boolean = false,
    updateAvailable: Boolean = false,
    versionInfo: UpdateManager.VersionInfo? = null,
    downloadProgress: Int = 0
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 标题栏
        item {
            SettingsHeader(appVersion = appVersion)
        }

        // API 配置分组
        item {
            SettingsSectionTitle(title = "API 配置")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Key,
                label = "API Key",
                badge = if (apiKey != null) "已配置" else null,
                onClick = onApiKeyClick
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Link,
                label = "API Endpoint",
                value = apiEndpoint,
                onClick = onApiEndpointClick
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Hub,
                label = "模型名称",
                badge = modelName,
                onClick = onModelNameClick
            )
        }

        // 用户画像分组
        item {
            SettingsSectionTitle(title = "用户画像")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Person,
                label = "编辑用户画像",
                onClick = onUserProfileClick
            )
        }

        // 压缩配置分组
        item {
            SettingsSectionTitle(title = "压缩配置")
        }

        item {
            SettingsToggleItem(
                icon = Icons.Default.Download,
                label = "启用压缩",
                checked = compressionEnabled,
                onToggle = onCompressionToggle
            )
        }

        item {
            SettingsSliderItem(
                label = "压缩阈值",
                value = compressionThreshold,
                valueLabel = "$compressionThreshold tokens",
                min = 1000,
                max = 8000,
                onValueChange = onCompressionThresholdChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            SettingsSliderItem(
                label = "目标压缩比例",
                value = compressionRatio,
                valueLabel = "$compressionRatio%",
                min = 10,
                max = 90,
                onValueChange = onCompressionRatioChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 系统分组
        item {
            SettingsSectionTitle(title = "系统")
        }

        item {
            SettingsItem(
                icon = Icons.Default.Accessibility,
                label = "无障碍服务",
                badge = if (accessibilityEnabled) "已启用" else "未启用",
                onClick = onAccessibilityClick
            )
        }

        // 自动更新按钮
        item {
            AutoUpdateButton(
                onCheckUpdate = onCheckUpdate,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                isChecking = isCheckingUpdate,
                updateAvailable = updateAvailable,
                versionInfo = versionInfo,
                downloadProgress = downloadProgress
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                label = "关于",
                value = appVersion,
                onClick = onAboutClick
            )
        }
    }
}

// ==================== Settings Header ====================

/**
 * 设置页面标题栏
 */
@Composable
private fun SettingsHeader(
    appVersion: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BgSecondary,
                        BgSecondary.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = "设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(GradientPrimary)
                )
            )
            Text(
                text = "$appVersion (Build ${appVersion.replace(".", "").take(5)})",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

// ==================== Settings Section Title ====================

/**
 * 设置分组标题组件
 */
@Composable
fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = AccentCyan,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ==================== Settings Item ====================

/**
 * 设置项组件
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    value: String? = null,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPressed) BgGlass else BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = GradientPrimary.map { it.copy(alpha = 0.12f) },
                                radius = 40f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }

            // 右侧：值/徽章 + 箭头
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 徽章
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = AccentCyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AccentCyan
                        )
                    }
                } else if (value != null) {
                    Text(
                        text = value,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // 箭头
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==================== Settings Toggle Item ====================

/**
 * 设置开关项组件
 */
@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggle(!checked) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = GradientPrimary.map { it.copy(alpha = 0.12f) },
                                radius = 40f
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }

            // 开关
            ToggleSwitch(
                checked = checked,
                onToggle = onToggle
            )
        }
    }
}

// ==================== Toggle Switch ====================

/**
 * 开关组件
 */
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .background(
                brush = if (checked) {
                    Brush.linearGradient(GradientPrimary)
                } else {
                    Brush.linearGradient(listOf(BgTertiary, BgTertiary))
                },
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onToggle(!checked) }
    ) {
        // 开关圆点
        Box(
            modifier = Modifier
                .padding(start = animatedOffset.dp, top = 2.dp)
                .size(20.dp)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
                .graphicsLayer {
                    shadowElevation = 8f
                    shape = CircleShape
                    clip = false
                }
        )
    }
}

// ==================== Settings Slider Item ====================

/**
 * 设置滑块项组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSliderItem(
    label: String,
    value: Int,
    valueLabel: String,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = valueLabel,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 滑块
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = BgTertiary
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                brush = Brush.linearGradient(GradientPrimary),
                                shape = CircleShape
                            )
                            .graphicsLayer {
                                shadowElevation = 10f
                                shape = CircleShape
                                clip = false
                            }
                    )
                }
            )
        }
    }
}

// ==================== Auto Update Button ====================

/**
 * 自动更新按钮组件
 * 
 * 功能：
 * - 检查服务器最新版本
 * - 下载 APK 文件
 * - 安装更新
 * 
 * 服务器：
 * - https://oneapi.xintiandi.online/chenyi-agent/
 */
@Composable
fun AutoUpdateButton(
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    isChecking: Boolean = false,
    updateAvailable: Boolean = false,
    versionInfo: UpdateManager.VersionInfo? = null,
    downloadProgress: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckUpdate() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (updateAvailable) {
                            listOf(AccentGreen.copy(alpha = 0.1f), AccentCyan.copy(alpha = 0.05f))
                        } else {
                            listOf(AccentCyan.copy(alpha = 0.05f), AccentPurple.copy(alpha = 0.05f))
                        }
                    )
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(GradientPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = "检查更新",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = when {
                            downloadProgress > 0 -> "下载中 $downloadProgress%"
                            updateAvailable && versionInfo != null -> "v${versionInfo.versionName} 可用"
                            isChecking -> "检查中..."
                            else -> "检查更新"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    when {
                        downloadProgress > 0 -> {
                            // 下载进度条
                            LinearProgressIndicator(
                                progress = downloadProgress / 100f,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .width(80.dp)
                                    .height(3.dp),
                                color = AccentCyan,
                                trackColor = BgTertiary
                            )
                        }
                        updateAvailable && versionInfo != null -> {
                            Text(
                                text = "点击下载安装",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        else -> {
                            Text(
                                text = "当前已是最新版本",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // 右侧箭头
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==================== Preview ====================

@Composable
fun SettingsScreenPreview() {
    var compressionEnabled by remember { mutableStateOf(true) }
    var compressionThreshold by remember { mutableStateOf(4000) }
    var compressionRatio by remember { mutableStateOf(50) }

    SettingsScreen(
        apiKey = "sk-xxx",
        apiEndpoint = "api.openai.com",
        modelName = "glm-5",
        compressionEnabled = compressionEnabled,
        compressionThreshold = compressionThreshold,
        compressionRatio = compressionRatio,
        accessibilityEnabled = true,
        appVersion = "1.0.16",
        onApiKeyClick = {},
        onApiEndpointClick = {},
        onModelNameClick = {},
        onUserProfileClick = {},
        onCompressionToggle = { compressionEnabled = it },
        onCompressionThresholdChange = { compressionThreshold = it },
        onCompressionRatioChange = { compressionRatio = it },
        onAccessibilityClick = {},
        onAboutClick = {},
        onCheckUpdate = {}
    )
}
