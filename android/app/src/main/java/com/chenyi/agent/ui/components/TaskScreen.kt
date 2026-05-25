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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenyi.agent.ui.theme.*

// ==================== Data Models ====================

/**
 * 任务状态枚举
 */
enum class TaskStatus {
    RUNNING,
    PAUSED,
    STOPPED
}

/**
 * 任务数据类
 * @param id 任务ID
 * @param name 任务名称
 * @param description 任务描述
 * @param status 任务状态
 * @param schedule 执行时间
 * @param lastRunTime 上次运行时间
 */
data class Task(
    val id: String,
    val name: String,
    val description: String,
    val status: TaskStatus,
    val schedule: String,
    val lastRunTime: Long = 0
)

// ==================== Main Task Screen ====================

/**
 * 任务页面主组件
 *
 * 特性：
 * - 任务卡片列表
 * - 浮动创建按钮
 * - 创建任务对话框
 *
 * @param tasks 任务列表
 * @param onTaskClick 任务点击回调
 * @param onCreateTask 创建任务回调
 * @param onPauseTask 暂停任务回调
 * @param onDeleteTask 删除任务回调
 * @param modifier 修饰符
 */
@Composable
fun TaskScreen(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onCreateTask: (String, String, String) -> Unit,
    onPauseTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
        ) {
            // 顶部标题栏
            TaskHeader(taskCount = tasks.count { it.status == TaskStatus.RUNNING })

            // 任务列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onPauseToggle = { onPauseTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onClick = { onTaskClick(task) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // 浮动创建按钮
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(GradientPrimary),
                        shape = CircleShape
                    )
                    .graphicsLayer {
                        shadowElevation = 20f
                        shape = CircleShape
                        clip = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "创建任务",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // 创建任务对话框
    if (showDialog) {
        CreateTaskDialog(
            onDismiss = { showDialog = false },
            onCreate = { name, desc, schedule ->
                onCreateTask(name, desc, schedule)
                showDialog = false
            }
        )
    }
}

// ==================== Task Header ====================

/**
 * 任务页面标题栏
 */
@Composable
private fun TaskHeader(
    taskCount: Int
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
                text = "任务管理",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Transparent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(GradientPrimary)
                )
            )
            Text(
                text = "$taskCount 个定时任务",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}

// ==================== Task Item ====================

/**
 * 任务卡片组件
 *
 * @param task 任务数据
 * @param onPauseToggle 暂停/继续回调
 * @param onDelete 删除回调
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun TaskItem(
    task: Task,
    onPauseToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.status) {
        TaskStatus.RUNNING -> AccentGreen
        TaskStatus.PAUSED -> AccentYellow
        TaskStatus.STOPPED -> TextMuted
    }

    val statusText = when (task.status) {
        TaskStatus.RUNNING -> "运行中"
        TaskStatus.PAUSED -> "已暂停"
        TaskStatus.STOPPED -> "已停止"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 头部：标题 + 状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // 状态标签
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 描述
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 底部：时间 + 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = task.schedule,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                // 操作按钮
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 暂停/继续按钮
                    TaskActionButton(
                        icon = if (task.status == TaskStatus.RUNNING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        onClick = onPauseToggle
                    )

                    // 删除按钮
                    TaskActionButton(
                        icon = Icons.Default.Delete,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

// ==================== Task Action Button ====================

/**
 * 任务操作按钮组件
 */
@Composable
private fun TaskActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(32.dp)
            .background(
                color = BgTertiary,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .then(
                if (isPressed) {
                    Modifier.background(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentCyan.copy(alpha = 0.1f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) AccentCyan else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ==================== Create Task Dialog ====================

/**
 * 创建任务对话框组件
 *
 * @param onDismiss 取消回调
 * @param onCreate 创建回调（name, description, schedule）
 */
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }

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
                    text = "创建任务",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 输入框
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 任务名称
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
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = TextPrimary
                            ),
                            decorationBox = { innerTextField ->
                                if (name.isEmpty()) {
                                    Text(
                                        text = "任务名称",
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

                    // 任务描述
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
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = TextPrimary
                            ),
                            decorationBox = { innerTextField ->
                                if (description.isEmpty()) {
                                    Text(
                                        text = "任务描述",
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

                    // 执行时间
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
                            value = schedule,
                            onValueChange = { schedule = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                color = TextPrimary
                            ),
                            decorationBox = { innerTextField ->
                                if (schedule.isEmpty()) {
                                    Text(
                                        text = "执行时间 (cron表达式)",
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
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = TextSecondary
                        )
                    ) {
                        Text(text = "取消")
                    }

                    // 创建按钮
                    Button(
                        onClick = { onCreate(name, description, schedule) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.linearGradient(GradientPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "创建",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Preview ====================

@Composable
fun TaskScreenPreview() {
    val sampleTasks = listOf(
        Task(
            id = "1",
            name = "每日早报提醒",
            description = "每天早上8:00推送新闻摘要和天气信息",
            status = TaskStatus.RUNNING,
            schedule = "每天 08:00"
        ),
        Task(
            id = "2",
            name = "内存清理",
            description = "每6小时清理一次后台应用缓存",
            status = TaskStatus.PAUSED,
            schedule = "每6小时"
        )
    )

    TaskScreen(
        tasks = sampleTasks,
        onTaskClick = {},
        onCreateTask = { _, _, _ -> },
        onPauseTask = {},
        onDeleteTask = {}
    )
}
