# 项目包结构

## 当前结构

```
com.chenyi.agent/
├── data/                    # 数据层
│   ├── ConfigManager.kt     # 配置管理
│   └── ConfigValidator.kt   # 配置验证
│
├── repository/              # 仓库层（新增）
│   └── ConfigRepository.kt  # 配置仓库
│
├── viewmodel/               # ViewModel 层（新增）
│   ├── MainViewModel.kt     # 主 ViewModel
│   ├── ChatViewModel.kt     # 聊天逻辑
│   └── UpdateViewModel.kt   # 更新逻辑
│
├── tools/                   # 工具类（新增）
│   ├── ToolExecutor.kt      # 工具执行器
│   ├── Result.kt            # 统一结果类
│   ├── UITreeBuilder.kt     # UI 树构建
│   ├── OcrTool.kt           # OCR 工具
│   └── GestureExecutor.kt   # 手势执行
│
├── utils/                   # 工具类（新增）
│   ├── ErrorHandler.kt      # 错误处理
│   ├── BitmapUtils.kt       # Bitmap 优化
│   ├── FileIOUtils.kt       # 异步文件 IO
│   └── OcrInitializer.kt    # OCR 初始化
│
├── ui/                      # UI 层
│   ├── components/          # UI 组件
│   │   ├── ChatScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── ToolsScreen.kt
│   │   ├── SessionListScreen.kt
│   │   └── ...
│   └── theme/               # 主题
│       ├── Theme.kt
│       ├── Color.kt
│       └── Typography.kt
│
├── ChenyiAccessibilityService.kt  # 无障碍服务（待重构）
├── MainActivity.kt                 # 主 Activity
├── Kernel.kt                       # Rust 内核接口
├── OcrEngine.kt                    # OCR 引擎
├── Session.kt                      # 会话模型
├── SessionManager.kt               # 会话管理
├── Constants.kt                    # 常量定义（新增）
└── ... 其他文件

```

## 改进建议

### 1. 服务层分离
```
service/
├── AccessibilityServiceDelegate.kt  # 无障碍服务代理
├── ScreenshotService.kt             # 截图服务
└── UpdateService.kt                 # 更新服务
```

### 2. 模型层明确
```
model/
├── Session.kt
├── Message.kt
├── ToolExecution.kt
└── AppConfig.kt
```

### 3. 通知管理
```
notification/
└── NotificationManager.kt
```

## 迁移计划

### 阶段 1: 新增目录已 ✅
- [x] viewmodel/
- [x] repository/
- [x] tools/
- [x] utils/

### 阶段 2: 文件迁移（建议）
- [ ] Session.kt → model/Session.kt
- [ ] SessionManager.kt → manager/SessionManager.kt
- [ ] TaskManager.kt → manager/TaskManager.kt

### 阶段 3: 服务重构
- [ ] 拆分 ChenyiAccessibilityService
- [ ] 创建服务代理模式

---

**维护**: 保持包结构清晰，职责单一
