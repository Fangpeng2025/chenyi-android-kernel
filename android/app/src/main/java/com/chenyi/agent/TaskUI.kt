package com.chenyi.agent

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 任务列表面板
 */
@Composable
fun TaskPanel(
    taskManager: TaskManager,
    onExecute: (AutoTask) -> Unit,
    onCancel: () -> Unit,
    isRunning: Boolean,
    currentProgress: Triple<Int, Int, String>?
) {
    var tasks by remember { mutableStateOf(taskManager.getAllTasks()) }
    var showTemplates by remember { mutableStateOf(false) }
    var showCreateTask by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "自动任务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // 模板按钮
            OutlinedButton(
                onClick = { showTemplates = !showTemplates },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("模板", style = MaterialTheme.typography.labelMedium)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 新建按钮
            OutlinedButton(
                onClick = { showCreateTask = !showCreateTask },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("新建", style = MaterialTheme.typography.labelMedium)
            }
        }
        
        // 执行进度
        if (isRunning && currentProgress != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "正在执行...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${currentProgress.first}/${currentProgress.second}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = if (currentProgress.second > 0) 
                            currentProgress.first.toFloat() / currentProgress.second else 0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = currentProgress.third,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("取消执行", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        
        // 模板列表
        if (showTemplates) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "任务模板",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TaskTemplates.getAllTemplates().forEach { template ->
                        TemplateItem(
                            name = template.name,
                            description = template.description,
                            stepCount = template.steps.size,
                            onClick = {
                                val task = taskManager.createFromTemplate(template)
                                tasks = taskManager.getAllTasks()
                                showTemplates = false
                            }
                        )
                    }
                }
            }
        }
        
        // 任务列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                TaskCard(
                    task = task,
                    onExecute = { onExecute(task) },
                    onDelete = {
                        taskManager.deleteTask(task.id)
                        tasks = taskManager.getAllTasks()
                    }
                )
            }
            
            if (tasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无任务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "从模板创建或新建任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 任务卡片
 */
@Composable
fun TaskCard(
    task: AutoTask,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (task.status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusIcon = when (task.status) {
        TaskStatus.PENDING -> Icons.Default.Schedule
        TaskStatus.RUNNING -> Icons.Default.PlayArrow
        TaskStatus.COMPLETED -> Icons.Default.CheckCircle
        TaskStatus.FAILED -> Icons.Default.Error
        TaskStatus.CANCELLED -> Icons.Default.Cancel
    }
    
    val statusText = when (task.status) {
        TaskStatus.PENDING -> "待执行"
        TaskStatus.RUNNING -> "执行中"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.FAILED -> "失败"
        TaskStatus.CANCELLED -> "已取消"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 状态标签
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 描述
            if (task.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 步骤预览
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.steps.size} 个步骤",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 步骤进度点
                Spacer(modifier = Modifier.width(8.dp))
                task.steps.take(8).forEach { step ->
                    val dotColor = when (step.status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.outline
                        StepStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        StepStatus.COMPLETED -> Color(0xFF4CAF50)
                        StepStatus.FAILED -> MaterialTheme.colorScheme.error
                    }
                    Surface(
                        modifier = Modifier.size(8.dp),
                        color = dotColor,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {}
                    Spacer(modifier = Modifier.width(3.dp))
                }
                if (task.steps.size > 8) {
                    Text(
                        text = "+${task.steps.size - 8}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 操作按钮
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (task.status == TaskStatus.PENDING) {
                    OutlinedButton(
                        onClick = onExecute,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("执行", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 模板项
 */
@Composable
fun TemplateItem(
    name: String,
    description: String,
    stepCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$description · $stepCount 步",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * 快捷指令面板
 */
@Composable
fun QuickActionsPanel(
    onAction: (String) -> Unit
) {
    Column {
        Text(
            text = "快捷指令",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton("📸 截图", "screenshot", onAction, Modifier.weight(1f))
            QuickActionButton("🔍 OCR", "ocr", onAction, Modifier.weight(1f))
            QuickActionButton("📱 应用", "listApps", onAction, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 第二行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton("🏠 主页", "home", onAction, Modifier.weight(1f))
            QuickActionButton("⬅️ 返回", "back", onAction, Modifier.weight(1f))
            QuickActionButton("📋 任务", "tasks", onAction, Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    action: String,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = { onAction(action) },
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
