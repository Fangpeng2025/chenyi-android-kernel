#!/bin/bash

# 晨翼Agent 更新功能测试脚本

set -e

echo "========================================="
echo "晨翼Agent 更新功能测试"
echo "========================================="
echo ""

# 服务器地址
BASE_URL="https://oneapi.xintiandi.online/chenyi-agent"

echo "📋 测试项目:"
echo "  1. 检查 version.json 可访问性"
echo "  2. 验证 JSON 格式"
echo "  3. 检查 APK 文件可访问性"
echo "  4. 验证 APK 文件大小"
echo ""

# 测试 1: 检查 version.json
echo "🔍 测试 1: 检查 version.json"
echo "-----------------------------------"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/version.json)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ version.json 可访问 (HTTP $HTTP_CODE)"
else
    echo "❌ version.json 不可访问 (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""

# 测试 2: 验证 JSON 格式
echo "🔍 测试 2: 验证 JSON 格式"
echo "-----------------------------------"
JSON=$(curl -s $BASE_URL/version.json)

# 检查必需字段
check_field() {
    FIELD=$1
    if echo "$JSON" | jq -e ".$FIELD" > /dev/null 2>&1; then
        VALUE=$(echo "$JSON" | jq -r ".$FIELD")
        echo "✅ $FIELD: $VALUE"
    else
        echo "❌ 缺少字段: $FIELD"
        return 1
    fi
}

check_field "versionName"
check_field "versionCode"
check_field "releaseNotes"
check_field "downloadUrl"

echo ""

# 测试 3: 检查 APK 文件
echo "🔍 测试 3: 检查 APK 文件"
echo "-----------------------------------"
APK_URL=$(echo "$JSON" | jq -r ".downloadUrl")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -I $APK_URL)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ APK 文件可访问 (HTTP $HTTP_CODE)"
else
    echo "❌ APK 文件不可访问 (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""

# 测试 4: 验证 APK 文件大小
echo "🔍 测试 4: 验证 APK 文件大小"
echo "-----------------------------------"
EXPECTED_SIZE=$(echo "$JSON" | jq -r ".apkSize")
ACTUAL_SIZE=$(curl -s -I $APK_URL | grep -i content-length | awk '{print $2}' | tr -d '\r')

if [ "$EXPECTED_SIZE" = "$ACTUAL_SIZE" ]; then
    echo "✅ APK 大小匹配: $(echo $ACTUAL_SIZE | numfmt --to=iec-i --suffix=B)"
else
    echo "⚠️  APK 大小不匹配"
    echo "   预期: $EXPECTED_SIZE bytes"
    echo "   实际: $ACTUAL_SIZE bytes"
fi

echo ""

# 测试 5: 检查 HTTPS
echo "🔍 测试 5: 检查 HTTPS"
echo "-----------------------------------"
if [[ $BASE_URL == https:* ]]; then
    echo "✅ 使用 HTTPS 加密传输"
else
    echo "⚠️  未使用 HTTPS，建议启用"
fi

echo ""

# 测试 6: 检查 CORS
echo "🔍 测试 6: 检查 CORS"
echo "-----------------------------------"
CORS=$(curl -s -I $BASE_URL/version.json | grep -i "access-control-allow-origin" || true)

if [ -n "$CORS" ]; then
    echo "✅ CORS 已配置: $CORS"
else
    echo "⚠️  CORS 未配置，APP 可能无法访问"
fi

echo ""

# 显示完整 JSON
echo "📄 完整 version.json:"
echo "-----------------------------------"
echo "$JSON" | jq .

echo ""
echo "========================================="
echo "✅ 测试完成！"
echo "========================================="
