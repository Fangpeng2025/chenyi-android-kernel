# 晨翼Agent APP 自动更新部署流程

## 📝 工作流程

### 1. GitHub Actions 自动构建
当推送代码或创建 Release 时，GitHub Actions 自动构建 APK：
- 仓库：`Fangpeng2025/chenyi-android-kernel`
- 构建产物：`app-release.apk`
- Release 页面：https://github.com/Fangpeng2025/chenyi-android-kernel/releases

### 2. 同步到官网（两种方式）

#### 方式 A：从本地上传（推荐，速度快）
```bash
# 在本地 Windows 上执行
scp -i "C:\Users\Administrator\Downloads\服务器2 (1).pem" \
  /c/Users/Administrator/chenyi-android-kernel/artifacts/chenyi-agent-release/app-release.apk \
  root@8.147.232.175:/tmp/

# SSH 到服务器
ssh -i "C:\Users\Administrator\Downloads\服务器2 (1).pem" root@8.147.232.175

# 执行同步脚本
sync-chenyi-app.sh /tmp/app-release.apk v1.0.4
```

#### 方式 B：从 GitHub Release 下载
```bash
# SSH 到服务器
ssh -i "C:\Users\Administrator\Downloads\服务器2 (1).pem" root@8.147.232.175

# 执行同步脚本（自动从 GitHub 下载最新版本）
sync-chenyi-app.sh
```
⚠️ 注意：国内访问 GitHub 较慢，建议使用方式 A

### 3. 同步脚本功能

`/usr/local/bin/sync-chenyi-app.sh` 会自动：
1. ✅ 复制/下载 APK 到服务器
2. ✅ 更新 `version.json` 和 `version.txt`
3. ✅ 更新官网下载链接
4. ✅ 清理旧版本（保留最近 3 个）

## 🌐 访问地址

### 官网
- https://xintiandi.online
- https://www.xintiandi.online

### APP 下载
- 官网下载页面：https://xintiandi.online/download/app
- 直接下载链接：https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-{版本号}.apk
- 版本信息：https://oneapi.xintiandi.online/chenyi-agent/version.json

### GitHub Release
- https://github.com/Fangpeng2025/chenyi-android-kernel/releases/latest

## 🔄 Android App 更新逻辑

App 会按优先级检查更新：
1. **阿里云（最快）**：`https://oneapi.xintiandi.online/chenyi-agent/version.json`
2. **Gitee（备用）**：`https://gitee.com/api/v5/repos/.../releases/latest`
3. **GitHub（备用）**：`https://api.github.com/repos/.../releases/latest`

## 📋 快速命令

### 查看当前版本
```bash
ssh root@8.147.232.175 'cat /www/wwwroot/xintiandi.online/files/chenyi-agent/version.json'
```

### 手动同步
```bash
ssh root@8.147.232.175 'sync-chenyi-app.sh /tmp/app-release.apk v1.0.4'
```

### 查看服务器上的 APK 文件
```bash
ssh root@8.147.232.175 'ls -lh /www/wwwroot/xintiandi.online/files/chenyi-agent/*.apk'
```

## 🎯 最佳实践

1. **发布新版本时**：
   - 确保 GitHub Actions 构建成功
   - 从本地上传 APK 到服务器（方式 A）
   - 验证官网下载链接是否正确

2. **测试更新功能**：
   - 在手机上安装旧版本
   - 打开 App → 设置 → 检查更新
   - 验证能检测到新版本并成功下载

3. **监控更新**：
   - 查看 Nginx 访问日志：`tail -f /var/log/nginx/access.log | grep chenyi-agent`
   - 检查下载统计

## 📞 故障排查

### 问题：官网无法访问
```bash
# 检查 Nginx 状态
systemctl status nginx
nginx -t

# 查看 SSL 证书
certbot certificates
```

### 问题：APK 下载失败
```bash
# 检查文件是否存在
ls -lh /www/wwwroot/xintiandi.online/files/chenyi-agent/

# 测试下载链接
curl -I https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-1.0.3.apk
```

### 问题：App 无法检测到更新
- 检查 `version.json` 中的版本号是否正确
- 确认 App 当前版本低于服务器版本
- 查看 App 日志：`adb logcat | grep HotUpdate`

---

**最后更新**：2026-05-22
**维护者**：晨翼Agent 团队
