#!/bin/bash
# 从 GitHub Release 下载 OCR 模型文件

set -e

ASSETS_DIR="android/app/src/main/assets"
TEMP_DIR="/tmp/ocr_extract"

echo "=== 下载 OCR 模型文件 ==="

# 创建目录
mkdir -p "$ASSETS_DIR"
mkdir -p "$TEMP_DIR"

# 下载 APK
echo "1. 下载 RapidOCR APK..."
APK_URL="https://github.com/RapidAI/RapidOcrAndroidOnnx/releases/download/1.3.0/RapidOcrAndroidOnnx-1.3.0-release.apk"
APK_FILE="$TEMP_DIR/ocr.apk"

if ! curl -L -o "$APK_FILE" "$APK_URL"; then
    echo "错误: 无法下载 APK"
    echo "请手动下载模型文件，参考 docs/OCR_MODELS.md"
    exit 1
fi

echo "2. 解压 APK..."
cd "$TEMP_DIR"
unzip -q -o "$APK_FILE"

echo "3. 复制模型文件..."
cp assets/*.onnx "$ASSETS_DIR/" 2>/dev/null || true
cp assets/ppocr_keys_v1.txt "$ASSETS_DIR/" 2>/dev/null || true

# 清理
rm -rf "$TEMP_DIR"

echo ""
echo "=== 下载完成 ==="
echo "模型文件位置: $ASSETS_DIR"
ls -lh "$ASSETS_DIR"
