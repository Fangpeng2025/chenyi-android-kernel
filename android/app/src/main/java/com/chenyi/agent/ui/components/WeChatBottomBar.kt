package com.chenyi.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyi.agent.ui.theme.*

/**
 * 微信风格底部导航栏 - 3个标签页
 * 
 * @param selectedTab 当前选中的标签索引 (0: 聊天, 1: 技能, 2: 设置)
 * @param onTabSelected 标签切换回调
 * @param modifier 修饰符
 */
@Composable
fun WeChatBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = BgSecondary,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 聊天
            TabItem(
                icon = Icons.Default.Chat,
                label = "聊天",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            
            // 技能
            TabItem(
                icon = Icons.Default.AutoAwesome,
                label = "技能",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            
            // 设置
            TabItem(
                icon = Icons.Default.Settings,
                label = "设置",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

/**
 * 单个标签项
 */
@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(80.dp)
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentCyan else TextMuted,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) AccentCyan else TextMuted,
            maxLines = 1
        )
        
        // 选中指示器
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        brush = Brush.horizontalGradient(GradientPrimary)
                    )
            )
        }
    }
}

@Composable
fun WeChatBottomBarPreview() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        verticalArrangement = Arrangement.Bottom
    ) {
        WeChatBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}
