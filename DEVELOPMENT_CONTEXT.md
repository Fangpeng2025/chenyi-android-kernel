# 晨翼Agent 开发上下文文档

**本文档用于 AI 助手开发时注入上下文，保持开发连续性。**

---

## 当前版本状态

**版本**: v1.0.21  
**构建状态**: ✅ 成功  
**最后更新**: 2026-05-26

---

## 项目核心信息

### 基本信息
- **项目名称**: 晨翼Agent Android
- **GitHub**: https://github.com/Fangpeng2025/chenyi-android-kernel
- **官网**: https://xintiandi.online
- **APK下载**: https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
- **服务器**: root@8.147.232.175

### 架构
```
Kotlin UI (Compose) --JNI--> Rust Kernel
     ↓                         ↓
DataStore              SQLite + Tools
     ↓                         ↓
用户配置/画像           Agent循环/LLM调用
```

### 技术栈
- **Android**: Kotlin 2.0 + Compose + Material 3 + DataStore + Coil
- **Rust**: Tokio + serde + rusqlite + reqwest (rustls-tls)
- **构建**: GitHub Actions + gradle + cargo

### 版本号规则
```kotlin
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
// 例: 1.0.21 = 10021
```

---

## 最近修改记录

### 2026-05-26: v1.0.21 - 修复对话框不显示

**问题**: 
用户反馈"API配置和用户画像点击后没有弹窗"

**根本原因**:
```kotlin
// ❌ 错误: 对话框在 Box 外部，不会渲染
Box { ... }
if (showApiKeyDialog) { ApiKeyDialog(...) }
if (showUserProfileDialog) { UserProfileDialog(...) }

// ✅ 修复: 对话框必须在 Box 内部
Box {
    // ... 主内容
    
    // 对话框层
    if (showApiKeyDialog) { ApiKeyDialog(...) }
    if (showUserProfileDialog) { UserProfileDialog(...) }
}
```

**修改文件**:
- `MainActivity.kt`: 将所有对话框移到 Box 组件内部

**教训**:
> Compose 对话框必须作为 Box 的子组件，否则不会渲染

---

### 2026-05-26: v1.0.20 - 数据持久化

**新增功能**:
1. **ConfigManager.kt** - DataStore 配置管理
2. **ConfigValidator.kt** - API 配置验证
3. **用户画像增强** - 头像、语言、风格、专业领域

**关键代码**:
```kotlin
// ConfigManager 使用 Flow 异步加载
fun getApiKey(): Flow<String?>
fun getUserProfile(): Flow<UserProfile>

// MainActivity 中加载
LaunchedEffect(Unit) {
    apiKey = configManager.getApiKey().first()
    userProfile = configManager.getUserProfile().first()
}
```

**注意事项**:
- DataStore 是异步的，必须用 `Flow.first()` 或 `collect`
- 用户画像首次加载时可能是空值

---

### 2026-05-25: v1.0.19 - 微信风格底部导航

**修改**:
- 底部导航改为 3 标签：聊天、工具、设置
- 移除"我"页面，合并到设置页面

**文件**:
- `ModernBottomBar.kt`: 微信风格导航栏
- `SettingsScreen.kt`: 新增用户画像入口、关于、检查更新

---

## 关键文件说明

### 1. MainActivity.kt
**职责**: 主入口，UI 状态管理，对话框控制

**关键状态**:
```kotlin
// 对话框状态
var showApiKeyDialog by remember { mutableStateOf(false) }
var showUserProfileDialog by remember { mutableStateOf(false) }

// 配置状态（从 DataStore 加载）
var apiKey by remember { mutableStateOf<String?>(null) }
var userProfile by remember { mutableStateOf<UserProfile?>(null) }
```

**重要**:
- 对话框代码必须在 `Box { ... }` 内部
- 使用 `LaunchedEffect(Unit)` 加载初始配置

---

### 2. ConfigManager.kt
**职责**: DataStore 配置持久化

**Keys**:
```kotlin
object Keys {
    // API 配置
    val API_KEY = stringPreferencesKey("api_key")
    val API_ENDPOINT = stringPreferencesKey("api_endpoint")
    val MODEL_NAME = stringPreferencesKey("model_name")
    
    // 用户画像
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_ROLE = stringPreferencesKey("user_role")
    val USER_TIMEZONE = stringPreferencesKey("user_timezone")
    // ...
}
```

