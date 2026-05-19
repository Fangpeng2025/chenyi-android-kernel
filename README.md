# 晨翼Agent Android 内核

基于 Rust 的高性能 AI Agent 内核，专为 Android 平台优化。

## 架构

```
┌─────────────────────────────────────┐
│         Kotlin UI (Compose)          │
└─────────────────────────────────────┘
                    │ JNI
                    ▼
┌─────────────────────────────────────┐
│           Rust 内核                  │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │  Agent  │ │  Memory │ │   LLM  │ │
│  │  Loop   │ │  Engine │ │ Client │ │
│  └─────────┘ └─────────┘ └────────┘ │
│  ┌─────────────────────────────────┐│
│  │     Android Native Tools        ││
│  │  截图 | 输入 | 应用管理 | OCR   ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────┐
│      Android System Services         │
│  AccessibilityService | 原生 API     │
└─────────────────────────────────────┘
```

## 目录结构

```
chenyi-android-kernel/
├── Cargo.toml              # Rust 项目配置
├── src/
│   ├── lib.rs              # 主入口
│   ├── jni.rs              # JNI 接口
│   ├── types.rs            # 类型定义
│   ├── agent/              # Agent 核心
│   ├── memory/             # 记忆引擎
│   ├── llm/                # LLM 客户端
│   ├── storage/            # SQLite 存储
│   └── tools/              # 工具系统
│       └── android/        # 安卓原生工具
├── android/                # Android 项目
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/chenyi/agent/
│   │       │   ├── Kernel.kt           # JNI 桥接
│   │       │   ├── MainActivity.kt     # UI
│   │       │   └── ChenyiAccessibilityService.kt
│   │       └── res/
│   └── build.gradle.kts
├── scripts/
│   ├── build-android.sh    # Linux/Mac 编译脚本
│   └── build-android.bat   # Windows 编译脚本
└── .cargo/config.toml      # 交叉编译配置
```

## 编译

### 1. 设置环境

```bash
# Linux/Mac
export ANDROID_NDK_HOME=/path/to/ndk/25.2.9519653

# Windows
set ANDROID_NDK_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\ndk\25.2.9519653
```

### 2. 编译 Rust 内核

```bash
# Linux/Mac
./scripts/build-android.sh

# Windows
scripts\build-android.bat
```

### 3. 编译 Android APP

```bash
cd android
./gradlew assembleDebug
```

## 功能

### 安卓原生工具

| 工具 | 功能 | 实现 |
|------|------|------|
| `screenshot` | 截图 | MediaProjection API |
| `tap` | 点击坐标 | AccessibilityService |
| `long_press` | 长按 | AccessibilityService |
| `swipe` | 滑动 | AccessibilityService |
| `type_text` | 输入文本 | AccessibilityService |
| `press_key` | 按键 | AccessibilityService |
| `open_app` | 打开应用 | PackageManager |
| `close_app` | 关闭应用 | AccessibilityService |
| `current_app` | 获取当前应用 | AccessibilityService |
| `list_apps` | 列出应用 | PackageManager |
| `ocr` | OCR 识别 | RapidOCR4j-Android |

### JNI 接口

```kotlin
// 初始化
kernel.init()

// 发送消息
kernel.chat("你好")

// 执行工具
kernel.executeTool("tap", mapOf("x" to 100, "y" to 200))

// 获取状态
kernel.getStatus()

// 销毁
kernel.destroy()
```

## 使用示例

### 抖音发布流程

```kotlin
// 1. 打开抖音
kernel.executeTool("open_app", mapOf("package" to "com.ss.android.ugc.aweme"))

// 2. 等待加载
Thread.sleep(2000)

// 3. OCR 定位发布按钮
kernel.executeTool("tap_text", mapOf("text" to "发布"))

// 4. 选择相册
kernel.executeTool("tap_text", mapOf("text" to "相册"))

// 5. 选择视频
kernel.executeTool("tap", mapOf("x" to 200, "y" to 500))

// 6. 下一步
kernel.executeTool("tap_text", mapOf("text" to "下一步"))

// 7. 输入标题
kernel.executeTool("type_text", mapOf("text" to "AI 生成的视频"))

// 8. 发布
kernel.executeTool("tap_text", mapOf("text" to "发布"))
```

## 技术栈

- **Rust**: 内核实现 (tokio, serde, rusqlite, jni)
- **Kotlin**: Android UI (Jetpack Compose)
- **Android**: AccessibilityService, MediaProjection API
- **SQLite**: 本地存储

## 开发状态

- [x] Phase 1: Rust 内核基础架构
- [x] Phase 2: JNI 接口层
- [x] Phase 3: 安卓原生工具模块
- [x] Phase 4: Agent 核心引擎
- [x] Phase 5: Kotlin Android 项目
- [x] Phase 6: 编译和集成配置

## License

MIT