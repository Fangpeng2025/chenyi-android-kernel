# 晨翼Agent 代码质量优化任务计划

**创建时间**: 2026-05-26  
**项目版本**: v1.0.21  
**优先级**: 高严重性 > 中严重性 > 低严重性

---

## 📋 任务概览

| 阶段 | 任务数 | 预计时间 | 状态 |
|------|--------|---------|------|
| 阶段一：架构重构 | 5 | 3-5天 | 🔴 未开始 |
| 阶段二：安全加固 | 3 | 1-2天 | 🔴 未开始 |
| 阶段三：代码规范 | 4 | 1-2天 | 🔴 未开始 |
| 阶段四：性能优化 | 3 | 1天 | 🔴 未开始 |
| 阶段五：清理工作 | 3 | 0.5天 | 🔴 未开始 |

**总计**: 18个任务，预计 6-10天完成

---

## 🎯 阶段一：架构重构（最高优先级）

### 任务 1.1：引入 MVVM 架构

**优先级**: 🔴 高  
**预计时间**: 1天  
**影响范围**: 整个项目

**现状问题**:
- 所有状态在 Composable 中管理
- 难以测试
- 配置加载、更新逻辑混在 UI 中

**目标**:
```
引入 ViewModel + StateFlow + Repository 模式
```

**实施步骤**:
1. 创建 `MainViewModel.kt`
2. 将 MainActivity 中的状态移到 ViewModel
3. 使用 StateFlow 替代 mutableStateOf
4. 创建 Repository 层（ConfigRepository, SessionRepository）

**验收标准**:
- [ ] MainActivity 不包含业务逻辑
- [ ] 所有状态在 ViewModel 中管理
- [ ] 可以编写单元测试

**文件清单**:
```
新建:
- viewmodel/MainViewModel.kt
- repository/ConfigRepository.kt
- repository/SessionRepository.kt

修改:
- MainActivity.kt (简化为纯 UI 代码)
```

---

### 任务 1.2：拆分 MainActivity

**优先级**: 🔴 高  
**预计时间**: 0.5天  
**依赖**: 任务 1.1

**现状问题**:
- MainActivity.kt ~400行，包含过多职责
- 配置加载、更新检查、对话框管理全在一起

**目标**:
```
MainActivity 只负责 UI 渲染和事件分发
```

**实施步骤**:
1. 配置加载逻辑 → MainViewModel
2. 更新检查逻辑 → UpdateManager（已存在，需优化）
3. 对话框状态 → 各自的 ViewModel

**验收标准**:
- [ ] MainActivity < 150行
- [ ] 不包含业务逻辑
- [ ] 职责单一

---

### 任务 1.3：拆分 ChenyiAccessibilityService

**优先级**: 🔴 高  
**预计时间**: 1天

**现状问题**:
- ChenyiAccessibilityService.kt ~800行
- 包含所有工具执行逻辑

**目标**:
```
使用策略模式，每个工具独立为 ToolExecutor
```

**实施步骤**:
1. 创建 `ToolExecutor` 接口
2. 为每个工具创建独立的 Executor 类
3. 创建 `ToolExecutorRegistry` 管理所有执行器
4. 重构 ChenyiAccessibilityService 为调度器

**文件清单**:
```
新建:
- tools/ToolExecutor.kt (接口)
- tools/executors/ScreenshotExecutor.kt
- tools/executors/OcrExecutor.kt
- tools/executors/TapExecutor.kt
- tools/executors/SwipeExecutor.kt
- tools/executors/InputExecutor.kt
- tools/executors/AppExecutor.kt
- tools/executors/SystemExecutor.kt
- tools/ToolExecutorRegistry.kt

修改:
- ChenyiAccessibilityService.kt (精简为调度器)
```

**验收标准**:
- [ ] ChenyiAccessibilityService < 200行
- [ ] 每个工具独立文件
- [ ] 易于扩展新工具

---

### 任务 1.4：统一 Theme 配置

**优先级**: 🔴 高  
**预计时间**: 0.5天

**现状问题**:
- `Theme.kt` (根包) 和 `ui/theme/Theme.kt` 重复
- 样式混乱

**目标**:
```
删除重复，统一使用 ui/theme/ChenyiAgentTheme
```

**实施步骤**:
1. 确认使用哪个主题（推荐 ui/theme/）
2. 删除根包的 Theme.kt
3. 更新所有引用
4. 统一颜色命名（AccentCyan → AccentBlue）

**验收标准**:
- [ ] 只有一个 Theme 文件
- [ ] 所有 Composable 使用统一主题
- [ ] 颜色命名准确

