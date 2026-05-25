# 自动更新功能对接文档

## 概述

自动更新功能已对接到你们的服务器，完整流程如下：

1. **检查更新** - 从服务器获取版本信息
2. **下载 APK** - 下载最新版本安装包
3. **安装更新** - 自动启动安装界面

---

## 服务器配置

### 服务器信息

- **官网**: https://xintiandi.online
- **API 服务器**: https://oneapi.xintiandi.online
- **APK 下载地址**: https://oneapi.xintiandi.online/chenyi-agent/

### 需要部署的文件

#### 1. version.json

上传到服务器: `https://oneapi.xintiandi.online/chenyi-agent/version.json`

```json
{
  "versionName": "1.0.17",
  "versionCode": 10017,
  "releaseNotes": "【v1.0.17 更新内容】\n\n✨ 新功能\n• 自动更新功能\n• 下载进度显示\n• 版本检查优化\n\n🔧 技术改进\n• UpdateManager 更新管理器\n• 服务器版本对接\n• 下载进度回调",
  "minAndroidVersion": 21,
  "apkSize": 8500000,
  "downloadUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "forceUpdate": false,
  "buildDate": "2026-05-25"
}
```

**字段说明**：
- `versionName`: 版本名称（用户看到的，如 "1.0.17"）
- `versionCode`: 版本号（整数，用于比较版本大小）
  - 计算公式: `MAJOR * 10000 + MINOR * 100 + PATCH`
  - 例如: 1.0.17 = 10017
- `releaseNotes`: 更新日志（支持 \n 换行）
- `minAndroidVersion`: 最低 Android 版本（21 = Android 5.0）
- `apkSize`: APK 文件大小（字节，可选）
- `downloadUrl`: APK 下载地址
- `forceUpdate`: 是否强制更新（预留字段）
- `buildDate`: 构建日期

#### 2. APK 文件

上传到服务器：
- `https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk` (正式版)
- `https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-debug.apk` (测试版，可选)

---

## 客户端实现

### UpdateManager.kt

**功能**：
- ✅ 检查服务器版本
- ✅ 下载 APK 文件（带进度）
- ✅ 安装 APK（支持 Android 7.0+ FileProvider）
- ✅ 错误处理和日志记录

**核心方法**：

```kotlin
// 1. 检查更新
suspend fun checkForUpdate(): Result<VersionInfo?>

// 2. 下载 APK
suspend fun downloadApk(
    versionInfo: VersionInfo,
    onProgress: (Int) -> Unit
): Result<String>

// 3. 安装 APK
fun installApk(apkPath: String): Result<Unit>
```

### SettingsScreen.kt

**UI 组件**：
- ✅ 检查更新按钮
- ✅ 下载进度条
- ✅ 版本信息显示
- ✅ 状态文字提示

**状态显示**：
1. **空闲状态**: "检查更新" + "当前已是最新版本"
2. **检查中**: "检查中..."
3. **有新版本**: "v1.0.17 可用" + "点击下载安装"
4. **下载中**: "下载中 45%" + 进度条
5. **下载完成**: 自动启动安装界面

### MainActivity.kt

**集成逻辑**：
- ✅ 创建 UpdateManager 实例
- ✅ 状态管理（检查中、有更新、下载进度）
- ✅ 点击事件处理（检查或下载）
- ✅ Toast 提示反馈

---

## 部署步骤

### 方法 1: SSH 部署到服务器

```bash
# 1. 连接服务器
ssh root@8.147.232.175

# 2. 创建目录
mkdir -p /var/www/chenyi-agent

# 3. 上传文件（本地执行）
scp version.json root@8.147.232.175:/var/www/chenyi-agent/
scp chenyi-agent-release.apk root@8.147.232.175:/var/www/chenyi-agent/

# 4. 配置 Nginx（如果还没配置）
# 编辑 /etc/nginx/sites-available/oneapi.xintiandi.online
# 添加:
# location /chenyi-agent/ {
#     alias /var/www/chenyi-agent/;
#     autoindex on;
# }
```

### 方法 2: GitHub Actions 自动部署

修改 `.github/workflows/build-apk.yml`，添加自动上传步骤：

