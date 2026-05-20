# 晨翼Agent UI 美化方案 - Material Design 3

## 🎨 设计语言

采用 **Material Design 3** (Material You) 设计语言，这是 Google 最新的设计系统，具有以下特点：

### 核心原则
1. **动态颜色** - 根据用户壁纸自动生成配色方案
2. **圆角设计** - 大量使用圆角，视觉更柔和
3. **层次分明** - 通过阴影和颜色区分层次
4. **动画流畅** - 丰富的过渡动画
5. **无障碍** - 高对比度，易于阅读

## 📐 设计规范

### 1. 颜色系统

#### 主色调（紫色系）
- Primary: `#6750A4` - 主要操作按钮
- Primary Container: `#EADDFF` - 卡片背景
- On Primary: `#FFFFFF` - 按钮文字

#### 辅助色
- Secondary: `#625B71` - 次要元素
- Tertiary: `#7D5260` - 强调元素
- Error: `#B3261E` - 错误状态

#### 表面色
- Background: `#FFFBFE` - 页面背景
- Surface: `#FFFBFE` - 卡片背景
- Surface Variant: `#E7E0EC` - 分组背景

### 2. 字体系统

#### 字号层级
```
Display Large: 57sp - 欢迎页大标题
Display Medium: 45sp - 页面标题
Display Small: 36sp - 区块标题

Headline Large: 32sp - 重要标题
Headline Medium: 28sp - 卡片标题
Headline Small: 24sp - 小标题

Title Large: 22sp - 列表标题
Title Medium: 16sp - 卡片标题
Title Small: 14sp - 小标题

Body Large: 16sp - 正文
Body Medium: 14sp - 辅助文字
Body Small: 12sp - 说明文字

Label Large: 14sp - 按钮文字
Label Medium: 12sp - 标签文字
Label Small: 11sp - 小标签
```

#### 字重
- Bold: 标题、重要信息
- SemiBold: 卡片标题
- Medium: 按钮、标签
- Normal: 正文

### 3. 形状系统

#### 圆角半径
```
Small: 8dp - 按钮、文本框
Medium: 16dp - 卡片
Large: 28dp - 对话框
Extra Large: 32dp - 底部导航栏
```

### 4. 间距系统

#### 基础间距
```
4dp - 图标与文字间距
8dp - 元素内间距
12dp - 卡片内间距
16dp - 页面边距
24dp - 区块间距
32dp - 大区块间距
```

## 🧩 组件设计

### 1. 顶部应用栏 (TopAppBar)

#### LargeTopAppBar
- 高度: 152dp
- 标题: Headline Small (24sp, Bold)
- 滚动时收缩为普通高度
- 背景: Surface 颜色
- 阴影: 滚动时显示

#### 权限状态指示器
- 使用 `StatusIndicator` 组件
- 绿色圆点: 已启用
- 橙色圆点: 未启用
- 带脉冲动画

### 2. 卡片 (Card)

#### ElevatedCard
- 形状: Large (28dp)
- 阴影: 2dp
- 背景: Surface
- 内边距: 16dp

#### 消息卡片
- 用户消息: Primary Container 背景
- 助手消息: Surface Variant 背景
- 圆角: Medium (16dp)
- 内边距: 12dp

### 3. 按钮 (Button)

#### FilledButton
- 主要操作
- 背景: Primary
- 文字: On Primary
- 形状: Large (28dp)

#### OutlinedButton
- 次要操作
- 边框: Outline
- 文字: Primary
- 形状: Large (28dp)

#### FAB (FloatingActionButton)
- 位置: 右下角
- 大小: 56dp
- 形状: Circle
- 颜色: Primary Container

### 4. 输入框 (TextField)

#### OutlinedTextField
- 形状: Small (8dp)
- 边框: Outline
- 标签: Label Medium
- 聚焦时: Primary 边框

### 5. 底部导航栏 (BottomAppBar)

#### 设计
- 高度: 80dp
- 背景: Surface
- FAB 切口: 圆形
- 导航项: 4-5 个

### 6. 对话框 (Dialog)

#### AlertDialog
- 形状: Extra Large (32dp)
- 背景: Surface
- 标题: Headline Small
- 内容: Body Medium
- 按钮: TextButton

### 7. 底部抽屉 (BottomSheet)

#### ModalBottomSheet
- 形状: Extra Large (顶部)
- 拖动指示器: 32dp x 4dp
- 背景: Surface
- 内容: 可滚动

## 🎬 动画规范

### 1. 过渡动画

#### 页面切换
```kotlin
animateContentSize(
    animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
)
```

#### 元素出现
```kotlin
animateEnterExit(
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically()
)
```

### 2. 交互反馈

#### 按钮点击
```kotlin
scale(
    animateFloatAsState(
        if (pressed) 0.95f else 1f
    )
)
```

#### 状态变化
```kotlin
animateColorAsState(
    targetValue = if (enabled) Primary else Gray,
    animationSpec = tween(300)
)
```

### 3. 加载动画

#### 进度指示器
- LinearProgressIndicator: 高度 4dp
- CircularProgressIndicator: 直径 24dp
- 颜色: Primary