---

### 任务 1.5：创建通用 Result 类

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
- Result 类在 Kernel.kt 中定义
- 各组件重复定义类似的结果封装

**目标**:
```
创建通用的 Result 封装类
```

**实施步骤**:
1. 创建 `common/Result.kt`
2. 定义泛型 Result<Success, Error>
3. 替换所有重复的 Result 定义

**验收标准**:
- [ ] 统一的 Result 类
- [ ] 支持成功/失败/加载状态
- [ ] 易于扩展

---

## 🔒 阶段二：安全加固（高优先级）

### 任务 2.1：使用正式的 Release Keystore

**优先级**: 🔴 高  
**预计时间**: 0.5天

**现状问题**:
```kotlin
// build.gradle.kts
create("release") {
    storeFile = file("debug.keystore")  // ❌ 使用 debug keystore
}
```

**目标**:
```
使用正式的 release keystore 进行签名
```

**实施步骤**:
1. 生成正式的 release keystore
2. 将 keystore 添加到 GitHub Secrets（Base64编码）
3. 修改 build.gradle.kts 使用正式签名
4. 更新 GitHub Actions workflow

**验收标准**:
- [ ] Release APK 使用正式签名
- [ ] Keystore 安全存储在 GitHub Secrets
- [ ] 签名验证通过

---

### 任务 2.2：完成签名验证

**优先级**: 🔴 高  
**预计时间**: 1天

**现状问题**:
```kotlin
// HotUpdateManager.kt
fun verifyLibrary(path: String): Boolean {
    // TODO: 实现签名验证
    return true  // ❌ 未验证
}
```

**目标**:
```
实现完整的 APK 签名验证
```

**实施步骤**:
1. 获取应用的签名信息
2. 验证下载的 APK 签名
3. 比对签名是否一致
4. 记录验证日志

**验收标准**:
- [ ] 验证下载 APK 的签名
- [ ] 拒绝签名不一致的 APK
- [ ] 记录验证失败原因

---

### 任务 2.3：移除硬编码坐标

**优先级**: 🔴 高  
**预计时间**: 0.5天

**现状问题**:
```kotlin
// AutoTask.kt
TapStep(x = 610, y = 2550)  // ❌ 硬编码坐标，不适配不同屏幕
```

**目标**:
```
使用相对坐标或元素定位
```

**实施步骤**:
1. 创建 `ScreenAdapter` 工具类
2. 使用相对坐标（百分比）
3. 或使用元素定位（文本、ID）
4. 添加屏幕尺寸适配逻辑

**验收标准**:
- [ ] 不使用硬编码坐标
- [ ] 支持不同屏幕尺寸
- [ ] 自动任务在不同设备上正常工作

---

## 📝 阶段三：代码规范（中优先级）

### 任务 3.1：定义有意义常量

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
```kotlin
// 魔法数字
maxTokens = 4096
timeout = 120_000L
maxRounds = 10
```

**目标**:
```
所有魔法数字定义为有意义的常量
```

**实施步骤**:
1. 创建 `Constants.kt`
2. 定义常量：
   - MAX_TOKENS = 4096
   - TIMEOUT_MS = 120_000L
   - MAX_TOOL_ROUNDS = 10
   - MAX_COMPRESSION_THRESHOLD = 4000
3. 替换所有魔法数字

**验收标准**:
- [ ] 所有数字有明确含义
- [ ] 常量集中管理
- [ ] 易于修改配置

---

### 任务 3.2：统一错误处理

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
```kotlin
Result.error(e.message ?: "执行失败")  // ❌ 通用错误信息
```

**目标**:
```
提供详细的错误上下文和错误码
```

**实施步骤**:
1. 创建 `ErrorCode` 枚举
2. 创建 `AppException` 自定义异常
3. 统一错误处理逻辑
4. 记录完整堆栈

**验收标准**:
- [ ] 错误包含错误码
- [ ] 错误信息详细
- [ ] 记录完整堆栈

---

### 任务 3.3：优化包结构

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
```
com.chenyi.agent/
├── data/ConfigManager.kt
├── Session.kt  ← 数据类在根包
├── AutoTask.kt ← 数据类在根包
└── ...
```

**目标**:
```
合理的包结构
```

