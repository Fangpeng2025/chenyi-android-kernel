//! Agent 内核

use crate::types::{KernelConfig, Result, Error};
use crate::memory::MemoryEngine;
use crate::llm::LlmClient;
use crate::storage::StorageEngine;
use crate::tools::ToolRegistry;
use parking_lot::Mutex;
use std::sync::Arc;

/// Agent 内核
pub struct AgentKernel {
    /// 配置
    config: KernelConfig,
    
    /// 记忆引擎
    memory: Arc<Mutex<MemoryEngine>>,
    
    /// LLM 客户端
    llm: Arc<LlmClient>,
    
    /// 存储引擎
    storage: Arc<StorageEngine>,
    
    /// 工具注册表
    tools: Arc<ToolRegistry>,
    
    /// 是否已初始化
    initialized: bool,
}

impl AgentKernel {
    /// 创建新的 Agent 内核
    pub fn new(config: KernelConfig) -> Result<Self> {
        // 初始化存储引擎
        let storage = Arc::new(StorageEngine::new(&config.data_dir)?);
        
        // 初始化记忆引擎
        let memory = Arc::new(Mutex::new(
            MemoryEngine::new(config.memory.clone(), storage.clone())?
        ));
        
        // 初始化 LLM 客户端
        let llm = Arc::new(LlmClient::new(config.llm.clone()));
        
        // 初始化工具注册表
        let tools = Arc::new(ToolRegistry::new());
        
        Ok(Self {
            config,
            memory,
            llm,
            storage,
            tools,
            initialized: true,
        })
    }
    
    /// 处理消息
    pub async fn chat(&self, message: &str) -> Result<String> {
        // 1. 获取上下文
        let context = self.memory.lock().get_context(message)?;
        
        // 2. 调用 LLM
        let response = self.llm.chat(message, &context).await?;
        
        // 3. 存储记忆
        self.memory.lock().add_message(message, &response)?;
        
        Ok(response)
    }
    
    /// 执行工具
    pub fn execute_tool(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        self.tools.execute(name, params)
    }
    
    /// 获取内核状态
    pub fn status(&self) -> KernelStatus {
        KernelStatus {
            initialized: self.initialized,
            data_dir: self.config.data_dir.to_string_lossy().to_string(),
            memory_entries: self.memory.lock().count(),
        }
    }
}

/// 内核状态
#[derive(Debug, serde::Serialize)]
pub struct KernelStatus {
    pub initialized: bool,
    pub data_dir: String,
    pub memory_entries: usize,
}