#!/bin/bash
# 完整构建脚本

set -e

echo "=== 晨翼Agent Android 完整构建 ==="

# 1. 下载 OCR 模型（如果不存在）
if [ ! -f "android/app/src/main/assets/ch_PP-OCRv3_det_infer.onnx" ]; then
    echo ""
    echo "=== 下载 OCR 模型 ==="
    bash scripts/download_ocr_models.sh
fi

# 2. 编译 Rust 内核
echo ""
echo "=== 编译 Rust 内核 ==="
bash scripts/build_rust.sh

# 3. 编译 Android APK
echo ""
echo "=== 编译 Android APK ==="
cd android
./gradlew assembleRelease

echo ""
echo "=== 构建完成 ==="
echo "APK 位置: android/app/build/outputs/apk/release/app-release.apk"
