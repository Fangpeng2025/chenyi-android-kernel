//! Agent 内核

use crate::types::{KernelConfig, Result, Error};
use crate::memory::MemoryEngine;
use crate::llm::{LlmClient, ChatMessage, ToolCall};
use crate::storage::StorageEngine;
use crate::tools::ToolRegistry;
use parking_lot::Mutex;
use std::sync::Arc;
use std::time::{Duration, Instant};

/// 最大工具调用轮数
const MAX_TOOL_ROUNDS: usize = 10;

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
        let llm = Arc::new(LlmClient::new(config.llm.clone())?);
        
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
    
    /// 获取工具注册表
    pub fn tools(&self) -> Arc<ToolRegistry> {
        self.tools.clone()
    }
    
    /// 处理消息（带工具调用循环）
    pub async fn chat(&self, message: &str) -> Result<String> {
        let mut messages = vec![
            ChatMessage::system("你是晨翼Agent，一个智能助手。你可以使用工具来帮助用户完成任务。"),
        ];
        
        // 1. 获取上下文
        let context = self.memory.lock().get_context(message)?;
        if !context.is_empty() {
            messages.push(ChatMessage::user(&format!("相关记忆：\n{}\n\n用户问题：{}", context, message)));
        } else {
            messages.push(ChatMessage::user(message));
        }
        
        // 2. 工具调用循环
        let mut response_text = String::new();
        let mut rounds = 0;
        
        loop {
            if rounds >= MAX_TOOL_ROUNDS {
                response_text = "达到最大工具调用轮数，请简化请求。".to_string();
                break;
            }
            
            // 调用 LLM
            let response = self.llm.chat_with_tools(&messages, self.tools.list_tools()).await?;
            
            // 检查是否有工具调用
            if response.tool_calls.is_empty() {
                // 没有工具调用，返回文本响应
                response_text = response.content.unwrap_or_else(|| "无响应".to_string());
                break;
            }
            
            // 执行工具调用
            for tool_call in &response.tool_calls {
                eprintln!("[Agent] 执行工具: {} ({})", tool_call.function.name, tool_call.function.arguments);
                
                let tool_result = self.tools.execute(&tool_call.function.name, &tool_call.function.arguments);
                
                // 将工具结果添加到消息
                let content = response.content.as_deref().unwrap_or("");
                messages.push(ChatMessage::assistant_with_tools(
                    content,
                    response.tool_calls.clone(),
                ));
                
                let result_json = match tool_result {
                    Ok(v) => serde_json::to_string(&v).unwrap_or_else(|_| "{}".to_string()),
                    Err(e) => serde_json::json!({"error": e.to_string()}).to_string(),
                };
                
                messages.push(ChatMessage::tool_result(&tool_call.id, &result_json));
            }
            
            rounds += 1;
        }
        
        // 3. 存储记忆
        self.memory.lock().add_message(message, &response_text)?;
        
        Ok(response_text)
    }
    
    /// 执行单个工具
    pub fn execute_tool(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        self.tools.execute(name, params)
    }
    
    /// 获取内核状态
    pub fn status(&self) -> KernelStatus {
        KernelStatus {
            initialized: self.initialized,
            data_dir: self.config.data_dir.to_string_lossy().to_string(),
            memory_entries: self.memory.lock().count(),
            tools_count: self.tools.count(),
        }
    }
}

/// 内核状态
#[derive(Debug, serde::Serialize)]
pub struct KernelStatus {
    pub initialized: bool,
    pub data_dir: String,
    pub memory_entries: usize,
    pub tools_count: usize,
}