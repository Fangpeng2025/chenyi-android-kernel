# 更新日志

## [1.0.22] - 2026-05-26

### 新增 ✨
- MVVM 架构重构
  - MainViewModel: 主界面状态管理
  - ChatViewModel: 聊天逻辑分离
  - UpdateViewModel: 更新逻辑分离
  - ConfigRepository: 配置数据仓库

- 工具类拆分
  - ToolExecutor: 工具执行器
  - UITreeBuilder: UI 树构建
  - OcrTool: OCR 识别工具
  - GestureExecutor: 手势执行器

- 工具函数
  - Constants: 常量集中管理
  - ErrorHandler: 统一错误处理
  - BitmapUtils: Bitmap 内存优化
  - FileIOUtils: 异步文件 IO
  - OcrInitializer: OCR 初始化管理

- 单元测试
  - ChatViewModelTest
  - ErrorHandlerTest

### 优化 ⚡
- MainActivity: 384行 → 314行 (-18%)
- 删除重复 Theme.kt 文件
- 异步文件操作，避免阻塞主线程
- Bitmap 内存管理优化

### 修复 🐛
- 修复对话框状态管理
- 修复配置加载逻辑

### 文档 📚
- CODE_QUALITY_PLAN.md: 代码质量优化计划
- CODE_QUALITY_TRACKER.md: 任务跟踪
- CODE_QUALITY_REPORT.md: 完成报告
- PACKAGE_STRUCTURE.md: 包结构文档

---

## [1.0.21] - 2026-05-25

### 新增
- 会话管理功能
- 任务管理可视化
- 工具执行 UI

### 优化
- UI 性能优化
- 内存泄漏修复

---

## [1.0.20] - 2026-05-24

### 新增
- 仿微信 UI 设计
- 三个标签页（聊天/技能/设置）
- Material Design 3

---

## [1.0.1] - 2026-05-15

### 新增
- Rust 内核集成
- JNI 接口
- 基础工具（截图、点击、滑动）

---

## [1.0.0] - 2026-05-01

### 新增
- 项目初始化
- 基础架构搭建
