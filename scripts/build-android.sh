#!/bin/bash
# 晨翼Agent Android 内核编译脚本

set -e

echo "=== 晨翼Agent Android 内核编译 ==="

# 检查环境变量
if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "错误: ANDROID_NDK_HOME 未设置"
    echo "请设置 ANDROID_NDK_HOME 环境变量，例如:"
    echo "export ANDROID_NDK_HOME=/path/to/ndk/25.2.9519653"
    exit 1
fi

echo "ANDROID_NDK_HOME: $ANDROID_NDK_HOME"

# 设置 NDK 工具链路径
export PATH="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"

# 添加 Android 目标
echo "添加 Rust Android 目标..."
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
rustup target add i686-linux-android

# 编译 arm64-v8a
echo "编译 arm64-v8a..."
cargo build --release --target aarch64-linux-android --features android

# 编译 armeabi-v7a
echo "编译 armeabi-v7a..."
cargo build --release --target armv7-linux-androideabi --features android

# 编译 x86_64 (模拟器)
echo "编译 x86_64..."
cargo build --release --target x86_64-linux-android --features android

# 编译 x86 (模拟器)
echo "编译 x86..."
cargo build --release --target i686-linux-android --features android

# 复制到 Android 项目
echo "复制编译产物..."
PROJECT_DIR="../android/app/src/main/jniLibs"

mkdir -p "$PROJECT_DIR/arm64-v8a"
mkdir -p "$PROJECT_DIR/armeabi-v7a"
mkdir -p "$PROJECT_DIR/x86_64"
mkdir -p "$PROJECT_DIR/x86"

cp target/aarch64-linux-android/release/libchenyi.so "$PROJECT_DIR/arm64-v8a/"
cp target/armv7-linux-androideabi/release/libchenyi.so "$PROJECT_DIR/armeabi-v7a/"
cp target/x86_64-linux-android/release/libchenyi.so "$PROJECT_DIR/x86_64/"
cp target/i686-linux-android/release/libchenyi.so "$PROJECT_DIR/x86/"

echo "=== 编译完成 ==="
echo "产物位置: $PROJECT_DIR"
ls -la "$PROJECT_DIR/arm64-v8a/libchenyi.so"
ls -la "$PROJECT_DIR/armeabi-v7a/libchenyi.so"