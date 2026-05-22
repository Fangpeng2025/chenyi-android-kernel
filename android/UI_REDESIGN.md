# 晨翼Agent Android UI 重构方案

## 设计原则
1. **Material Design 3** - 现代、简洁、符合 Android 17 规范
2. **国内风格** - 参考微信、支付宝等国内主流APP
3. **绿色主题** - 主色调 #07C160（微信绿）
4. **简洁易用** - 减少层级，核心功能直达

## 架构设计

### 1. 底部导航栏（4个Tab）
```
┌─────────────────────────────────────┐
│  聊天  │  工具  │  设置  │  我      │
└─────────────────────────────────────┘
```

### 2. 各Tab功能

#### Tab 1: 聊天（ChatScreen）
- **会话列表** - 显示历史会话
- **消息气泡** - 用户绿色，AI白色
- **输入框** - 底部固定，支持语音/文字
- **工具执行可视化** - 显示工具调用状态

#### Tab 2: 工具（ToolsScreen）
- **常用工具** - 截图、OCR、自动化
- **MCP工具** - 从内核动态加载
- **工具详情** - 参数说明、使用示例

#### Tab 3: 设置（SettingsScreen）
- **API配置** - API Key、模型选择
- **权限管理** - 无障碍、截图权限
- **自动更新** - 检查更新、下载APK
- **高级设置** - 日志、调试模式

#### Tab 4: 我（ProfileScreen）
- **用户信息** - 版本、设备信息
- **使用统计** - 消息数、工具调用次数
- **关于** - 官网、GitHub、反馈

## 技术栈

### UI框架
- **Jetpack Compose** - 声明式UI
- **Material Design 3** - 组件库
- **Compose Navigation** - 导航

### 核心组件
```kotlin
// 主题
Theme.kt - 绿色主题 #07C160
Typography.kt - 字体系统
Shapes.kt - 圆角系统

// UI组件
MainActivity.kt - 主Activity
ChatScreen.kt - 聊天界面
ToolsScreen.kt - 工具界面
SettingsScreen.kt - 设置界面
ProfileScreen.kt - 个人中心
```

### 数据流
```
Rust内核 (FFI)
    ↓
Kernel.kt (JNI封装)
    ↓
ViewModel (状态管理)
    ↓
Compose UI (界面渲染)
```

## 自动更新集成

### 版本检查流程
```
启动APP
  ↓
检查阿里云 version.json
  ↓
对比本地版本
  ↓
显示更新对话框
  ↓
下载APK (进度条)
  ↓
安装APK
```

### 更新UI设计
```kotlin
// 设置页面 - 自动更新卡片
Card {
    Row {
        Icon(Icons.Default.Update)
        Column {
            Text("检查更新")
            Text("当前版本: $version")
        }
        Button("检查") { checkUpdate() }
    }
}

// 更新对话框
AlertDialog {
    Title("发现新版本 $newVersion")
    Text(releaseNotes)
    Button("立即更新") { downloadApk() }
    Button("稍后提醒") { dismiss() }
}

// 下载进度对话框
AlertDialog {
    Title("正在下载...")
    LinearProgressIndicator(progress = progress)
    Text("$progress%")
}
```

## 颜色系统

### 主色调（微信绿）
```kotlin
val WeChatGreen = Color(0xFF07C160)
val WeChatGreenLight = Color(0xFF4CAF50)
val WeChatGreenDark = Color(0xFF059648)

// 消息气泡
val UserBubble = WeChatGreen
val AIBubble = Color.White

// 背景
val Background = Color(0xFFEDEDED)
```

## 组件规范

### 圆角
```kotlin
// 气泡圆角
RoundedCornerShape(8.dp)

// 卡片圆角
RoundedCornerShape(12.dp)

// 按钮圆角
RoundedCornerShape(24.dp)
```

### 间距
```kotlin
// 页面边距
padding(16.dp)

// 列表项间距
spacer(8.dp)

// 卡片内边距
padding(16.dp)
```

### 字体
```kotlin
// 标题
fontSize = 20.sp
fontWeight = FontWeight.Bold

// 正文
fontSize = 16.sp
fontWeight = FontWeight.Normal

// 辅助文字
fontSize = 14.sp
color = Color.Gray
```

## 性能优化

### 1. 懒加载
```kotlin
LazyColumn {
    items(messages) { message ->
        MessageBubble(message)
    }
}
```

### 2. 状态管理
```kotlin
// 使用 ViewModel
hiltViewModel<ChatViewModel>()

// 使用 remember
var text by remember { mutableStateOf("") }
```

### 3. 协程
```kotlin
LaunchedEffect(Unit) {
    // 初始化操作
    kernel.init()
}
```

## 无障碍支持

### 1. 内容描述
```kotlin
Icon(
    imageVector = Icons.Default.Send,
    contentDescription = "发送消息"
)
```

### 2. 最小点击区域
```kotlin
Modifier.size(48.dp) // 最小 48dp
```

### 3. 焦点管理
```kotlin
FocusRequester.createRefs()
```

## 下一步实施

### Phase 1: 优化现有UI ✅
- 改进聊天气泡样式
- 优化输入框交互
- 添加工具执行可视化

### Phase 2: 集成自动更新 ⏳
- 在设置页面添加"检查更新"卡片
- 实现更新对话框
- 实现下载进度显示
- 实现APK安装

### Phase 3: 完善细节 ⏳
- 添加加载动画
- 优化错误提示
- 添加空状态页面

### Phase 4: 测试优化 ⏳
- Android 17 兼容性测试
- 性能优化
- 内存泄漏检查