**实施步骤**:
```
com.chenyi.agent/
├── data/
│   ├── model/
│   │   ├── Session.kt
│   │   ├── AutoTask.kt
│   │   └── UserProfile.kt
│   ├── repository/
│   └── ConfigManager.kt
├── ui/
│   ├── theme/
│   ├── components/
│   └── viewmodel/
├── tools/
│   ├── executor/
│   └── ToolManager.kt
├── service/
│   └── ChenyiAccessibilityService.kt
├── common/
│   ├── Constants.kt
│   ├── Result.kt
│   └── Extensions.kt
└── MainActivity.kt
```

**验收标准**:
- [ ] 包结构清晰
- [ ] 职责分离
- [ ] 易于查找文件

---

### 任务 3.4：添加单元测试

**优先级**: 🟡 中  
**预计时间**: 1天

**现状问题**:
- 整个项目缺少单元测试

**目标**:
```
关键逻辑有单元测试覆盖
```

**实施步骤**:
1. 添加测试依赖
2. 为以下类添加测试：
   - ConfigManager
   - SessionManager
   - TaskManager
   - ToolExecutorRegistry
3. 配置 CI 运行测试

**验收标准**:
- [ ] 核心类测试覆盖率 > 60%
- [ ] CI 自动运行测试
- [ ] 关键逻辑有测试保障

---

## ⚡ 阶段四：性能优化

### 任务 4.1：异步文件 IO

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
```kotlin
// SessionManager.kt
val content = file.readText()  // ❌ 同步读取
```

**目标**:
```
使用协程进行异步文件操作
```

**实施步骤**:
1. 将文件操作改为协程
2. 使用 `Dispatchers.IO`
3. 添加错误处理

**验收标准**:
- [ ] 文件操作不阻塞主线程
- [ ] 使用协程
- [ ] 错误处理完善

---

### 任务 4.2：优化 Bitmap 使用

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
- Bitmap 未及时释放
- 大图 Base64 编码占用内存

**目标**:
```
及时释放 Bitmap，优化内存使用
```

**实施步骤**:
1. 确保所有 Bitmap 使用后 recycle()
2. 使用 `use` 扩展函数
3. 考虑图片压缩

**验收标准**:
- [ ] Bitmap 及时释放
- [ ] 无内存泄漏
- [ ] 内存占用优化

---

### 任务 4.3：OCR 初始化优化

**优先级**: 🟡 中  
**预计时间**: 0.5天

**现状问题**:
```kotlin
Thread { ocrEngine = OcrEngine() }.start()  // ❌ 未等待完成
```

**目标**:
```
使用协程 + StateFlow 管理 OCR 状态
```

**实施步骤**:
1. 使用协程初始化 OCR
2. 添加初始化状态 StateFlow
3. 确保 OCR 使用前已初始化

**验收标准**:
- [ ] OCR 状态可观察
- [ ] 使用前确保初始化完成
- [ ] 无竞态条件

---

## 🧹 阶段五：清理工作

### 任务 5.1：删除未使用的代码

**优先级**: 🟢 低  
**预计时间**: 0.5天

**任务清单**:
- [ ] 删除废弃的 `setToolCallback` 方法
- [ ] 删除未使用的导入
- [ ] 删除未使用的 Preview 组件
- [ ] 清理 TODO 注释（或完成）

---

### 任务 5.2：完善文档

**优先级**: 🟢 低  
**预计时间**: 0.5天

**任务清单**:
- [ ] 更新 README.md
- [ ] 更新架构图
- [ ] 添加代码注释
- [ ] 更新 API 文档

---

### 任务 5.3：添加错误追踪

**优先级**: 🟢 低  
**预计时间**: 0.5天

**任务清单**:
- [ ] 集成 Crashlytics 或类似工具
- [ ] 配置错误上报
- [ ] 添加关键事件日志

---

## 📊 进度跟踪

### 如何使用此计划

1. **每完成一个任务**，更新状态：
   - 🔴 未开始 → 🟡 进行中 → ✅ 已完成

2. **更新任务详情**：
   - 记录实际花费时间
   - 记录遇到的问题
   - 更新文件清单

3. **提交代码时**：
   - 引用任务编号（如 "任务 1.1: 引入 MVVM 架构"）
   - 更新 CHANGELOG_DEV.md

---

## 📝 执行日志

### 2026-05-26
- 创建代码质量优化任务计划
- 共 18 个任务，预计 6-10 天完成

---

## ⚠️ 注意事项

1. **每个阶段完成后提交一次代码**，避免大量改动
2. **优先完成高严重性问题**，不要跳过
3. **更新技能文档**，记录新发现的问题
4. **测试每个改动**，确保功能正常

---

**维护者**: ChenYi Team  
**最后更新**: 2026-05-26
