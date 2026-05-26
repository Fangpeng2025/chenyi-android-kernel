# 晨翼Agent Android 项目整理报告

**版本**: v1.0.21  
**更新日期**: 2026-05-26  
**GitHub**: https://github.com/Fangpeng2025/chenyi-android-kernel  
**官网**: https://xintiandi.online  
**下载**: https://oneapi.xintiandi.online/chenyi-agent/

---

## 一、项目架构

```
┌─────────────────────────────────────┐
│     Kotlin UI Layer (Compose)       │
│  ┌─────────────────────────────────┐│
│  │  MainActivity.kt                ││
│  │  ├── MainAppContent()           ││
│  │  │   ├── ChatScreen             ││
│  │  │   ├── ToolsScreen            ││
│  │  │   └── SettingsScreen         ││
│  │  └── Dialogs (API/用户画像等)    ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
                    │ JNI
                    ▼
┌─────────────────────────────────────┐
│         Rust Kernel Layer           │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │  Agent  │ │  Memory │ │   LLM  │ │
│  │  Loop   │ │  Engine │ │ Client │ │
│  └─────────┘ └─────────┘ └────────┘ │
│  ┌─────────────────────────────────┐│
│  │      Android Native Tools       ││
│  │ 截图 | 点击 | 滑动 | 输入 | OCR ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────┐
│    Android System Services          │
│  AccessibilityService | 原生 API    │
└─────────────────────────────────────┘
```

---

## 二、目录结构

```
chenyi-android-kernel/
├── Cargo.toml                    # Rust 项目配置
├── version.json                  # 版本信息
├── README.md                     # 项目说明
│
├── src/                          # Rust 内核源码
│   ├── lib.rs                    # 主入口
│   ├── jni.rs                    # JNI 接口
│   ├── types.rs                  # 类型定义
│   │
│   ├── agent/                    # Agent 核心
│   │   ├── mod.rs
│   │   └── orchestrator.rs       # 编排器
│   │
│   ├── memory/                   # 记忆引擎
│   │   └── mod.rs
│   │
│   ├── llm/                      # LLM 客户端
│   │   └── mod.rs
│   │
│   ├── storage/                  # SQLite 存储
│   │   └── mod.rs
│   │
│   ├── skills/                   # 技能系统
│   │   ├── mod.rs
│   │   ├── loader.rs
│   │   ├── matcher.rs
│   │   ├── registry.rs
│   │   └── skill.rs
│   │
│   ├── mcp/                      # MCP 协议
│   │   ├── mod.rs
│   │   ├── client.rs
│   │   ├── discovery.rs
│   │   ├── protocol.rs
│   │   └── transport.rs
│   │
│   ├── compression/              # 上下文压缩
│   │   └── mod.rs
│   │
│   ├── config/                   # 配置管理
│   │   └── mod.rs
│   │
│   └── tools/                    # 工具系统
│       ├── mod.rs
│       └── android/              # 安卓原生工具
│           ├── mod.rs
│           ├── app.rs            # 应用管理
│           ├── input.rs          # 输入操作
│           ├── ocr.rs            # OCR 识别
│           └── screen.rs         # 截图/屏幕
│
├── android/                      # Android 项目
│   ├── build.gradle.kts
│   └── app/
│       ├── build.gradle.kts      # APP 配置
│       └── src/main/
│           ├── AndroidManifest.xml
│           └── java/com/chenyi/agent/
│               │
│               ├── MainActivity.kt        # 主 Activity
│               ├── Kernel.kt              # JNI 桥接层
│               ├── ChenyiAccessibilityService.kt  # 无障碍服务
│               │
│               ├── data/                  # 数据层
│               │   ├── ConfigManager.kt  # 配置管理 (DataStore)
│               │   └── ConfigValidator.kt # 配置验证
│               │
│               ├── ui/                    # UI 层
│               │   ├── theme/            # 主题
│               │   │   ├── Color.kt
│               │   │   ├── Theme.kt
│               │   │   ├── Shape.kt
│               │   │   └── Type.kt
│               │   │
│               │   └── components/        # 组件
│               │       ├── ChatScreen.kt         # 聊天页面
│               │       ├── ToolsScreen.kt        # 工具页面
│               │       ├── SettingsScreen.kt     # 设置页面
│               │       ├── SessionListScreen.kt  # 会话列表
│               │       ├── TaskScreen.kt         # 任务页面
│               │       ├── ModernBottomBar.kt    # 底部导航栏
│               │       ├── ParticleBackground.kt  # 粒子背景
│               │       ├── ConfigDialogs.kt      # 配置对话框
│               │       └── AboutDialog.kt        # 关于对话框
│               │
│               ├── Session.kt             # 会话模型
│               ├── SessionManager.kt      # 会话管理
│               ├── SessionExtensions.kt   # 会话扩展
│               ├── TaskManager.kt         # 任务管理
│               ├── UpdateManager.kt       # 更新管理
│               ├── ScreenshotManager.kt   # 截图管理
│               ├── ToolExecutionManager.kt # 工具执行
│               └── AutoTask.kt            # 自动任务
│
├── .github/workflows/            # GitHub Actions
│   ├── build.yml                 # 构建 APK
│   └── deploy.yml                # 部署到服务器
│
└── scripts/                      # 编译脚本
    ├── build-android.sh
    └── build-android.bat
```

