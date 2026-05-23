# 版本管理规范

## 版本号统一管理

晨翼Agent Android 的版本号需要在以下位置同步更新：

### 1. Android 项目配置
**文件**: `android/app/build.gradle.kts`
```kotlin
versionCode = 9
versionName = "1.0.9"
```

### 2. 服务器更新配置
**文件**: `version.json`
```json
{
  "versionCode": 9,
  "versionName": "1.0.9",
  "apkUrl": "https://oneapi.xintiandi.online/chenyi-agent/chenyi-agent-latest.apk",
  "updateLog": "更新说明",
  "forceUpdate": false
}
```

### 3. Git 标签
```bash
git tag v1.0.9
git push origin v1.0.9
```

### 4. GitHub Release
使用 `gh release create v1.0.9` 创建发布

---

## 版本号规则

- **versionCode**: 整数，每次发布递增 1（用于 Android 系统判断版本新旧）
- **versionName**: 字符串，格式 "主版本.次版本.修订号"（如 1.0.9，用于显示给用户）

---

## 发布流程

1. **更新版本号**
   ```bash
   # 修改 android/app/build.gradle.kts
   versionCode = <新版本号>
   versionName = "<新版本名>"
   
   # 修改 version.json
   versionCode: <新版本号>
   versionName: "<新版本名>"
   ```

2. **提交代码**
   ```bash
   git add .
   git commit -m "release: v1.0.9 - 版本更新说明"
   git push
   ```

3. **创建标签和发布**
   ```bash
   git tag v1.0.9
   git push origin v1.0.9
   gh release create v1.0.9 --title "v1.0.9 - 更新标题" --notes "更新说明"
   ```

4. **上传到服务器**
   - APK 上传到服务器
   - version.json 上传到服务器
   - 更新官网下载链接

---

## 当前版本: v1.0.9

- versionCode: 9
- versionName: "1.0.9"
- 发布日期: 2026-05-23
