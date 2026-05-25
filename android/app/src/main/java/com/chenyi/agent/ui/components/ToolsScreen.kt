package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*

// ==================== Data Models ====================

/**
 * 工具数据类
 * @param id 工具ID
 * @param name 工具名称
 * @param description 工具描述
 * @param icon 工具图标
 * @param gradient 渐变色
 */
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color> = GradientPrimary
)

/**
 * 预定义的工具列表
 */
val defaultTools = listOf(
    Tool(
        id = "screenshot",
        name = "截图",
        description = "截取当前屏幕",
        icon = Icons.Default.Screenshot,
        gradient = GradientPrimary
    ),
    Tool(
        id = "ocr",
        name = "OCR 识别",
        description = "识别屏幕文字",
        icon = Icons.Default.DocumentScanner,
        gradient = listOf(AccentPurple, AccentPink)
    ),
    Tool(
        id = "click",
        name = "点击坐标",
        description = "点击指定位置",
        icon = Icons.Default.TouchApp,
        gradient = listOf(AccentGreen, Color(0xFF10B981))
    ),
    Tool(
        id = "swipe",
        name = "滑动屏幕",
        description = "滑动操作",
        icon = Icons.Default.Swipe,
        gradient = listOf(AccentYellow, Color(0xFFF59E0B))
    ),
    Tool(
        id = "input",
        name = "输入文本",
        description = "输入文字",
        icon = Icons.Default.Keyboard,
        gradient = listOf(AccentCyan, AccentPurple)
    ),
    Tool(
        id = "open_app",
        name = "打开应用",
        description = "启动应用",
        icon = Icons.Default.Apps,
        gradient = listOf(AccentPink, AccentRed)
    ),
    Tool(
        id = "home",
        name = "返回桌面",
        description = "回到主屏幕",
        icon = Icons.Default.Home,
        gradient = listOf(AccentGreen, AccentCyan)
    ),
    Tool(
        id = "current_app",
        name = "获取当前应用",
        description = "获取应用信息",
        icon = Icons.Default.Info,
        gradient = listOf(AccentPurple, AccentCyan)
    )
)

// ==================== Main Tools Screen ====================

/**
 * 工具页面主组件
 *
 * 特性：
 * - 2列网格布局
 * - 工具卡片（图标 + 名称 + 描述）
 * - 点击动画效果
 *
 * @param tools 工具列表
 * @param onToolClick 工具点击回调
 * @param modifier 修饰符
 */
@Composable
fun ToolsScreen(
    tools: List<Tool> = defaultTools,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部标题栏
        ToolsHeader(toolCount = tools.size)

        // 工具网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tools, key = { it.id }) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}

// ==================== Tools Header ====================

/**
 * 工具页面标题栏
 */
@Composable
private fun ToolsHeader(
    toolCount: Int
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
                text = "工具与技能",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(GradientPrimary)
                )
            )
            Text(
                text = "$toolCount 个可用工具",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

// ==================== Tool Card ====================

/**
 * 工具卡片组件
 *
 * @param tool 工具数据
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 动画效果
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Hover glow 效果
                    if (isPressed) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = tool.gradient.map { it.copy(alpha = 0.1f) },
                                center = androidx.compose.ui.geometry.Offset(
                                    this.size.width / 2,
                                    this.size.height / 2
                                ),
                                radius = this.size.width
                            )
                        )
                    }
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = tool.gradient.map { it.copy(alpha = 0.12f) },
                                radius = 56f
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.name,
                        tint = tool.gradient.first(),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 工具名称
                Text(
                    text = tool.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // 工具描述
                Text(
                    text = tool.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==================== Preview ====================

@Composable
fun ToolsScreenPreview() {
    ToolsScreen(
        tools = defaultTools,
        onToolClick = {}
    )
}
