# 晨翼Agent v1.0.16 UI 逻辑文档

**生成时间**: 2026-05-25  
**版本**: v1.0.16 (versionCode: 10016)

---

## 一、整体架构

### 1.1 应用架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity                            │
│  - 初始化 Kernel, SessionManager, ScreenshotManager         │
│  - 加载 SharedPreferences 配置                               │
│  - 检查无障碍服务                                            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    FullFeaturedApp                           │
│  - 状态管理: selectedTab (当前标签页)                        │
│  - 底部导航: FullFeatureBottomBar (5 个标签)                 │
│  - 无障碍服务提示对话框                                      │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┬─────────────┐
        │                   │                   │             │
        ▼                   ▼                   ▼             ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ ChatScreen  │   │SessionList  │   │ToolsScreen  │   │ TaskScreen  │
│  (Tab 0)    │   │Screen (Tab 1)│   │  (Tab 2)    │   │  (Tab 3)    │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
        │
        └─────────────┬─────────────┐
                      ▼             ▼
              ┌─────────────┐   ┌─────────────┐
              │SettingsScreen│   │  对话框组件  │
              │  (Tab 4)    │   │ UserProfile  │
              └─────────────┘   └─────────────┘
```

### 1.2 核心组件

| 组件名称 | 类型 | 职责 |
|---------|------|------|
| **MainActivity** | Activity | 应用入口，初始化全局对象 |
| **FullFeaturedApp** | Composable | 主应用框架，标签页切换 |
| **FullFeatureBottomBar** | Composable | 底部导航栏（微信风格） |
| **ChatScreen** | Composable | 聊天界面，消息交互 |
| **SessionListScreen** | Composable | 会话列表管理 |
| **ToolsScreen** | Composable | 工具/技能展示 |
| **TaskScreen** | Composable | 定时任务管理 |
| **SettingsScreen** | Composable | 设置页面 |
| **UserProfileDialog** | Composable | 用户画像编辑 |
| **StreamingMessageBubble** | Composable | 流式消息气泡 |
| **TokenUsageProgressBar** | Composable | Token 使用进度条 |
| **CompressionConfigSection** | Composable | 压缩配置 UI |

---

## 二、页面详细逻辑

### 2.1 MainActivity - 应用入口

**初始化流程:**

```
1. onCreate()
   ├─ Kernel.initLibrary(applicationContext)  // 加载 Rust 动态库
   ├─ 创建 Kernel 实例
   ├─ 创建 SessionManager 实例
   ├─ kernel.init()  // 初始化 Rust 内核
   ├─ 从 SharedPreferences 加载配置
   │  ├─ api_key
   │  ├─ api_endpoint
   │  └─ model_name
   ├─ kernel.setApiKey()  // 设置 API 配置
   └─ setContent { FullFeaturedApp(...) }
```

**全局对象:**

| 对象 | 类型 | 作用域 |
|------|------|--------|
| `prefs` | SharedPreferences | 全局配置存储 |
| `kernel` | Kernel | Rust 内核实例 |
| `sessionManager` | SessionManager | 会话管理器 |
| `screenshotManager` | ScreenshotManager | 截图管理器 |
| `ocrEngine` | OcrEngine | OCR 引擎 |

---

### 2.2 FullFeaturedApp - 主应用框架

**状态管理:**

```kotlin
// 当前选中的标签页
var selectedTab by remember { mutableStateOf(0) }

// 无障碍服务对话框
var showA11yDialog by remember { mutableStateOf(false) }
```

**标签页切换逻辑:**

```kotlin
when (selectedTab) {
    0 -> ChatScreen(kernel, prefs, sessionManager, kernelInitialized)
    1 -> SessionListScreen(sessionManager, onSessionSelected)
    2 -> ToolsScreen(screenshotManager, ocrEngine, kernel)
    3 -> TaskScreen(kernel, prefs)
    4 -> SettingsScreen(prefs, kernel, screenshotManager, kernelInitialized)
}
```

**LaunchedEffect - 无障碍服务检查:**

```kotlin
LaunchedEffect(Unit) {
    if (ChenyiAccessibilityService.getInstance() == null) {
        showA11yDialog = true
    }
}
```

---

### 2.3 ChatScreen - 聊天页面

**状态变量:**

```kotlin
// API 配置
var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }

