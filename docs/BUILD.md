# 晨翼Agent Android 构建指南

## 快速开始

```bash
# 一键构建
bash scripts/build_all.sh
```

## 环境要求

### 必需
- **JDK 17+**
- **Android SDK** (API 34)
- **Rust** (1.70+)

### 可选
- **Android NDK** - 用于编译 Rust 内核和 OCR native 库

## 构建步骤

### 1. 克隆项目
```bash
git clone https://github.com/Fangpeng2025/chenyi-android-kernel.git
cd chenyi-android-kernel
```

### 2. 配置环境变量

#### Linux/macOS
```bash
export ANDROID_HOME=/path/to/android-sdk
export ANDROID_NDK_HOME=/path/to/android-ndk
```

#### Windows
```cmd
set ANDROID_HOME=C:\Users\你的用户名\AppData\Local\Android\Sdk
set ANDROID_NDK_HOME=%ANDROID_HOME%\ndk\25.2.9519653
```

### 3. 安装 Rust Android 目标
```bash
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
```

### 4. 下载 OCR 模型

**方式一：自动下载**
```bash
bash scripts/download_ocr_from_github.sh
```

**方式二：手动下载**
参考 [OCR_MODELS.md](docs/OCR_MODELS.md)

### 5. 构建

#### 完整构建（包含 Rust 内核 + OCR）
```bash
bash scripts/build_all.sh
```

#### 仅构建 APK（不包含 native 库）
```bash
cd android
./gradlew assembleRelease
```

## 项目结构

```
chenyi-android-kernel/
├── android/                 # Android 项目
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/       # Kotlin 代码
│   │   │   ├── cpp/        # C++ 代码（RapidOCR）
│   │   │   └── assets/     # OCR 模型文件
│   │   └── build.gradle.kts
│   └── ...
├── src/                     # Rust 内核
│   ├── agent/              # Agent 逻辑
│   ├── llm/                # LLM 调用
│   ├── memory/             # 记忆管理
│   ├── tools/              # 工具定义
│   └── jni.rs              # JNI 接口
└── scripts/                 # 构建脚本
    ├── build_all.sh        # 完整构建
    ├── build_rust.sh       # 编译 Rust
    └── download_ocr_from_github.sh  # 下载模型
```

## 功能清单

| 功能 | 状态 | 说明 |
|------|------|------|
| ✅ LLM 对话 | 完成 | 支持 OneAPI |
| ✅ OCR 识别 | 完成 | RapidOCR (中文) |
| ✅ 截图 | 完成 | MediaProjection API |
| ✅ 无障碍服务 | 完成 | 15 个工具 |
| ✅ Memory 持久化 | 完成 | SQLite |
| ⚠️ Rust 内核 | 可选 | 需要 NDK 编译 |

## 已注册的工具

1. `screenshot` - 截取当前屏幕
2. `tap` - 点击屏幕坐标
3. `long_press` - 长按屏幕
4. `swipe` - 滑动屏幕
5. `type_text` - 输入文本
6. `press_key` - 按键 (home/back/recent)
7. `open_app` - 打开应用
8. `close_app` - 关闭应用
9. `current_app` - 获取当前应用
10. `list_apps` - 列出应用
11. `ocr` - OCR 识别
12. `find_text` - 查找文本
13. `wait` - 等待
14. `get_screen_size` - 获取屏幕尺寸
15. `scroll` - 滚动屏幕

## 故障排除

### OCR 模型下载失败
```bash
# 手动下载模型文件到 assets 目录
# 参考 docs/OCR_MODELS.md
```

### Rust 编译失败
```bash
# 检查 NDK 路径
echo $ANDROID_NDK_HOME

# 检查 Rust 目标
rustup target list | grep android
```

### Gradle 构建失败
```bash
# 清理构建缓存
cd android
./gradlew clean
./gradlew assembleRelease
```

## 相关文档

- [OCR 模型下载](docs/OCR_MODELS.md)
- [API 配置](README.md#配置)
- [开发指南](README.md#开发)