```yaml
- name: Deploy to Server
  if: success()
  uses: appleboy/scp-action@master
  with:
    host: ${{ secrets.SERVER_HOST }}
    username: ${{ secrets.SERVER_USER }}
    key: ${{ secrets.SERVER_SSH_KEY }}
    source: "android/app/build/outputs/apk/release/*.apk"
    target: "/var/www/chenyi-agent"
    strip_components: 6
```

---

## 测试流程

### 1. 本地测试

```kotlin
// 在 MainActivity 中测试
fun testUpdate() {
    scope.launch {
        val result = updateManager.checkForUpdate()
        result.fold(
            onSuccess = { info ->
                Log.d("Test", "Latest version: ${info?.versionName}")
            },
            onFailure = { error ->
                Log.e("Test", "Error: ${error.message}")
            }
        )
    }
}
```

### 2. 验证清单

- [ ] 服务器 version.json 可访问
- [ ] APK 文件可下载
- [ ] 客户端能正确解析版本信息
- [ ] 版本比较逻辑正确
- [ ] 下载进度显示正常
- [ ] 安装界面能正常启动
- [ ] 安装后版本号正确

---

## 版本更新流程

### 发布新版本的步骤

1. **更新代码中的版本号**
   - `build.gradle.kts`: versionCode 和 versionName
   - `version.json`: versionCode 和 versionName

2. **构建 APK**
   ```bash
   # GitHub Actions 自动构建
   git tag v1.0.17
   git push origin v1.0.17
   ```

3. **上传到服务器**
   - 更新 `version.json`
   - 上传新的 APK 文件

4. **验证更新**
   - 打开 APP 设置页面
   - 点击"检查更新"
   - 验证下载和安装

---

## 注意事项

### 1. 版本号规则

```
versionCode = MAJOR * 10000 + MINOR * 100 + PATCH

示例:
1.0.0  -> 10000
1.0.16 -> 10016
1.1.0  -> 10100
2.0.0  -> 20000
```

### 2. 签名一致性

- 所有版本必须使用相同签名
- 签名密钥已保存到 GitHub Secrets
- 密码: `chenyi2026agent`
- 别名: `chenyi-agent`

### 3. FileProvider 配置

AndroidManifest.xml 已配置:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

file_paths.xml:
```xml
<paths>
    <cache-path name="cache" path="." />
    <files-path name="files" path="." />
</paths>
```

### 4. 权限

已在 AndroidManifest.xml 中添加:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

---

## API 接口文档

### 获取版本信息

**请求**:
```
GET https://oneapi.xintiandi.online/chenyi-agent/version.json
```

**响应**:
```json
{
  "versionName": "1.0.17",
  "versionCode": 10017,
  "releaseNotes": "更新内容...",
  "minAndroidVersion": 21,
  "apkSize": 8500000,
  "downloadUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk",
  "forceUpdate": false,
  "buildDate": "2026-05-25"
}
```

### 下载 APK

**请求**:
```
GET https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-release.apk
```

**响应**: APK 文件（二进制流）

---

## 常见问题

### Q: 提示"检查更新失败"

**A**: 检查以下项：
1. 服务器 version.json 是否存在
2. URL 是否正确
3. HTTPS 证书是否有效
4. 网络连接是否正常

### Q: 下载后无法安装

**A**: 检查以下项：
1. 签名是否一致
2. FileProvider 是否配置正确
3. 是否有安装权限

### Q: 版本比较不正确

**A**: 确保 versionCode 计算正确：
- 使用公式: `MAJOR * 10000 + MINOR * 100 + PATCH`
- 服务器 versionCode 必须大于当前版本

---

## 相关文件

- `UpdateManager.kt` - 更新管理器
- `SettingsScreen.kt` - 设置界面
- `MainActivity.kt` - 主 Activity
- `version.json` - 版本信息文件
- `AndroidManifest.xml` - 权限配置
- `file_paths.xml` - FileProvider 配置

---

## 更新日志

### v1.0.17 (2026-05-25)
- ✅ 新增自动更新功能
- ✅ 对接服务器版本检查
- ✅ 下载进度显示
- ✅ 自动安装 APK
- ✅ UpdateManager 更新管理器