// 当前会话
var currentSession by remember { mutableStateOf(sessionManager.getOrCreateCurrentSession()) }

// 输入框文本
var inputText by remember { mutableStateOf("") }

// 加载状态
var isLoading by remember { mutableStateOf(false) }

// 消息列表滚动状态
val listState = rememberLazyListState()

// 待发送的消息
var pendingMessage by remember { mutableStateOf<String?>(null) }

// API Key 警告标志
var hasShownApiKeyWarning by remember { mutableStateOf(false) }
```

**发送消息流程:**

```
用户点击发送按钮
    │
    ├─ 检查 API Key 是否为空
    │  └─ 为空 → 显示警告对话框
    │
    ├─ 检查 inputText 是否为空
    │  └─ 为空 → 不发送
    │
    ├─ 添加用户消息到 currentSession.messages
    │
    ├─ 清空输入框
    │
    ├─ 设置 isLoading = true
    │
    ├─ 启动协程（rememberCoroutineScope.launch）
    │  │
    │  ├─ 调用 kernel.chatWithTools(inputText)
    │  │  │
    │  │  ├─ 成功 → result.success
    │  │  │  ├─ 添加 AI 回复到 currentSession.messages
    │  │  │  └─ 更新会话标题（如果是第一条消息）
    │  │  │
    │  │  └─ 失败 → result.error
    │  │     └─ 显示错误消息
    │  │
    │  ├─ 保存会话到 sessionManager
    │  │
    │  └─ 设置 isLoading = false
    │
    └─ 滚动到列表底部
```

**LaunchedEffect - 滚动到底部:**

```kotlin
LaunchedEffect(currentSession.messages.size) {
    if (currentSession.messages.isNotEmpty()) {
        listState.animateScrollToItem(currentSession.messages.size - 1)
    }
}
```

**UI 组件树:**

```
Column
├─ 标题栏
│  └─ Text("晨翼Agent")
├─ TokenUsageProgressBar (显示 Token 使用情况)
├─ LazyColumn (消息列表)
│  └─ items(messages)
│     └─ WeChatMessageBubble / StreamingMessageBubble
├─ Row (输入框区域)
│  ├─ OutlinedTextField (输入框)
│  └─ IconButton (发送按钮)
└─ DisposableEffect (清理协程)
```

---

### 2.4 SessionListScreen - 会话管理页面

**状态变量:**

```kotlin
// 所有会话列表
var sessions by remember { mutableStateOf(sessionManager.getAllSessions()) }

// 搜索关键词
var searchQuery by remember { mutableStateOf("") }

// 删除确认对话框
var showDeleteDialog by remember { mutableStateOf(false) }

// 待删除的会话
var sessionToDelete by remember { mutableStateOf<Session?>(null) }
```

**搜索过滤逻辑:**

```kotlin
val filteredSessions = remember(sessions, searchQuery) {
    if (searchQuery.isBlank()) {
        sessions
    } else {
        sessions.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.messages.any { msg -> msg.content.contains(searchQuery, ignoreCase = true) }
        }
    }
}
```

**会话操作流程:**

```
用户点击会话
    │
    ├─ 调用 onSessionSelected(session)
    │  └─ 切换到聊天页面 (selectedTab = 0)
    │     └─ 加载会话内容
    │
用户长按会话
    │
    ├─ sessionToDelete = session
    ├─ showDeleteDialog = true
    │
    └─ 用户确认删除
       ├─ sessionManager.deleteSession(session.id)
       ├─ sessions = sessionManager.getAllSessions()
       └─ Toast 提示 "会话已删除"
