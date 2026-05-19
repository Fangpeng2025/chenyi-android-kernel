@echo off
REM 晨翼Agent Android 内核编译脚本 (Windows)

echo === 晨翼Agent Android 内核编译 ===

REM 检查环境变量
if "%ANDROID_NDK_HOME%"=="" (
    echo 错误: ANDROID_NDK_HOME 未设置
    echo 请设置 ANDROID_NDK_HOME 环境变量，例如:
    echo set ANDROID_NDK_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk\ndk\25.2.9519653
    exit /b 1
)

echo ANDROID_NDK_HOME: %ANDROID_NDK_HOME%

REM 添加 Android 目标
echo 添加 Rust Android 目标...
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
rustup target add i686-linux-android

REM 编译 arm64-v8a
echo 编译 arm64-v8a...
cargo build --release --target aarch64-linux-android --features android

REM 编译 armeabi-v7a
echo 编译 armeabi-v7a...
cargo build --release --target armv7-linux-androideabi --features android

REM 编译 x86_64 (模拟器)
echo 编译 x86_64...
cargo build --release --target x86_64-linux-android --features android

REM 编译 x86 (模拟器)
echo 编译 x86...
cargo build --release --target i686-linux-android --features android

REM 复制到 Android 项目
echo 复制编译产物...
set PROJECT_DIR=android\app\src\main\jniLibs

if not exist "%PROJECT_DIR%\arm64-v8a" mkdir "%PROJECT_DIR%\arm64-v8a"
if not exist "%PROJECT_DIR%\armeabi-v7a" mkdir "%PROJECT_DIR%\armeabi-v7a"
if not exist "%PROJECT_DIR%\x86_64" mkdir "%PROJECT_DIR%\x86_64"
if not exist "%PROJECT_DIR%\x86" mkdir "%PROJECT_DIR%\x86"

copy target\aarch64-linux-android\release\chenyi.dll "%PROJECT_DIR%\arm64-v8a\libchenyi.so"
copy target\armv7-linux-androideabi\release\chenyi.dll "%PROJECT_DIR%\armeabi-v7a\libchenyi.so"
copy target\x86_64-linux-android\release\chenyi.dll "%PROJECT_DIR%\x86_64\libchenyi.so"
copy target\i686-linux-android\release\chenyi.dll "%PROJECT_DIR%\x86\libchenyi.so"

echo === 编译完成 ===
echo 产物位置: %PROJECT_DIR%
dir "%PROJECT_DIR%\arm64-v8a\libchenyi.so"