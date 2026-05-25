# 晨翼Agent 自动更新 - 完整流程

## 📱 用户使用流程

```
用户打开 APP
    ↓
进入「设置」页面
    ↓
点击「检查更新」
    ↓
┌─────────────────────────────┐
│  1. 检查服务器版本           │
│     GET version.json        │
│     比较版本号               │
└─────────────────────────────┘
    ↓
   有新版本？
    ├─ 是 → 显示 "v1.0.17 可用"
    │         ↓
    │      用户再次点击
    │         ↓
    │      ┌─────────────────────────────┐
    │      │  2. 下载 APK                │
    │      │     显示进度条 0-100%       │
    │      │     保存到缓存目录          │
    │      └─────────────────────────────┘
    │         ↓
    │      ┌─────────────────────────────┐
    │      │  3. 安装 APK                │
    │      │     FileProvider URI        │
    │      │     启动安装界面            │
    │      └─────────────────────────────┘
    │         ↓
    │      用户确认安装
    │         ↓
    │      ✅ 更新完成
    │
    └─ 否 → 显示 "当前已是最新版本"
```

---

## 🚀 开发者发布流程

### 方式 1: 手动发布

```bash
# 1. 更新版本号
# 编辑 build.gradle.kts
versionCode = 10017
versionName = "1.0.17"

# 2. 构建签名 APK
cd android
./gradlew assembleRelease

# 3. 更新 version.json
{
  "versionName": "1.0.17",
  "versionCode": 10017,
  ...
}

# 4. 上传到服务器
./scripts/deploy-update.sh
```

### 方式 2: 自动发布（推荐）

```bash
# 1. 更新版本号
# 编辑 build.gradle.kts

# 2. 创建 Git 标签
git tag v1.0.17
git push origin v1.0.17

# 3. GitHub Actions 自动完成
# - 构建 APK
# - 生成 version.json
# - 上传到服务器
# - 验证部署
```

---

## 📋 服务器文件结构

```
/var/www/chenyi-agent/
├── version.json                    # 版本信息文件
├── chenyi-agent-release.apk        # 正式版 APK
└── chenyi-agent-debug.apk          # 测试版 APK（可选）
```

---

## 🔗 API 端点

### 1. 获取版本信息

```
GET https://oneapi.xintiandi.online/chenyi-agent/version.json
```

**响应示例**:
```json
{
  "versionName": "1.0.17",
  "versionCode": 10017,
  "releaseNotes": "【v1.0.17 更新内容】\n\n✨ 新功能\n• 自动更新功能",
  "minAndroidVersion": 21,
  "apkSize": 8500000,
  "downloadUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "forceUpdate": false,
  "buildDate": "2026-05-25"
}
```

### 2. 下载 APK

```
GET https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
```

**响应**: APK 文件（二进制流）

**支持**:
- Range 请求（断点续传）
- HTTPS 加密传输
- CORS 跨域访问

---

## 🛠️ 故障排查

### 问题 1: "检查更新失败"

**可能原因**:
1. 服务器 version.json 不存在
2. URL 配置错误
3. HTTPS 证书问题
4. 网络连接失败

**解决方法**:
```bash
# 检查文件是否存在
curl -I https://oneapi.xintiandi.online/chenyi-agent/version.json

# 检查证书
openssl s_client -connect oneapi.xintiandi.online:443

# 查看服务器日志
ssh root@8.147.232.175
tail -f /var/log/nginx/chenyi-agent-error.log
```

### 问题 2: "下载失败"

**可能原因**:
1. APK 文件不存在
2. 磁盘空间不足
3. 网络中断

**解决方法**:
```bash
# 检查 APK 文件
curl -I https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk

# 检查服务器磁盘空间
ssh root@8.147.232.175
df -h /var/www/chenyi-agent
```

### 问题 3: "无法安装"

**可能原因**:
1. 签名不一致
2. FileProvider 配置错误
3. 安装权限未授予

**解决方法**:
```bash
# 检查 APK 签名
jarsigner -verify -verbose -certs app.apk

# 确认使用相同签名密钥
# GitHub Secrets: KEYSTORE_BASE64
# 密码: chenyi2026agent
```

---

## 📊 版本号规则

### 计算公式

```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH
```

### 示例

| 版本名称 | versionCode |
|---------|-------------|
| 1.0.0   | 10000       |
| 1.0.16  | 10016       |
| 1.0.17  | 10017       |
| 1.1.0   | 10100       |
| 2.0.0   | 20000       |

### 更新逻辑

```kotlin
// 服务器版本 > 本地版本 → 有更新
if (serverVersionCode > localVersionCode) {
    // 显示更新提示
}
```

---

## 🔐 安全考虑

### 1. HTTPS 加密

- ✅ 所有传输使用 HTTPS
- ✅ 防止中间人攻击
- ✅ 保护用户隐私

### 2. 签名验证

- ✅ APK 必须签名才能安装
- ✅ 签名密钥安全存储
- ✅ GitHub Secrets 加密

### 3. 权限控制

- ✅ 最小权限原则
- ✅ 只请求必要权限
- ✅ 用户明确授权

---

## 📈 监控与统计

### 服务器日志

```bash
# 查看访问日志
tail -f /var/log/nginx/chenyi-agent-access.log

# 统计下载次数
grep "chenyi-agent-release.apk" /var/log/nginx/chenyi-agent-access.log | wc -l

# 统计版本检查次数
grep "version.json" /var/log/nginx/chenyi-agent-access.log | wc -l
```

### 日志格式

```
$remote_addr - $remote_user [$time_local] "$request" $status $body_bytes_sent
```

---

## 🎯 最佳实践

### 1. 发布前测试

- [ ] 本地测试版本检查
- [ ] 测试下载流程
- [ ] 测试安装流程
- [ ] 验证签名一致性
- [ ] 检查服务器文件可访问

### 2. 版本管理

- [ ] 严格遵循版本号规则
- [ ] 更新 releaseNotes
- [ ] 记录更新日志
- [ ] 保留历史版本（可选）

### 3. 用户体验

- [ ] 不强制更新（除非必要）
- [ ] 提供清晰的更新说明
- [ ] 显示下载进度
- [ ] 允许后台下载

---

## 📞 技术支持

- **服务器**: root@8.147.232.175
- **官网**: https://xintiandi.online
- **GitHub**: https://github.com/Fangpeng2025/chenyi-android-kernel
- **文档**: docs/AUTO_UPDATE_DEPLOYMENT.md