```

**UI 组件树:**

```
Column
├─ Surface (标题栏)
│  ├─ Text("会话管理")
│  └─ OutlinedTextField (搜索框)
├─ LazyColumn (会话列表)
│  └─ items(filteredSessions)
│     └─ SessionItem
│        ├─ Text (会话标题)
│        ├─ Text (最后一条消息)
│        └─ Text (时间戳)
└─ AlertDialog (删除确认对话框)
```

---

### 2.5 ToolsScreen - 技能页面

**工具列表:**

```kotlin
val tools = listOf(
    Triple("截图", "截取当前屏幕") { /* 截图逻辑 */ },
    Triple("OCR 识别", "识别屏幕文字") { /* OCR 逻辑 */ },
    Triple("点击坐标", "点击指定位置") { /* 点击逻辑 */ },
    Triple("滑动屏幕", "滑动操作") { /* 滑动逻辑 */ },
    Triple("输入文本", "输入文字") { /* 输入逻辑 */ },
    Triple("打开应用", "启动应用") { /* 打开应用逻辑 */ },
    Triple("返回桌面", "回到主屏幕") { /* 返回桌面逻辑 */ },
    Triple("获取当前应用", "获取应用信息") { /* 获取应用信息逻辑 */ }
)
```

**工具执行流程:**

```
用户点击工具卡片
    │
    ├─ 截图工具
    │  ├─ 检查无障碍服务
    │  ├─ 调用 screenshotManager.takeScreenshot()
    │  └─ Toast 提示结果
    │
    ├─ OCR 工具
    │  ├─ 检查无障碍服务
    │  ├─ 调用 ocrEngine.recognize()
    │  └─ Toast 提示结果
    │
    └─ 其他工具
       └─ Toast 提示 "功能开发中"
```

**UI 组件树:**

```
Column
├─ Surface (标题栏)
│  └─ Text("工具与技能")
├─ LazyVerticalGrid (工具卡片网格)
│  └─ items(tools)
│     └─ WeChatToolItem
│        ├─ Icon (工具图标)
│        ├─ Text (工具名称)
│        └─ Text (工具描述)
└─ SnackbarHost (Toast 消息)
```

---

### 2.6 TaskScreen - 任务管理页面

**状态变量:**

```kotlin
// 任务列表
var tasks by remember { mutableStateOf(listOf<AutoTask>()) }

// 创建任务对话框
var showCreateDialog by remember { mutableStateOf(false) }

// 任务名称
var taskName by remember { mutableStateOf("") }

// 任务描述
var taskDescription by remember { mutableStateOf("") }

// 任务类型
var taskType by remember { mutableStateOf("定时提醒") }

// 执行时间
var executionTime by remember { mutableStateOf("") }
```

**任务操作流程:**

```
用户点击"创建任务"
    │
    ├─ showCreateDialog = true
    │
    └─ 用户填写任务信息
       ├─ taskName (任务名称)
       ├─ taskDescription (任务描述)
       ├─ taskType (任务类型)
       └─ executionTime (执行时间)
       │
       └─ 用户点击"确定"
          ├─ 创建 AutoTask 对象
          ├─ tasks = tasks + newTask
          └─ showCreateDialog = false

用户点击任务项
    │
    ├─ 暂停/恢复任务
    │  └─ task.status = if (task.status == "运行中") "已暂停" else "运行中"
    │
    └─ 删除任务
       └─ tasks = tasks.filter { it.id != task.id }
```

**UI 组件树:**

```
Column
├─ Surface (标题栏)
│  ├─ Text("任务管理")
│  └─ IconButton (创建任务按钮)
├─ LazyColumn (任务列表)
│  └─ items(tasks)
│     └─ TaskItem
│        ├─ Text (任务名称)
│        ├─ Text (任务描述)
│        ├─ Text (执行时间)
│        ├─ Text (任务状态)
│        └─ Row (操作按钮)
│           ├─ IconButton (暂停/恢复)
│           └─ IconButton (删除)
└─ CreateTaskDialog (创建任务对话框)
```

---

### 2.7 SettingsScreen - 设置页面

**状态变量:**

```kotlin
// API 配置
var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
var apiEndpoint by remember { mutableStateOf(prefs.getString("api_endpoint", "...") ?: "") }
var modelName by remember { mutableStateOf(prefs.getString("model_name", "glm-5") ?: "") }
var showApiKey by remember { mutableStateOf(false) }

