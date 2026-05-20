package com.chenyi.agent

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 形状系统
 */
val ChenYiShapes = Shapes(
    // 小圆角 - 用于按钮、文本框
    small = RoundedCornerShape(8.dp),
    
    // 中圆角 - 用于卡片
    medium = RoundedCornerShape(16.dp),
    
    // 大圆角 - 用于对话框、底部抽屉
    large = RoundedCornerShape(28.dp),
    
    // 超大圆角 - 用于底部导航栏
    extraLarge = RoundedCornerShape(32.dp)
)