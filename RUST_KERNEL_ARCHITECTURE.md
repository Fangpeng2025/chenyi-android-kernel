# 晨翼Agent Rust内核架构规划

## 🎯 目标：完全复刻 Hermes Agent 的核心架构

## 📊 Hermes 核心架构分析

### 1️⃣ 技能系统 (Skills)
```
技能 = SKILL.md (YAML前置 + Markdown说明)
├── 定义：如何完成特定任务
├── 触发：用户意图匹配
├── 执行：调用工具组合
└── 示例：claude-code、peekaboo-android等
```

**关键特性**：
- 技能存储在 `~/.hermes/skills/` 目录
- SKILL.md 格式：YAML frontmatter + Markdown body
- 技能定义了工作流程、工具调用、验证步骤
- Agent 根据用户意图匹配合适的技能

### 2️⃣ 工具系统 (Tools)
```
工具 = 可执行的原子操作
├── 内置工具：terminal、read_file、write_file、patch等
├── MCP工具：通过MCP协议连接外部工具
├── 工具定义：name、description、parameters(JSON Schema)
└── 工具执行：Agent解析LLM响应，调用工具
```

**关键特性**：
- 工具通过 `ToolRegistry` 注册
- LLM 返回 TOOL_CALLS_REQUIRED 时，Agent执行工具
- 工具执行结果反馈给LLM，继续对话循环

### 3️⃣ MCP协议 (Model Context Protocol)
```
MCP = 连接外部工具/技能的标准协议
├── MCP Client：连接MCP Server
├── MCP Server：提供工具列表
├── 工具发现：启动时调用 list_tools()
├── 工具调用：调用 call_tool(name, args)
└── 命名规则：mcp_{server}_{tool}
```

**关键特性**：
- MCP Server 通过 config.yaml 配置
- 支持 stdio（命令行）和 HTTP 两种传输
- 工具自动发现并注入到工具集
- Peekaboo 就是一个 MCP Server

### 4️⃣ LLM集成
```
chat_with_tools 循环：
1. 发送消息 + 工具定义 → LLM
2. LLM返回 → 文本 or TOOL_CALLS_REQUIRED
3. 如果是工具调用 → 执行工具
4. 工具结果 → 继续调用LLM
5. 循环直到LLM返回最终文本
```

---

## 🚀 Rust内核实施计划

### Phase 1: 技能系统（核心）

#### 文件结构
```
src/
├── skills/
│   ├── mod.rs          # 技能管理器
│   ├── skill.rs        # 技能定义
│   ├── loader.rs       # 技能加载器
│   ├── registry.rs     # 技能注册表
│   └── matcher.rs      # 技能匹配器
└── ...
```

#### 关键实现
```rust
// src/skills/skill.rs
pub struct Skill {
    pub name: String,
    pub description: String,
    pub version: String,
    pub author: String,
    pub platforms: Vec<String>,
    pub metadata: serde_json::Value,
    pub content: String,  // Markdown内容
}

// src/skills/loader.rs
pub struct SkillLoader {
    skills_dir: PathBuf,
}

impl SkillLoader {
    pub fn load_skills(&self) -> Result<Vec<Skill>> {
        // 从目录加载所有SKILL.md
        // 解析YAML frontmatter + Markdown
    }
}

// src/skills/registry.rs
pub struct SkillRegistry {
    skills: HashMap<String, Skill>,
}

impl SkillRegistry {
    pub fn find_skill(&self, intent: &str) -> Option<&Skill> {
        // 根据用户意图匹配技能
        // 关键词匹配、描述匹配等
    }
}
```

### Phase 2: MCP协议（关键）

#### 文件结构
```
src/
├── mcp/
│   ├── mod.rs          # MCP模块
│   ├── client.rs       # MCP客户端
│   ├── protocol.rs     # MCP协议定义
│   ├── transport.rs    # 传输层（stdio/http）
│   └── discovery.rs    # 工具发现
└── ...
```

#### 关键实现
```rust
// src/mcp/client.rs
pub struct McpClient {
    server_name: String,
    transport: McpTransport,
}

impl McpClient {
    pub async fn connect(&mut self) -> Result<()> {
        // 连接MCP Server
    }
    
    pub async fn list_tools(&self) -> Result<Vec<ToolDefinition>> {
        // 发现可用工具
    }
    
    pub async fn call_tool(&self, name: &str, args: Value) -> Result<Value> {
        // 调用工具
    }
}

// src/mcp/protocol.rs
pub enum McpTransport {
    Stdio {
        command: String,
        args: Vec<String>,
        env: HashMap<String, String>,
    },
    Http {
        url: String,
        headers: HashMap<String, String>,
    },
}
```

### Phase 3: 工具系统（集成）

#### 文件结构
```
src/
├── tools/
│   ├── mod.rs          # 工具管理器（已存在）
│   ├── registry.rs     # 工具注册表（已存在）
│   ├── builtin.rs      # 内置工具
│   └── mcp_bridge.rs   # MCP工具桥接
└── ...
```