---

## 三、核心功能模块

### 1. UI 层 (Kotlin/Compose)

| 模块 | 文件 | 功能 |
|------|------|------|
| **聊天页面** | `ChatScreen.kt` | 消息列表、输入框、Token 使用量显示 |
| **工具页面** | `ToolsScreen.kt` | 20 个工具卡片（截图、OCR、点击等） |
| **设置页面** | `SettingsScreen.kt` | API 配置、压缩设置、用户画像入口、关于 |
| **会话管理** | `SessionListScreen.kt` | 会话列表、新建/删除/切换会话 |
| **底部导航** | `ModernBottomBar.kt` | 微信风格 3 标签（聊天、工具、设置） |
| **粒子背景** | `ParticleBackground.kt` | 全屏粒子动画背景 |

### 2. 对话框组件

| 对话框 | 文件 | 功能 |
|--------|------|------|
| **API Key** | `ConfigDialogs.kt` | API Key 输入、格式验证、提供商检测 |
| **API Endpoint** | `ConfigDialogs.kt` | API 端点配置、连接测试 |
| **Model Name** | `ConfigDialogs.kt` | 模型选择（glm-5、gpt-4 等） |
| **用户画像** | `ConfigDialogs.kt` | 头像、姓名、角色、语言、响应风格、专业领域 |
| **关于** | `AboutDialog.kt` | 版本信息、GitHub 链接、检查更新 |

### 3. 数据层

| 模块 | 文件 | 功能 |
|------|------|------|
| **配置管理** | `ConfigManager.kt` | DataStore 持久化（API 配置、用户画像、压缩设置） |
| **配置验证** | `ConfigValidator.kt` | API Key 格式验证、端点可达性测试 |

### 4. Rust 内核

| 模块 | 文件 | 功能 |
|------|------|------|
| **Agent** | `agent/orchestrator.rs` | Agent 循环、任务编排 |
| **LLM** | `llm/mod.rs` | LLM API 客户端（支持 OpenAI/自定义端点） |
| **Memory** | `memory/mod.rs` | 记忆引擎、上下文管理 |
| **Storage** | `storage/mod.rs` | SQLite 存储 |
| **Skills** | `skills/` | 技能加载、匹配、注册 |
| **MCP** | `mcp/` | MCP 协议客户端 |
| **Compression** | `compression/mod.rs` | 上下文压缩 |
| **Tools** | `tools/android/` | Android 原生工具（截图、OCR、输入等） |

---

## 四、版本历史

### v1.0.21 (2026-05-26)
- ✅ 修复对话框不显示问题（对话框移到 Box 组件内部）
- ✅ API 配置对话框正常弹出
- ✅ 用户画像对话框正常弹出

### v1.0.20 (2026-05-26)
- ✅ 数据持久化 (DataStore)
- ✅ 配置验证功能
- ✅ 用户画像增强（头像、语言、风格、专业领域）

### v1.0.19 (2026-05-25)
- ✅ 微信风格底部导航栏
- ✅ 设置页面实际功能

### v1.0.18 (2026-05-25)
- ✅ 自动更新功能
- ✅ UI 重构修复

---

## 五、技术栈

