# 晨翼Agent 快速开发参考

**快速查找常用代码片段和配置。**

---

## 1. 版本更新检查清单

发布新版本时，修改以下文件：

```bash
# 1. build.gradle.kts
versionCode = 10022  # MAJOR*10000 + MINOR*100 + PATCH
versionName = "1.0.22"

# 2. version.json
{
  "versionCode": 10022,
  "versionName": "1.0.22",
  "apkUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "updateLog": "..."
}

# 3. ChatScreen.kt
text = "v1.0.22 • Kernel Ready"
```

---

## 2. 对话框使用模板

```kotlin
// MainActivity.kt

// 1. 定义状态
var showDialog by remember { mutableStateOf(false) }

// 2. 触发显示
Button(onClick = { showDialog = true }) { Text("打开") }

// 3. 在 Box 内部渲染对话框
Box {
    // 主内容
    Column { ... }
    
    // 对话框（必须在 Box 内部）
    if (showDialog) {
        MyDialog(
            onDismiss = { showDialog = false },
            onConfirm = { 
                showDialog = false
                // 处理确认
            }
        )
    }
}
```

---

## 3. DataStore 配置读写

```kotlin
// ConfigManager.kt

// 定义 Key
private val MY_KEY = stringPreferencesKey("my_key")

// 读取
fun getMyValue(): Flow<String> = context.dataStore.data.map { 
    it[MY_KEY] ?: "default" 
}

// 保存
suspend fun saveMyValue(value: String) {
    context.dataStore.edit { it[MY_KEY] = value }
}

// 使用
LaunchedEffect(Unit) {
    val value = configManager.getMyValue().first()
}
scope.launch {
    configManager.saveMyValue(newValue)
}
```

---

## 4. GitHub Actions 常用命令

```bash
# 查看最近构建
gh run list --limit 5

# 查看失败日志
gh run view <run-id> --log-failed

# 查看特定工作流
gh run list --workflow "Build Android APK"

# 重新运行
gh run rerun <run-id>
```

---

## 5. 常见编译错误

| 错误 | 原因 | 解决 |
|------|------|------|
| `Syntax error: Expecting '}'` | 括号不匹配 | 检查函数/lambda 闭合 |
| `Type mismatch` | 类型不对 | 检查参数类型 |
| `Unresolved reference` | 导入缺失 | 添加 import |
| `Modifier private not allowed` | 顶级函数用 private | 移除 private |

---

## 6. Compose 常用组件

```kotlin
// 渐变背景
Modifier.background(Brush.linearGradient(GradientPrimary))

// 圆形裁剪
Modifier.clip(CircleShape)

// 点击
Modifier.clickable { }

// 填充
Modifier.fillMaxSize()
Modifier.fillMaxWidth()

// 内边距
Modifier.padding(16.dp)

// 间距
Spacer(modifier = Modifier.height(8.dp))
```

---

## 7. Rust JNI 接口

```rust
// jni.rs
#[jni_fn]
pub fn init(mut env: JNIEnv, _class: JClass) -> jlong {
    // 返回内核指针
}

#[jni_fn]
pub fn chat(mut env: JNIEnv, _class: JClass, ptr: jlong, message: JString) -> jstring {
    // 调用内核聊天
}
```

```kotlin
// Kernel.kt
external fun init(): Long
external fun chat(ptr: Long, message: String): String
```

---

## 8. 工具执行流程

```
用户点击工具卡片
    ↓
ToolsScreen.onToolClick(tool)
    ↓
MainActivity 处理
    ↓
ToolExecutionManager.execute(tool)
    ↓
Kernel JNI 调用
    ↓
Rust tools/android/xxx.rs
    ↓
返回结果
    ↓
更新 UI
```

---

## 9. 自动更新流程

```
用户点击"检查更新"
    ↓
UpdateManager.checkForUpdate()
    ↓
请求 version.json
    ↓
比较 versionCode
    ↓
显示更新提示
    ↓
用户确认下载
    ↓
UpdateManager.downloadApk()
    ↓
显示下载进度
    ↓
UpdateManager.installApk()
    ↓
系统安装器
```

---

## 10. 文件路径速查

```
# 核心文件
MainActivity.kt           # 主入口，UI 状态管理
ConfigManager.kt         # DataStore 配置
ConfigValidator.kt       # 配置验证
UpdateManager.kt         # 自动更新

# UI 组件
ChatScreen.kt            # 聊天页面
ToolsScreen.kt           # 工具页面
SettingsScreen.kt        # 设置页面
ConfigDialogs.kt         # 配置对话框
ModernBottomBar.kt       # 底部导航

# Rust 内核
src/lib.rs              # 主入口
src/jni.rs              # JNI 接口
src/agent/              # Agent 循环
src/tools/android/      # Android 工具

# 配置
Cargo.toml              # Rust 配置
build.gradle.kts        # Android 配置
version.json            # 版本信息
```

---

**快速搜索**: Ctrl+F 查找关键词