// 用户画像对话框
var showUserProfileDialog by remember { mutableStateOf(false) }

// 压缩配置
var compressionEnabled by remember { mutableStateOf(prefs.getBoolean("compression_enabled", true)) }
var compressionThreshold by remember { mutableStateOf(prefs.getInt("compression_threshold", 4000)) }
var compressionTargetRatio by remember { mutableStateOf(prefs.getFloat("compression_target_ratio", 0.5f)) }
```

**设置保存流程:**

```
用户修改设置项
    │
    ├─ API 配置修改
    │  ├─ prefs.edit().putString("api_key", apiKey).apply()
    │  ├─ prefs.edit().putString("api_endpoint", apiEndpoint).apply()
    │  ├─ prefs.edit().putString("model_name", modelName).apply()
    │  └─ kernel.setApiKey(apiKey, apiEndpoint, modelName)
    │
    ├─ 压缩配置修改
    │  ├─ prefs.edit().putBoolean("compression_enabled", compressionEnabled).apply()
    │  ├─ prefs.edit().putInt("compression_threshold", compressionThreshold).apply()
    │  └─ prefs.edit().putFloat("compression_target_ratio", compressionTargetRatio).apply()
    │
    └─ 用户画像修改
       └─ 显示 UserProfileDialog
```

**UI 组件树:**

```
Column
├─ Surface (标题栏)
│  └─ Text("设置")
├─ Column (可滚动)
│  ├─ SettingsSection ("API 配置")
│  │  ├─ SettingsItem (API Key 输入框)
│  │  ├─ SettingsItem (API Endpoint 输入框)
│  │  └─ SettingsItem (Model Name 输入框)
│  │
│  ├─ SettingsSection ("用户画像")
│  │  └─ SettingsItem (编辑用户画像按钮)
│  │     └─ onClick -> showUserProfileDialog = true
│  │
│  ├─ CompressionConfigSection (压缩配置)
│  │  ├─ Switch (启用压缩)
│  │  ├─ Slider (压缩阈值)
│  │  └─ Slider (目标比例)
│  │
│  ├─ SettingsSection ("无障碍服务")
│  │  └─ SettingsItem (跳转到系统设置)
│  │
│  └─ SettingsSection ("关于")
│     ├─ SettingsItem (版本信息)
│     └─ SettingsItem (GitHub 链接)
│
└─ UserProfileDialog (用户画像对话框)
```

---

## 三、关键功能流程

### 3.1 发送消息流程

```
用户输入消息 → 点击发送
    ↓
检查 API Key (为空则提示)
    ↓
添加用户消息到会话
    ↓
清空输入框，设置 isLoading = true
    ↓
协程启动 → kernel.chatWithTools(inputText)
    ↓
Rust 内核处理:
    ├─ memory.get_context() → 获取相关记忆
    ├─ compressor.needs_compression() → 检查是否需要压缩
    │  └─ 需要压缩 → LLM 压缩历史
    ├─ LLM chat_with_tools(消息+记忆)
    └─ memory.add_message() → 存储对话
    ↓
返回 Result → 添加 AI 回复到会话
    ↓
isLoading = false → 滚动到底部
```

### 3.2 工具执行流程

```
用户发送 "帮我点击(100,200)"
    ↓
kernel.chatWithTools() → Rust 分析意图
    ↓
返回 TOOL_CALLS_REQUIRED: tap(100,200)
    ↓
Kernel 解析工具调用
    ↓
A11yService.executeTool("tap", {x:100, y:200})
    ↓
执行点击操作 → 返回结果
    ↓
nativeSubmitToolResults(结果) → Rust 继续对话
    ↓
返回最终响应 → 显示 AI 回复
```

### 3.3 会话管理流程

```
打开应用 → SessionManager.getOrCreateCurrentSession
    ↓