#### 关键实现
```rust
// src/tools/mcp_bridge.rs
pub struct McpToolBridge {
    mcp_clients: Vec<McpClient>,
}

impl McpToolBridge {
    pub async fn discover_mcp_tools(&mut self, config: &Config) -> Result<()> {
        // 从config.yaml读取MCP服务器配置
        // 连接每个服务器
        // 发现工具并注册到ToolRegistry
    }
}
```

### Phase 4: Agent核心（编排）

#### 文件结构
```
src/
├── agent/
│   ├── mod.rs          # Agent核心（已存在）
│   ├── orchestrator.rs # 编排器
│   └── workflow.rs     # 工作流执行
└── ...
```

#### 关键实现
```rust
// src/agent/orchestrator.rs
pub struct Orchestrator {
    skill_registry: SkillRegistry,
    tool_registry: Arc<ToolRegistry>,
    mcp_bridge: McpToolBridge,
}

impl Orchestrator {
    pub async fn execute_user_request(&self, request: &str) -> Result<String> {
        // 1. 匹配技能
        let skill = self.skill_registry.find_skill(request);
        
        // 2. 如果找到技能，执行技能工作流
        if let Some(skill) = skill {
            return self.execute_skill(skill, request).await;
        }
        
        // 3. 否则，使用LLM + 工具循环
        self.chat_with_tools(request).await
    }
    
    pub async fn execute_skill(&self, skill: &Skill, request: &str) -> Result<String> {
        // 解析技能内容
        // 执行技能定义的工作流
        // 调用相应工具
    }
}
```

---

## 📋 实施优先级

### 优先级1：技能系统（最高）
**原因**：核心架构，决定Agent如何工作
**工作量**：3-5天
**关键文件**：
- `src/skills/mod.rs`
- `src/skills/skill.rs`
- `src/skills/loader.rs`
- `src/skills/registry.rs`

### 优先级2：MCP协议（关键）
**原因**：连接外部工具（Peekaboo）的标准方式
**工作量**：5-7天
**关键文件**：
- `src/mcp/mod.rs`
- `src/mcp/client.rs`
- `src/mcp/protocol.rs`

### 优先级3：工具系统扩展（集成）
**原因**：集成MCP工具到现有工具系统
**工作量**：2-3天
**关键文件**：
- `src/tools/mcp_bridge.rs`

### 优先级4：编排器（统一）
**原因**：统一技能和工具的调用流程
**工作量**：2-3天
**关键文件**：
- `src/agent/orchestrator.rs`

---

## 🔧 配置文件设计

### config.yaml
```yaml
# 模型配置
model: glm-5
provider: custom:oneapi

# OneAPI配置
custom_providers:
  oneapi:
    base_url: https://oneapi.xintiandi.online/v1
    api_key: sk-xxx

# MCP服务器配置
mcp_servers:
  peekaboo-android:
    command: node
    args: ["bin/peekaboo-android-mcp.js"]
    cwd: "C:\\Users\\Administrator\\peekaboo-android"
    timeout: 120

# 技能目录
skills_dir: "C:\\Users\\Administrator\\.hermes\\skills"
```

---

## 🎯 最终架构

```
晨翼Agent App
├── 前端（Kotlin）
│   ├── MainActivity（UI）
│   ├── OperationOverlayService（覆盖层）
│   ├── AgentKeepAliveService（前台服务）
│   └── ChenyiAccessibilityService（无障碍）
│
├── 后端（Rust内核）
│   ├── Orchestrator（编排器）
│   │   ├── SkillRegistry（技能注册表）
│   │   ├── ToolRegistry（工具注册表）
│   │   └── McpBridge（MCP桥接）
│   │
│   ├── Skills（技能系统）
│   │   ├── SkillLoader（加载器）
│   │   ├── SkillRegistry（注册表）
│   │   └── SkillMatcher（匹配器）
│   │
│   ├── MCP（MCP协议）
│   │   ├── McpClient（客户端）
│   │   ├── McpTransport（传输层）
│   │   └── ToolDiscovery（工具发现）
│   │
│   ├── Tools（工具系统）
│   │   ├── BuiltinTools（内置工具）
│   │   ├── McpTools（MCP工具）
│   │   └── KotlinTools（Kotlin回调）
│   │
│   └── LLM（LLM集成）
│   │   ├── chat_with_tools（工具循环）
│   │   └── ToolCallParser（解析器）
│
└── 外部MCP服务器
    ├── Peekaboo Android MCP Server
    │   └── 通过WebSocket连接鹏程万里APP
    │
    └── 其他MCP服务器
        └── GitHub、filesystem等
```

---

## 📌 关键区别

### Hermes（Python）vs 晨翼Agent（Rust）

| 模块 | Hermes (Python) | 晨翼Agent (Rust) |
|-----|----------------|-----------------|
| 技能系统 | SKILL.md + Python加载 | SKILL.md + Rust加载 |
| MCP协议 | mcp Python包 | 自实现MCP Client（Rust）|
| 工具执行 | Python asyncio | Rust async/await |
| LLM调用 | Python requests | Rust reqwest |
| 性能 | 中 | 高（Rust优势）|
| 部署 | Python环境 | 单一二进制 |

---

## 🚀 开始实施

下一步：立即实现技能系统核心模块