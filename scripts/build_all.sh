#!/bin/bash
# 晨翼Agent Android 完整构建脚本

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "================================================"
echo "   晨翼Agent Android 完整构建"
echo "================================================"
echo ""

# 1. 检查环境
echo "[1/5] 检查构建环境..."

if ! command -v rustc &> /dev/null; then
    echo "错误: Rust 未安装"
    echo "请访问 https://rustup.rs 安装 Rust"
    exit 1
fi

if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "警告: ANDROID_NDK_HOME 未设置"
    echo "将使用 Gradle 自动配置 NDK"
fi

echo "✓ Rust: $(rustc --version)"
echo ""

# 2. 下载 OCR 模型
echo "[2/5] 检查 OCR 模型文件..."

if [ ! -f "android/app/src/main/assets/ch_PP-OCRv3_det_infer.onnx" ]; then
    echo "下载 OCR 模型..."
    bash scripts/download_ocr_from_github.sh || {
        echo ""
        echo "警告: 自动下载失败"
        echo "请手动下载模型文件到 android/app/src/main/assets/"
        echo "参考文档: docs/OCR_MODELS.md"
        echo ""
        read -p "是否继续构建（不包含 OCR）？(y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    }
else
    echo "✓ OCR 模型文件已存在"
fi
echo ""

# 3. 编译 Rust 内核（如果设置了 NDK）
echo "[3/5] 编译 Rust 内核..."

if [ -n "$ANDROID_NDK_HOME" ]; then
    bash scripts/build_rust.sh || {
        echo "警告: Rust 内核编译失败，将使用纯 Kotlin 实现"
    }
else
    echo "跳过 Rust 内核编译（未设置 ANDROID_NDK_HOME）"
    echo "将使用纯 Kotlin 实现"
fi
echo ""

# 4. 构建 Android APK
echo "[4/5] 构建 Android APK..."

cd android

if [ ! -f "gradlew" ]; then
    echo "初始化 Gradle Wrapper..."
    gradle wrapper
fi

chmod +x gradlew

if [ -n "$ANDROID_NDK_HOME" ]; then
    echo "执行完整构建（包含 native 库）..."
    ./gradlew assembleRelease
else
    echo "执行纯 Kotlin 构建（不包含 native 库）..."
    ./gradlew assembleRelease -Pndk.disable=true || {
        echo "警告: 完整构建失败，尝试跳过 native 库..."
        ./gradlew assembleRelease -x externalNativeBuildRelease
    }
fi

cd ..
echo ""

# 5. 输出结果
echo "[5/5] 构建完成！"
echo ""
echo "================================================"
APK_PATH="android/app/build/outputs/apk/release/app-release.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo "✓ APK 位置: $APK_PATH"
    echo "✓ APK 大小: $APK_SIZE"
    echo ""
    echo "安装命令:"
    echo "  adb install -r $APK_PATH"
else
    echo "⚠ APK 未生成，请检查构建日志"
fi
echo "================================================"
