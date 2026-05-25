# 晨翼Agent 自动更新 - 服务器部署清单

## 需要上传的文件

### 1. version.json (必须)

**路径**: `/var/www/chenyi-agent/version.json`
**URL**: https://oneapi.xintiandi.online/chenyi-agent/version.json
**格式**: JSON
**大小**: ~1KB

**内容模板**:
```json
{
  "versionName": "1.0.17",
  "versionCode": 10017,
  "releaseNotes": "【v1.0.17 更新内容】\n\n✨ 新功能\n• 自动更新功能\n• 下载进度显示\n\n🔧 技术改进\n• UpdateManager 更新管理器\n• 服务器版本对接",
  "minAndroidVersion": 21,
  "apkSize": 8500000,
  "downloadUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "forceUpdate": false,
  "buildDate": "2026-05-25"
}
```

---

### 2. chenyi-agent-release.apk (必须)

**路径**: `/var/www/chenyi-agent/chenyi-agent-release.apk`
**URL**: https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
**格式**: APK (Android Application Package)
**大小**: ~8MB (根据实际构建大小)

**注意事项**:
- 必须使用固定签名密钥
- 密码: `chenyi2026agent`
- 别名: `chenyi-agent`
- 签名必须与已安装版本一致

---

### 3. chenyi-agent-debug.apk (可选)

**路径**: `/var/www/chenyi-agent/chenyi-agent-debug.apk`
**URL**: https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-debug.apk
**格式**: APK (Android Application Package)
**用途**: 测试版本，用于调试

---

## Nginx 配置

### 文件路径
`/etc/nginx/sites-available/oneapi.xintiandi.online`

### 配置内容
```nginx
location /chenyi-agent/ {
    alias /var/www/chenyi-agent/;
    autoindex on;
    add_header Access-Control-Allow-Origin *;
    add_header Access-Control-Allow-Methods 'GET, OPTIONS';
    expires 1h;
}
```

### 生效命令
```bash
sudo nginx -t  # 测试配置
sudo systemctl reload nginx  # 重载配置
```

---

## 部署步骤

### 1. 准备文件

```bash
# 本地构建 APK
cd android
./gradlew assembleRelease

# 生成的文件路径
# android/app/build/outputs/apk/release/chenyi-agent-release.apk
```

### 2. 更新版本信息

编辑 `version.json`，更新以下字段：
- `versionName`: 新版本名称
- `versionCode`: 新版本号
- `releaseNotes`: 更新说明
- `apkSize`: APK 文件大小 (使用 `stat -c%s` 命令获取)
- `buildDate`: 发布日期

### 3. 上传文件

```bash
# 使用部署脚本
./scripts/deploy-update.sh

# 或手动上传
scp version.json root@8.147.232.175:/var/www/chenyi-agent/
scp android/app/build/outputs/apk/release/chenyi-agent-release.apk root@8.147.232.175:/var/www/chenyi-agent/
```

### 4. 验证部署

```bash
# 测试脚本
./scripts/test-update.sh

# 或手动验证
curl -I https://oneapi.xintiandi.online/chenyi-agent/version.json
curl -I https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
```

---

## 文件权限

```bash
# 设置文件权限
ssh root@8.147.232.175
chmod 644 /var/www/chenyi-agent/version.json
chmod 644 /var/www/chenyi-agent/chenyi-agent-release.apk
chown -R www-data:www-data /var/www/chenyi-agent
```

---

## 目录结构

```
/var/www/chenyi-agent/
├── version.json                  # 版本信息文件
├── chenyi-agent-release.apk      # 正式版 APK
├── chenyi-agent-debug.apk        # 测试版 APK (可选)
└── index.html                    # 下载页面 (可选)
```

---

## 访问 URL

| 文件 | URL |
|------|-----|
| version.json | https://oneapi.xintiandi.online/chenyi-agent/version.json |
| release APK | https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk |
| debug APK | https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-debug.apk |

---

## 监控与维护

### 查看下载统计

```bash
grep "chenyi-agent-release.apk" /var/log/nginx/chenyi-agent-access.log | wc -l
```

### 查看版本检查统计

```bash
grep "version.json" /var/log/nginx/chenyi-agent-access.log | wc -l
```

### 清理旧版本

```bash
# 保留当前版本，删除旧版本
rm /var/www/chenyi-agent/chenyi-agent-1.0.15.apk
rm /var/www/chenyi-agent/chenyi-agent-1.0.16.apk
```

---

## 相关文档

- `docs/AUTO_UPDATE_DEPLOYMENT.md` - 部署文档
- `docs/AUTO_UPDATE_FLOW.md` - 用户使用流程
- `scripts/deploy-update.sh` - 部署脚本
- `scripts/test-update.sh` - 测试脚本