### Android 端
- **语言**: Kotlin 2.0
- **UI**: Jetpack Compose (Material 3)
- **架构**: MVVM
- **持久化**: DataStore (Preferences)
- **图片加载**: Coil
- **协程**: kotlinx-coroutines

### Rust 内核
- **语言**: Rust 2021 Edition
- **异步运行时**: Tokio
- **序列化**: serde / serde_json
- **存储**: rusqlite
- **HTTP**: reqwest (rustls-tls)
- **Android JNI**: jni / ndk

### 构建与部署
- **CI/CD**: GitHub Actions
- **构建产物**: 
  - `chenyi-agent-debug.apk`
  - `chenyi-agent-release.apk`
- **部署**: SSH 到服务器 `8.147.232.175`

---

## 六、工具清单 (20 个)

| ID | 工具名称 | 功能 | 实现 |
|----|----------|------|------|
| 1 | `screenshot` | 截图 | MediaProjection API |
| 2 | `ocr` | 文字识别 | RapidOCR |
| 3 | `tap` | 点击坐标 | AccessibilityService |
| 4 | `swipe` | 滑动操作 | AccessibilityService |
| 5 | `input` | 文字输入 | AccessibilityService |
| 6 | `open_app` | 打开应用 | Intent |
| 7 | `home` | 返回主页 | AccessibilityService |
| 8 | `back` | 返回键 | AccessibilityService |
| 9 | `recent` | 最近任务 | AccessibilityService |
| 10 | `notification` | 打开通知栏 | AccessibilityService |
| 11 | `quick_settings` | 快速设置 | AccessibilityService |
| 12 | `power_dialog` | 电源菜单 | AccessibilityService |
| 13 | `lock_screen` | 锁屏 | AccessibilityService |
| 14 | `volume_up` | 音量+ | AudioManager |
| 15 | `volume_down` | 音量- | AudioManager |
| 16 | `volume_mute` | 静音 | AudioManager |
| 17 | `brightness_up` | 亮度+ | Settings |
| 18 | `brightness_down` | 亮度- | Settings |
| 19 | `current_app` | 获取当前应用 | AccessibilityService |
| 20 | `clipboard` | 剪贴板操作 | ClipboardManager |

---

## 七、配置项

### API 配置
- `api_key`: API Key（支持 OpenAI/智谱/自定义）
- `api_endpoint`: API 端点（默认 `api.openai.com`）
- `model_name`: 模型名称（默认 `glm-5`）

### 用户画像
- `name`: 用户姓名
- `role`: 用户角色
- `timezone`: 时区（默认 `UTC+8`）
- `language`: 语言（`zh-CN`/`en-US`/`ja-JP`）
- `response_style`: 响应风格（`concise`/`balanced`/`detailed`）
- `expertise`: 专业领域
- `avatar_path`: 头像路径

### 压缩配置
- `compression_enabled`: 是否启用压缩（默认 `true`）
- `compression_threshold`: 压缩阈值（默认 `4000` tokens）
- `compression_ratio`: 压缩比例（默认 `50%`）

---

## 八、下载与安装

### 下载地址
- **最新版本**: https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
- **版本信息**: https://oneapi.xintiandi.online/chenyi-agent/version.json

### 版本检查
```json
{
  "versionCode": 10021,
  "versionName": "1.0.21",
  "apkUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "updateLog": "..."
}
```

### 自动更新
- 应用内检查更新
- 下载进度显示
- 自动安装 APK

---

## 九、开发命令

### 编译 Rust 内核
```bash
# Windows
scripts\build-android.bat

# Linux/Mac
./scripts/build-android.sh
```

### 编译 Android APP
```bash
cd android
./gradlew assembleDebug
./gradlew assembleRelease
```

### 本地构建 APK
```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

### 查看构建状态
```bash
gh run list --limit 5
```

---

## 十、已知问题与待办

### 已解决
- ✅ 对话框不显示问题
- ✅ API 配置功能完整实现
- ✅ 用户画像功能完整实现
- ✅ 自动更新正常工作

### 待完善
- 🔄 聊天功能与 Rust 内核集成
- 🔄 上下文压缩功能
- 🔄 视觉分析功能
- 🔄 语音 STT/TTS 功能
- 🔄 技能语义匹配

---

**文档生成时间**: 2026-05-26  
**最后更新**: v1.0.21
