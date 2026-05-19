package com.chenyi.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 主 Activity
 */
class MainActivity : ComponentActivity() {

    private lateinit var kernel: Kernel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化内核
        kernel = Kernel(this)
        val initialized = kernel.init()

        setContent {
            ChenYiTheme {
                MainScreen(
                    kernelInitialized = initialized,
                    kernel = kernel
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        kernel.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    kernelInitialized: Boolean,
    kernel: Kernel
) {
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("晨翼 Agent") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val message = inputText
                                inputText = ""
                                messages = messages + Message("user", message)
                                isLoading = true

                                scope.launch {
                                    try {
                                        // 在后台线程执行 JNI 调用
                                        val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            kernel.chat(message)
                                        }
                                        isLoading = false
                                        if (result.success) {
                                            val response = result.data?.get("response") as? String ?: "无响应"
                                            messages = messages + Message("assistant", response)
                                        } else {
                                            snackbarHostState.showSnackbar(result.error ?: "未知错误")
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        snackbarHostState.showSnackbar("错误: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = kernelInitialized && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
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
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(2.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = if (kernelInitialized) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {}
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (kernelInitialized) "内核已就绪" else "内核未初始化",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 消息列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageCard(message)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: Message) {
    val isUser = message.role == "user"

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
            Text(
                text = if (isUser) "你" else "晨翼",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

data class Message(
    val role: String,
    val content: String
)

@Composable
fun ChenYiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}