**使用方式**:
```kotlin
// 读取
val apiKey = configManager.getApiKey().first()

// 保存
scope.launch {
    configManager.saveApiKey(newKey)
}
```

---

### 3. ConfigDialogs.kt
**职责**: 配置对话框组件

**组件列表**:
- `ApiKeyDialog`: API Key 输入，带格式验证
- `ApiEndpointDialog`: API 端点配置，带连接测试
- `ModelNameDialog`: 模型选择
- `UserProfileDialog`: 用户画像编辑

**注意**:
- 对话框使用 `Dialog` 组件，不是 `AlertDialog`
- 需要传入 `validator` 参数进行验证

---

### 4. SettingsScreen.kt
**职责**: 设置页面 UI

**回调参数**:
```kotlin
onApiKeyClick: () -> Unit          // 弹出 API Key 对话框
onUserProfileClick: () -> Unit     // 弹出用户画像对话框
onCompressionToggle: (Boolean) -> Unit
onCheckUpdate: () -> Unit
```

---

### 5. UpdateManager.kt
**职责**: 自动更新功能

**流程**:
1. `checkForUpdate()` - 检查服务器 version.json
2. `downloadApk()` - 下载 APK 并显示进度
3. `installApk()` - 调用系统安装器

**version.json 格式**:
```json
{
  "versionCode": 10021,
  "versionName": "1.0.21",
  "apkUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "updateLog": "..."
}
```

---

## 构建与部署

### GitHub Actions 工作流

**触发**: 推送到 master 分支

**步骤**:
1. **Test Build** - 编译检查（~35s）
2. **Build Android APK** - 构建 APK（~7min）
3. **Deploy to Server** - SSH 部署到服务器（~6min）
4. **Create Release** - 创建 GitHub Release

**查看状态**:
```bash
gh run list --limit 5
```

**本地构建**:
```bash
cd android
./gradlew assembleRelease
```

---

## 常见问题排查

### 1. 对话框不显示
**检查**:
- 对话框代码是否在 `Box { }` 内部
- `showXxxDialog` 状态是否正确设置
- 对话框组件是否正确导入

### 2. 配置不保存
**检查**:
- 是否在协程中调用 `scope.launch { }`
- DataStore 是否正确初始化
- 是否有写入权限

### 3. 自动更新失败
**检查**:
- version.json 中的 versionCode 是否大于当前版本
- APK URL 是否可访问
- 是否有存储权限

### 4. 构建失败
**检查**:
```bash
# 查看构建日志
gh run view <run-id> --log-failed

# 常见错误
- Kotlin 语法错误: 检查括号匹配、导入
- Rust 编译错误: 检查交叉编译配置
```

---

## 开发规范

### 1. 版本号更新
修改以下文件：
```bash
android/app/build.gradle.kts  # versionCode, versionName
version.json                   # versionCode, versionName, updateLog
ChatScreen.kt                  # 版本号显示
```

### 2. 提交规范
```bash
git commit -m "type: 描述

type:
- feat: 新功能
- fix: 修复 bug
- docs: 文档
- refactor: 重构
- style: 格式
"
```

### 3. 代码风格
- Kotlin: 遵循 Kotlin 编码规范
- Compose: 使用 Material 3 组件
- 命名: 驼峰命名，函数用动词开头

---

## 待办事项

### 高优先级
- [ ] 聊天功能与 Rust 内核集成
- [ ] 上下文压缩功能实现
- [ ] 视觉分析功能（截图 + OCR + 分析）

### 中优先级
- [ ] 语音 STT/TTS 功能
- [ ] 技能语义匹配
- [ ] 会话管理完善

### 低优先级
- [ ] UI 细节优化
- [ ] 性能优化
- [ ] 测试覆盖

---

## 用户画像 (USER.md)

**偏好**:
- 修复向前不回滚，而是修复新代码
- 生产级，不自作主张简化
- 系统性对比再实现
- 不能因为工作量大就不完善

**禁忌**:
- ❌ 擅自改技术方案
- ❌ 简化功能实现
- ❌ 回滚到旧版本

---

## 下次开发注入

将本文档内容作为系统提示词的一部分，让 AI 助手了解：
1. 当前项目状态和版本
2. 最近修改的内容和原因
3. 关键文件的作用和使用方式
4. 常见问题的解决方案
5. 开发规范和用户偏好

**使用方式**:
```
加载 DEVELOPMENT_CONTEXT.md 作为上下文，然后开始开发任务...
```

---

**文档更新时间**: 2026-05-26  
**维护者**: ChenYi Team
