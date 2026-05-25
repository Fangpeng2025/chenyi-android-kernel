#!/bin/bash

# 晨翼Agent 自动更新部署脚本
# 用途：将 version.json 和 APK 上传到服务器

set -e

# 服务器配置
SERVER_HOST="8.147.232.175"
SERVER_USER="root"
REMOTE_DIR="/var/www/chenyi-agent"
SERVER_URL="https://oneapi.xintiandi.online/chenyi-agent"

# 本地文件
VERSION_FILE="version.json"
APK_FILE="android/app/build/outputs/apk/release/chenyi-agent-release.apk"

echo "========================================="
echo "晨翼Agent 自动更新部署"
echo "========================================="
echo ""

# 检查文件是否存在
if [ ! -f "$VERSION_FILE" ]; then
    echo "❌ 错误: $VERSION_FILE 不存在"
    exit 1
fi

if [ ! -f "$APK_FILE" ]; then
    echo "❌ 错误: $APK_FILE 不存在"
    echo "请先运行构建: ./gradlew assembleRelease"
    exit 1
fi

echo "📦 准备上传的文件:"
echo "  - $VERSION_FILE ($(du -h $VERSION_FILE | cut -f1))"
echo "  - $APK_FILE ($(du -h $APK_FILE | cut -f1))"
echo ""

# 获取版本信息
VERSION_NAME=$(grep -o '"versionName": *"[^"]*"' $VERSION_FILE | cut -d'"' -f4)
VERSION_CODE=$(grep -o '"versionCode": *[0-9]*' $VERSION_FILE | cut -d':' -f2 | tr -d ' ')

echo "📋 版本信息:"
echo "  - Version Name: $VERSION_NAME"
echo "  - Version Code: $VERSION_CODE"
echo ""

# 确认上传
read -p "确认上传到服务器？ (y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 取消上传"
    exit 1
fi

echo ""
echo "🚀 开始上传..."

# 创建远程目录
echo "📁 创建远程目录..."
ssh $SERVER_USER@$SERVER_HOST "mkdir -p $REMOTE_DIR"

# 上传 version.json
echo "📤 上传 version.json..."
scp $VERSION_FILE $SERVER_USER@$SERVER_HOST:$REMOTE_DIR/

# 上传 APK
echo "📤 上传 APK..."
scp $APK_FILE $SERVER_USER@$SERVER_HOST:$REMOTE_DIR/

echo ""
echo "✅ 上传完成！"
echo ""

# 验证文件
echo "🔍 验证文件..."
echo ""

# 检查 version.json
echo "检查 version.json:"
curl -s $SERVER_URL/version.json | head -5
echo ""

# 检查 APK
echo "检查 APK:"
APK_SIZE=$(curl -sI $SERVER_URL/chenyi-agent-release.apk | grep -i content-length | awk '{print $2}' | tr -d '\r')
echo "  - Size: $(echo $APK_SIZE | numfmt --to=iec-i --suffix=B)"
echo ""

echo "========================================="
echo "🎉 部署成功！"
echo "========================================="
echo ""
echo "用户可以通过以下方式更新:"
echo "  1. 打开 APP 设置页面"
echo "  2. 点击「检查更新」"
echo "  3. 下载并安装新版本"
echo ""
echo "下载地址:"
echo "  - $SERVER_URL/chenyi-agent-release.apk"
echo ""
