package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*

// ==================== Data Models ====================

/**
 * 导航项数据类
 * @param label 标签文字
 * @param icon 图标
 * @param gradient 渐变色（用于活跃状态的 glow 效果）
 */
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val gradient: List<Color> = GradientPrimary
)

/**
 * 预定义的导航项列表
 */
val navItems = listOf(
    NavItem("聊天", Icons.Default.Chat, GradientPrimary),
    NavItem("会话", Icons.Default.History, listOf(AccentPurple, AccentPink)),
    NavItem("工具", Icons.Default.AutoAwesome, listOf(AccentGreen, Color(0xFF10B981))),
    NavItem("任务", Icons.Default.Schedule, listOf(AccentYellow, Color(0xFFF59E0B))),
    NavItem("设置", Icons.Default.Settings, listOf(AccentRed, Color(0xFFEF4444)))
)

// ==================== Main Composable ====================

/**
 * 现代化底部导航栏 - 微信风格
 * 
 * 特性：
 * - 5 个标签页导航
 * - 玻璃态背景效果
 * - 活跃状态 glow 动画
 * - 平滑的切换动画
 * 
 * @param selectedTab 当前选中的标签索引
 * @param onTabSelected 标签切换回调
 * @param modifier 修饰符
 */
@Composable
fun ModernBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 动画状态 - 用于未来扩展
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        // 玻璃态背景层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BgSecondary.copy(alpha = 0.95f),
                            BgSecondary.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
                .blur(20.dp)
        )
        
        // 渐变背景
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgSecondary.copy(alpha = 0.92f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(0.dp)
        ) {
            // 顶部边框线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BorderSubtle.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )
        }
        
        // 导航项容器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { index, item ->
                NavItemWidget(
                    item = item,
                    isSelected = selectedTab == index,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

// ==================== Nav Item Widget ====================

/**
 * 单个导航项组件
 * 
 * @param item 导航项数据
 * @param isSelected 是否选中
 * @param onClick 点击回调
 */
@Composable
fun NavItemWidget(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 选中状态的动画
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else 0f,
        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic),
        label = "glowAlpha"
    )
    
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(durationMillis = 200),
        label = "iconAlpha"
    )
    
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 自定义点击效果
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glow 背景效果
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (glowAlpha > 0.01f) {
                        Modifier
                            .background(
                                brush = Brush.radialGradient(
                                    colors = item.gradient.map { 
                                        it.copy(alpha = 0.12f * glowAlpha) 
                                    },
                                    radius = 48f
                                ),
                                shape = CircleShape
                            )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // 图标
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) AccentCyan else TextMuted,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = iconAlpha
                    }
                    .then(
                        if (isSelected) {
                            Modifier.drawBehind {
                                // Icon 周围的 glow 效果
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            AccentCyan.copy(alpha = 0.4f * glowAlpha),
                                            AccentCyan.copy(alpha = 0.2f * glowAlpha),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = 20.dp.toPx()
                                )
                            }
                        } else Modifier
                    )
            )
        }
        
        // 标签文字
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) AccentCyan else TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 2.dp)
                .graphicsLayer {
                    alpha = if (isSelected) 1f else 0.7f
                }
        )
    }
}

// ==================== Preview ====================

@Composable
fun ModernBottomBarPreview() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        verticalArrangement = Arrangement.Bottom
    ) {
        ModernBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}
