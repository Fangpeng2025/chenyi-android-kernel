#!/bin/bash
# 编译 Rust 内核为 Android 库

set -e

echo "=== 编译 Rust 内核 ==="

# 设置 Android NDK 路径（需要根据实际情况修改）
if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "错误: ANDROID_NDK_HOME 未设置"
    echo "请设置 ANDROID_NDK_HOME 环境变量，例如："
    echo "export ANDROID_NDK_HOME=/path/to/android-ndk"
    exit 1
fi

# 目标架构
TARGETS=("aarch64-linux-android" "armv7-linux-androideabi")

# 创建输出目录
mkdir -p android/app/src/main/jniLibs/arm64-v8a
mkdir -p android/app/src/main/jniLibs/armeabi-v7a

# 编译每个架构
for TARGET in "${TARGETS[@]}"; do
    echo ""
    echo "编译目标: $TARGET"
    
    # 设置工具链
    export AR_${TARGET//-/_}=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar
    export CC_${TARGET//-/_}=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/${TARGET}-clang
    
    # 编译
    cargo build --target $TARGET --release
    
    # 复制库文件
    if [ "$TARGET" = "aarch64-linux-android" ]; then
        cp target/$TARGET/release/libchenyi.so android/app/src/main/jniLibs/arm64-v8a/
    elif [ "$TARGET" = "armv7-linux-androideabi" ]; then
        cp target/$TARGET/release/libchenyi.so android/app/src/main/jniLibs/armeabi-v7a/
    fi
done

echo ""
echo "=== 编译完成 ==="
ls -lh android/app/src/main/jniLibs/*/
