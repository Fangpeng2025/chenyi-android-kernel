# 代码质量优化执行指南

## 快速开始

### 第一次执行时

```bash
# 1. 创建新分支
git checkout -b code-quality-optimization

# 2. 查看待办任务
cat CODE_QUALITY_PLAN.md | grep "### 任务" | head -5
```

### 每次执行任务时

```bash
# 1. 选择一个任务（建议按顺序）
# 阶段一：架构重构
# 任务 1.1 → 1.2 → 1.3 → 1.4 → 1.5

# 2. 阅读任务详情
# 打开 CODE_QUALITY_PLAN.md 找到对应任务

# 3. 执行修改
# 按照实施步骤逐步完成

# 4. 提交代码
git add -A
git commit -m "refactor: 任务 X.X - 任务名称

详细说明:
- 修改了什么
- 为什么修改
- 影响范围"

# 5. 更新进度
# 在 CODE_QUALITY_PLAN.md 中标记任务为 ✅ 已完成
```

---

## 任务优先级顺序

### 第一周：架构重构（最重要）

```
Day 1-2:
  ✅ 任务 1.1: 引入 MVVM 架构
  ✅ 任务 1.2: 拆分 MainActivity

Day 3:
  ✅ 任务 1.3: 拆分 ChenyiAccessibilityService

Day 4:
  ✅ 任务 1.4: 统一 Theme 配置
  ✅ 任务 1.5: 创建通用 Result 类

Day 5:
  ✅ 合并代码到 master
  ✅ 测试所有功能
```

### 第二周：安全加固 + 代码规范

```
Day 6:
  ✅ 任务 2.1: 使用正式的 Release Keystore
  ✅ 任务 2.2: 完成签名验证
  ✅ 任务 2.3: 移除硬编码坐标

Day 7:
  ✅ 任务 3.1: 定义有意义常量
  ✅ 任务 3.2: 统一错误处理
  ✅ 任务 3.3: 优化包结构
  ✅ 任务 3.4: 添加单元测试
```

### 第三周：性能优化 + 清理

```
Day 8:
  ✅ 任务 4.1: 异步文件 IO
  ✅ 任务 4.2: 优化 Bitmap 使用
  ✅ 任务 4.3: OCR 初始化优化

Day 9:
  ✅ 任务 5.1: 删除未使用的代码
  ✅ 任务 5.2: 完善文档
  ✅ 任务 5.3: 添加错误追踪

Day 10:
  ✅ 最终测试
  ✅ 发布新版本
```

---

## 每个任务的执行模板

### 模板：任务 X.X

```markdown
## 执行任务 X.X: 任务名称

**开始时间**: YYYY-MM-DD HH:MM
**预计时间**: X 小时

### Step 1: 理解问题
- 现状是什么？
- 为什么不合理？
- 目标是什么？

### Step 2: 阅读代码
- 找到相关文件
- 理解现有逻辑
- 确认影响范围

### Step 3: 实施修改
- 按照实施步骤逐步修改
- 每完成一步检查一次
- 确保语法正确

### Step 4: 测试验证
- 编译通过
- 功能正常
- 无副作用

### Step 5: 提交代码
git add -A
git commit -m "refactor: 任务 X.X - 任务名称"

### Step 6: 更新文档
- 更新 CODE_QUALITY_PLAN.md（标记完成）
- 更新 CHANGELOG_DEV.md（记录改动）

**完成时间**: YYYY-MM-DD HH:MM
**实际耗时**: X 小时
**遇到问题**: [记录问题]
**解决方案**: [记录方案]
```

---

## 检查清单

### 每次提交前

- [ ] 代码编译通过
- [ ] 功能测试正常
- [ ] 无明显性能问题
- [ ] 提交信息清晰
- [ ] 更新了文档

### 每个阶段完成后

- [ ] 所有任务完成
- [ ] 测试覆盖完整
- [ ] 代码审查通过
- [ ] 文档更新完整
- [ ] 准备合并到 master

---

## 常见问题

### Q: 任务太大，一次完成不了怎么办？
A: 可以拆分为多个小任务，分多次提交。例如任务 1.3 可以拆分为：
- 1.3.1 创建 ToolExecutor 接口
- 1.3.2 创建第一个 Executor
- 1.3.3 创建剩余 Executor
- 1.3.4 重构 Service

### Q: 修改后功能不正常怎么办？
A:
1. 检查最近的修改
2. 查看错误日志
3. 对比修改前后代码
4. 必要时回滚

### Q: 发现新的问题怎么办？
A:
1. 记录到 CODE_QUALITY_PLAN.md 末尾
2. 评估严重程度
3. 决定是立即修复还是加入待办

---

## 资源链接

- 项目文档: DEVELOPMENT_CONTEXT.md
- 修改日志: CHANGELOG_DEV.md
- 快速参考: DEV_QUICK_REF.md
- 技能文档: ~/AppData/Local/hermes/skills/android-automation/chenyi-android-development/

---

**最后更新**: 2026-05-26