当前会话不存在? → 创建新会话
    ↓
显示聊天页面
    ↓
用户操作:
    ├─ 发送消息 → 添加到会话 → 保存到 SQLite
    ├─ 切换会话 → 加载选中会话
    ├─ 新建会话 → 生成唯一 ID → 设为当前会话
    └─ 删除会话 → 从 SQLite 删除 → 刷新列表
```

---

## 四、与后端 Kernel 的交互

### 4.1 初始化流程

```kotlin
// 1. 加载 Rust 动态库
Kernel.initLibrary(applicationContext)

// 2. 创建 Kernel 实例
val kernel = Kernel(applicationContext)

// 3. 初始化 Rust 内核
kernel.init()  // 调用 nativeInit()

// 4. 设置 API Key
kernel.setApiKey(apiKey, apiEndpoint, modelName)  // 调用 nativeSetApiKey()
```

### 4.2 JNI 方法映射

| Kotlin 方法 | JNI 方法 | Rust 实现 | 说明 |
|------------|---------|-----------|------|
| `init()` | `nativeInit(dataDir)` | `fn init()` | 初始化内核 |
| `setApiKey()` | `nativeSetApiKey(...)` | `fn set_api_key()` | 设置 API 配置 |
| `chat()` | `nativeChat(message)` | `fn chat()` | 发送消息 |
| `chatWithTools()` | `nativeChat(message)` | `fn chat_with_tools()` | 带工具支持的消息 |
| `executeTool()` | `nativeExecuteTool(...)` | `fn execute_tool()` | 执行工具 |
| `getStatus()` | `nativeGetStatus()` | `fn get_status()` | 获取内核状态 |
| `clearHistory()` | `nativeClearHistory()` | `fn clear_history()` | 清除历史 |
| `chatStream()` | `nativeChatStream(...)` | `fn chat_stream()` | 流式输出 (v1.0.16 新增) |
| `getCompressionStats()` | `nativeGetCompressionStats()` | `fn compression_stats()` | 压缩统计 (v1.0.16 新增) |

### 4.3 数据流向

```
UI 层 (Compose)
    ↓ 用户操作
Kotlin 层 (Kernel)
    ↓ JNI 调用
Rust 层 (AgentKernel)
    ├─ LLM
    ├─ Memory
    ├─ Compressor
    └─ Tools
    ↓ HTTP 请求
LLM API (OpenAI 兼容)
```

---

## 五、状态管理模式

### 5.1 全局状态

**SharedPreferences:**
- `api_key`: API Key
- `api_endpoint`: API 端点
- `model_name`: 模型名称
- `compression_enabled`: 压缩开关
- `compression_threshold`: 压缩阈值
- `compression_target_ratio`: 目标压缩比例

**SessionManager (SQLite):**
- 所有会话列表
- 每个会话的消息历史
- 会话元数据（标题、创建时间等）

### 5.2 局部状态（remember）

**每个页面独立管理:**
- ChatScreen: `currentSession`, `inputText`, `isLoading`
- SessionListScreen: `sessions`, `searchQuery`
- TaskScreen: `tasks`, `showCreateDialog`
- SettingsScreen: `apiKey`, `apiEndpoint`, `modelName`

### 5.3 状态提升策略

**原则:**
1. **局部状态**: 仅在当前 Composable 使用 → 使用 `remember`
2. **共享状态**: 多个 Composable 共享 → 提升到父 Composable 或 ViewModel
3. **持久化状态**: 需要跨会话保存 → 使用 SharedPreferences 或 SQLite

---

## 六、协程使用

### 6.1 LaunchedEffect

**用途:** 副作用处理，如初始化、订阅、定时任务

```kotlin
// 示例1：检查无障碍服务
LaunchedEffect(Unit) {
    if (ChenyiAccessibilityService.getInstance() == null) {
        showA11yDialog = true
    }
}