#### 骨架屏
- 背景: Surface Variant
- 闪烁动画: 1.5s 循环

## 🌈 视觉层次

### 1. 阴影层级

```
Level 0: 无阴影 - 页面背景
Level 1: 1dp - 卡片
Level 2: 2dp - 悬浮卡片
Level 3: 3dp - 对话框
Level 4: 4dp - 底部抽屉
Level 5: 5dp - 导航栏
```

### 2. 颜色层次

```
Level 1: Background - 页面背景
Level 2: Surface - 卡片背景
Level 3: Surface Variant - 分组背景
Level 4: Primary Container - 强调卡片
Level 5: Primary - 按钮、图标
```

### 3. 文字层次

```
Level 1: On Background - 标题
Level 2: On Surface - 正文
Level 3: On Surface Variant - 辅助文字
Level 4: Outline - 禁用文字
```

## 📱 页面布局

### 1. 主页面

```
┌─────────────────────────────────┐
│  LargeTopAppBar (152dp)         │
│  ├─ 标题 + 消息数               │
│  ├─ 权限状态指示器              │
│  └─ 操作按钮                    │
├─────────────────────────────────┤
│  状态卡片 (可选)                │
├─────────────────────────────────┤
│                                 │
│  消息列表 (LazyColumn)          │
│  ├─ 消息卡片 1                  │
│  ├─ 消息卡片 2                  │
│  └─ ...                         │
│                                 │
├─────────────────────────────────┤
│  BottomAppBar (80dp)            │
│  ├─ 输入框                      │
│  └─ 发送按钮 (FAB)              │
└─────────────────────────────────┘
```

### 2. 消息卡片

```
┌─────────────────────────────────┐
│  [头像] 用户/助手    时间戳     │
│                                 │
│  消息内容...                    │
│                                 │
│  ───────────────────────        │
│  工具执行记录 (如有)             │
│  ✓ tap → 成功                   │
│  ✓ swipe → 成功                 │
│                                 │
│  [复制] [删除] [重发]           │
└─────────────────────────────────┘
```

### 3. 任务卡片

```
┌─────────────────────────────────┐
│  [图标] 任务名称      [状态标签] │
│  任务描述...                    │
│                                 │
│  ● ● ● ● ○ (步骤进度)           │
│                                 │
│  [执行] [删除]                  │
└─────────────────────────────────┘
```

## 🎯 设计亮点

### 1. 动态主题
- 支持浅色/深色模式
- Android 12+ 动态颜色
- 平滑的主题切换动画

### 2. 流畅动画
- 所有状态变化都有过渡动画
- 列表项出现/消失动画
- 按钮点击缩放反馈

### 3. 信息层次
- 清晰的视觉层次
- 合理的颜色对比
- 易于扫描的布局

### 4. 无障碍设计
- 高对比度文字
- 足够的点击区域 (48dp)
- 清晰的状态反馈

### 5. 响应式布局
- 适配不同屏幕尺寸
- 横竖屏切换
- 折叠屏适配

## 📊 设计对比

| 元素 | 旧设计 | 新设计 | 改进 |
|------|--------|--------|------|
| **颜色** | 单一蓝色 | 紫色系动态配色 | +100% |
| **圆角** | 4dp | 8-32dp | +700% |
| **阴影** | 无 | 5 级层次 | +∞ |
| **动画** | 无 | 全局过渡动画 | +∞ |
| **字体** | 2 级 | 13 级 | +550% |
| **间距** | 不统一 | 8dp 基础单位 | +100% |

## 🚀 实现清单

### ✅ 已完成
- [x] Material Design 3 主题
- [x] 颜色系统
- [x] 字体系统
- [x] 形状系统
- [x] 现代化组件库
- [x] 状态指示器
- [x] 进度卡片
- [x] 统计卡片
- [x] FAB 组件
- [x] 标签芯片
- [x] 底部抽屉头部
- [x] 空状态组件

### 🔄 待集成
- [ ] 更新 MainActivity 使用新组件
- [ ] 添加深色模式支持
- [ ] 添加动态颜色支持 (Android 12+)
- [ ] 优化动画效果
- [ ] 添加骨架屏加载

## 📝 使用示例

### 应用主题
```kotlin
ChenYiTheme(
    darkTheme = isSystemInDarkTheme(),
    dynamicColor = true  // Android 12+ 动态颜色
) {
    // 你的 UI
}
```

### 使用现代化组件
```kotlin
// 渐变卡片
GradientCard {
    Text("内容")
}

// 状态指示器
StatusIndicator(isOnline = true)

// 进度卡片
ProgressCard(
    title = "任务进度",
    progress = 0.75f,
    subtitle = "3/4 步骤完成"
)

// 统计卡片
StatCard(
    title = "今日任务",
    value = "12",
    icon = Icons.Default.Task,
    trend = 0.15f
)
```

---

**设计理念**: 简洁、现代、流畅、无障碍

**设计工具**: Material Design 3, Jetpack Compose

**设计目标**: 提供最佳的用户体验和视觉享受