// 示例2：滚动到列表底部
LaunchedEffect(currentSession.messages.size) {
    if (currentSession.messages.isNotEmpty()) {
        listState.animateScrollToItem(currentSession.messages.size - 1)
    }
}
```

### 6.2 rememberCoroutineScope

**用途:** 在事件处理器中启动协程

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        isLoading = true
        val result = kernel.chatWithTools(inputText)
        // 更新 UI
        isLoading = false
    }
}) {
    Text("发送")
}
```

### 6.3 withContext

**用途:** 切换协程上下文（线程）

```kotlin
scope.launch {
    // 在 IO 线程执行网络请求
    val result = withContext(Dispatchers.IO) {
        kernel.chatWithTools(inputText)
    }
    
    // 在主线程更新 UI
    withContext(Dispatchers.Main) {
        currentSession.messages.add(result)
    }
}
```

---

## 七、v1.0.16 新增功能

### 7.1 流式输出 (StreamCallback)

**接口定义:**

```kotlin
interface StreamCallback {
    fun onToken(token: String)     // 接收到一个 token
    fun onComplete(response: String) // 流式输出完成
    fun onError(error: String)      // 发生错误
}
```

**使用方式:**

```kotlin
// 协程版本
suspend fun chatStreamSuspend(
    message: String,
    onToken: (String) -> Unit
): Result = suspendCoroutine { continuation ->
    val callback = SuspendStreamCallback(continuation, onToken)
    nativeChatStream(message, callback)
}

// UI 中使用
scope.launch {
    kernel.chatStreamSuspend(message) { token ->
        // 实时更新 UI
        currentMessage += token
    }
}
```

### 7.2 压缩统计 (CompressionStats)

**数据类:**

```kotlin
data class CompressionStats(
    val totalCompressions: Long,   // 总压缩次数
    val tokensSaved: Long,         // 节省的 token 数
    val originalTokens: Long,      // 原始 token 数
    val compressedTokens: Long     // 压缩后 token 数
)
```

**使用方式:**

```kotlin
val stats = kernel.getCompressionStats()
Log.d("Compression", "节省了 ${stats.tokensSaved} tokens")
```

### 7.3 Token 使用进度条

**UI 组件:**

```kotlin
@Composable
fun TokenUsageProgressBar(
    currentTokens: Int,
    maxTokens: Int = 4096,
    threshold: Int = 4000
) {
    val progress = currentTokens.toFloat() / maxTokens
    val color = when {
        currentTokens > threshold -> Color.Red
        currentTokens > threshold * 0.8 -> Color.Yellow
        else -> Color.Green
    }
    
    LinearProgressIndicator(
        progress = progress,
        color = color,
        modifier = Modifier.fillMaxWidth()
    )
}
```

---

## 八、注意事项

### 8.1 内存管理

1. **避免内存泄漏**: 使用 `DisposableEffect` 清理资源
2. **协程取消**: 使用 `CoroutineScope` 的结构化并发
3. **Bitmap 回收**: 截图后及时回收 Bitmap

### 8.2 线程安全

1. **UI 更新**: 只在主线程（Dispatchers.Main）更新 UI
2. **网络请求**: 在 IO 线程（Dispatchers.IO）执行
3. **Rust 回调**: JNI 回调在主线程执行

### 8.3 生命周期管理

1. **Activity 重建**: 使用 `rememberSaveable` 保存状态
2. **配置变更**: 处理屏幕旋转等配置变更
3. **后台返回**: 恢复会话状态

---

## 九、性能优化建议

### 9.1 Compose 优化

1. **避免过度重组**: 使用 `key()` 优化 LazyColumn
2. **使用 `derivedStateOf`**: 减少不必要的状态更新
3. **延迟加载**: 使用 `LazyColumn` 代替 `Column + Modifier.verticalScroll`

### 9.2 数据优化

1. **分页加载**: 会话列表分页显示
2. **缓存策略**: 缓存常用数据
3. **数据库优化**: 添加索引，使用事务

### 9.3 网络优化

1. **请求合并**: 批量请求
2. **超时设置**: 合理设置超时时间
3. **重试机制**: 指数退避重试

---

**文档版本**: v1.0.16  
**最后更新**: 2026-05-25
