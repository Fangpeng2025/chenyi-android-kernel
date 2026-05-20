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

/// 最大会话历史长度（防止内存溢出）
const MAX_HISTORY_LENGTH: usize = 50;

/// Agent 内核
#[derive(Clone)]
pub struct AgentKernel {
    /// 配置
    config: KernelConfig,
    
    /// 记忆引擎
    memory: Arc<Mutex<MemoryEngine>>,
    
    /// LLM 客户端
    pub llm: Arc<LlmClient>,
    
    /// 存储引擎
    storage: Arc<StorageEngine>,
    
    /// 工具注册表
    tools: Arc<ToolRegistry>,
    
    /// 会话历史（当前对话上下文）
    conversation_history: Arc<Mutex<Vec<ChatMessage>>>,
    
    /// 系统提示词
    system_prompt: String,
    
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
        
        // 初始化会话历史
        let conversation_history = Arc::new(Mutex::new(Vec::new()));
        
        // 默认系统提示词
        let system_prompt = "你是晨翼Agent，一个智能助手。你可以使用工具来帮助用户完成任务。\
             当用户要求操作手机时，使用工具（tap、swipe、ocr等）来完成任务。\
             每次工具调用后，根据结果决定下一步操作。".to_string();
        
        Ok(Self {
            config,
            memory,
            llm,
            storage,
            tools,
            conversation_history,
            system_prompt,
            initialized: true,
        })
    }
    
    /// 设置系统提示词
    pub fn set_system_prompt(&mut self, prompt: String) {
        self.system_prompt = prompt;
        // 清空历史，让下次对话使用新的系统提示词
        self.conversation_history.lock().clear();
        log::info!("[Agent] 系统提示词已更新");
    }
    
    /// 获取工具注册表
    pub fn tools(&self) -> Arc<ToolRegistry> {
        self.tools.clone()
    }
    
    /// 清除会话历史
    pub fn clear_history(&self) {
        self.conversation_history.lock().clear();
        log::info!("[Agent] 会话历史已清除");
    }
    
    /// 获取会话历史长度
    pub fn history_length(&self) -> usize {
        self.conversation_history.lock().len()
    }
    
    /// 处理消息（带工具调用循环）
    pub async fn chat(&self, message: &str) -> Result<String> {
        log::info!("[Agent] 开始处理消息: {}", message);
        
        // 设置整体超时（5 分钟）
        let timeout_duration = Duration::from_secs(300);
        let start_time = Instant::now();
        
        // 获取会话历史
        let mut history = self.conversation_history.lock();
        
        // 如果历史为空，添加系统消息
        if history.is_empty() {
            history.push(ChatMessage::system(&self.system_prompt));
        }
        
        // 1. 获取相关记忆上下文
        let context = self.memory.lock().get_context(message)?;
        let user_content = if !context.is_empty() {
            log::info!("[Agent] 找到相关记忆: {} 字节", context.len());
            format!("相关记忆：\n{}\n\n用户问题：{}", context, message)
        } else {
            message.to_string()
        };
        
        // 添加用户消息到历史
        history.push(ChatMessage::user(&user_content));
        
        // 限制历史长度（保留第一条系统消息）
        if history.len() > MAX_HISTORY_LENGTH {
            // 计算需要删除的数量（保留系统消息）
            let keep_count = MAX_HISTORY_LENGTH - 1; // 保留的系统消息位置
            let current_user_msgs = history.len() - 1; // 当前用户/助手消息数
            let drain_count = current_user_msgs.saturating_sub(keep_count);
            if drain_count > 0 {
                // 删除最早的用户/助手消息（索引 1 到 drain_count）
                history.drain(1..=drain_count);
            }
        }
        
        // 克隆历史用于 LLM 调用（释放锁）
        let messages: Vec<ChatMessage> = history.clone();
        drop(history); // 释放锁
        
        // 2. 调用 LLM
        log::info!("[Agent] 调用 LLM");
        let response = self.llm.chat_with_tools(&messages, self.tools.list_tools()).await?;
        
        // 检查是否有工具调用
        if response.tool_calls.is_empty() {
            // 没有工具调用，返回文本响应
            let response_text = response.content.unwrap_or_else(|| "无响应".to_string());
            log::info!("[Agent] LLM 返回文本响应: {} 字节", response_text.len());
            
            // 更新会话历史（添加助手响应）
            {
                let mut history = self.conversation_history.lock();
                history.push(ChatMessage::assistant(&response_text));
            }
            
            // 存储到长期记忆
            {
                let mut memory = self.memory.lock();
                memory.add_message("user", message)?;
                memory.add_message("assistant", &response_text)?;
            }
            
            return Ok(response_text);
        }
        
        // 有工具调用，检查是否能执行
        log::info!("[Agent] LLM 返回 {} 个工具调用", response.tool_calls.len());
        
        // 检查工具执行回调是否设置
        if !self.tools.has_callback() {
            // 没有回调，返回工具调用信息给 Kotlin 端执行
            log::warn!("[Agent] 没有工具回调，返回给 Kotlin 执行");
            
            // 将当前状态保存到会话历史（包括用户的最新消息和 assistant 的 tool_calls）
            {
                let mut history = self.conversation_history.lock();
                let content = response.content.as_deref().unwrap_or("");
                history.push(ChatMessage::assistant_with_tools(
                    content,
                    response.tool_calls.clone(),
                ));
            }
            
            // 返回特殊标记，表示需要 Kotlin 执行工具
            let tool_calls_json = serde_json::to_string(&response.tool_calls)
                .unwrap_or_else(|_| "[]".to_string());
            
            return Err(Error::Other(format!(
                "TOOL_CALLS_REQUIRED:{}",
                tool_calls_json
            )));
        }
        
        // 有回调，执行工具调用循环
        let mut current_messages = messages;
        let mut response = response;
        let mut rounds = 0;
        let mut final_response_text = String::new();
        
        loop {
            // 检查超时
            if start_time.elapsed() > timeout_duration {
                log::error!("[Agent] 处理超时（{} 秒）", timeout_duration.as_secs());
                final_response_text = "处理超时，请稍后重试。".to_string();
                break;
            }
            
            if rounds >= MAX_TOOL_ROUNDS {
                log::warn!("[Agent] 达到最大工具调用轮数");
                final_response_text = "达到最大工具调用轮数，请简化请求。".to_string();
                break;
            }
            
            // 先添加 assistant 消息（包含所有 tool_calls）
            let content = response.content.as_deref().unwrap_or("");
            current_messages.push(ChatMessage::assistant_with_tools(
                content,
                response.tool_calls.clone(),
            ));
            
            // 执行工具调用
            for tool_call in &response.tool_calls {
                log::info!("[Agent] 执行工具: {} ({})", tool_call.function.name, tool_call.function.arguments);
                
                let tool_result = self.tools.execute(&tool_call.function.name, &tool_call.function.arguments);
                
                let result_json = match tool_result {
                    Ok(v) => {
                        log::info!("[Agent] 工具执行成功");
                        serde_json::to_string(&v).unwrap_or_else(|_| "{}".to_string())
                    },
                    Err(e) => {
                        log::error!("[Agent] 工具执行失败: {}", e);
                        serde_json::json!({"error": e.to_string()}).to_string()
                    },
                };
                
                current_messages.push(ChatMessage::tool_result(&tool_call.id, &result_json));
            }
            
            rounds += 1;
            
            // 再次调用 LLM
            log::info!("[Agent] 第 {} 轮调用 LLM", rounds + 1);
            let next_response = self.llm.chat_with_tools(&current_messages, self.tools.list_tools()).await?;
            
            // 检查是否还有工具调用
            if next_response.tool_calls.is_empty() {
                // 没有更多工具调用，返回最终响应
                final_response_text = next_response.content.unwrap_or_else(|| "无响应".to_string());
                log::info!("[Agent] LLM 返回最终文本响应: {} 字节", final_response_text.len());
                break;
            }
            
            // 继续执行工具
            response = next_response;
        }
        
        // 更新会话历史
        {
            let mut history = self.conversation_history.lock();
            history.push(ChatMessage::assistant(&final_response_text));
            
            if history.len() > MAX_HISTORY_LENGTH {
                let keep_count = MAX_HISTORY_LENGTH - 1;
                let current_user_msgs = history.len() - 1;
                let drain_count = current_user_msgs.saturating_sub(keep_count);
                if drain_count > 0 {
                    history.drain(1..=drain_count);
                }
            }
        }
        
        // 存储到长期记忆
        {
            let mut memory = self.memory.lock();
            memory.add_message("user", message)?;
            memory.add_message("assistant", &final_response_text)?;
        }
        
        Ok(final_response_text)
    }
    
    /// 执行单个工具
    pub fn execute_tool(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        self.tools.execute(name, params)
    }
    
    /// 继续对话（提交工具执行结果）
    /// 当 Kotlin 端执行完工具后，调用此方法继续对话
    pub async fn continue_with_tool_results(&self, tool_results: Vec<(String, String)>) -> Result<String> {
        log::info!("[Agent] 继续对话，提交 {} 个工具结果", tool_results.len());
        
        // 获取会话历史
        let mut history = self.conversation_history.lock();
        
        // 检查最后一条消息是否是 assistant 的 tool_calls
        // 如果不是，说明历史状态异常
        let last_is_tool_calls = history.last().map(|msg| {
            matches!(msg.role.as_str(), "assistant" if !msg.tool_calls.is_empty())
        }).unwrap_or(false);
        
        if !last_is_tool_calls {
            log::warn!("[Agent] 历史状态异常：最后一条消息不是 tool_calls");
            // 添加工具结果仍然继续，但记录警告
        }
        
        // 添加工具结果消息
        for (tool_call_id, result_json) in &tool_results {
            history.push(ChatMessage::tool_result(tool_call_id, result_json));
        }
        
        // 限制历史长度（防止内存溢出）
        if history.len() > MAX_HISTORY_LENGTH {
            let keep_count = MAX_HISTORY_LENGTH - 1;
            let current_msgs = history.len() - 1;
            let drain_count = current_msgs.saturating_sub(keep_count);
            if drain_count > 0 {
                history.drain(1..=drain_count);
            }
        }
        
        // 克隆历史用于 LLM 调用
        let messages: Vec<ChatMessage> = history.clone();
        drop(history);
        
        // 调用 LLM
        log::info!("[Agent] 调用 LLM 处理工具结果");
        let response = self.llm.chat_with_tools(&messages, self.tools.list_tools()).await?;
        
        // 检查是否有更多工具调用
        if !response.tool_calls.is_empty() {
            // 还有工具调用，检查是否能执行
            if !self.tools.has_callback() {
                // 返回给 Kotlin 执行
                let tool_calls_json = serde_json::to_string(&response.tool_calls)
                    .unwrap_or_else(|_| "[]".to_string());
                
                // 保存 assistant 消息到历史
                {
                    let mut history = self.conversation_history.lock();
                    let content = response.content.as_deref().unwrap_or("");
                    history.push(ChatMessage::assistant_with_tools(
                        content,
                        response.tool_calls.clone(),
                    ));
                }
                
                return Err(Error::Other(format!(
                    "TOOL_CALLS_REQUIRED:{}",
                    tool_calls_json
                )));
            }
            
            // 有回调，继续执行工具循环
            let mut current_messages = messages;
            let mut response = response;
            let mut rounds = 0;
            let mut final_response_text = String::new();
            
            loop {
                if rounds >= MAX_TOOL_ROUNDS {
                    final_response_text = "达到最大工具调用轮数".to_string();
                    break;
                }
                
                // 添加 assistant 消息
                let content = response.content.as_deref().unwrap_or("");
                current_messages.push(ChatMessage::assistant_with_tools(
                    content,
                    response.tool_calls.clone(),
                ));
                
                // 执行工具
                for tool_call in &response.tool_calls {
                    let tool_result = self.tools.execute(&tool_call.function.name, &tool_call.function.arguments);
                    let result_json = match tool_result {
                        Ok(v) => serde_json::to_string(&v).unwrap_or_else(|_| "{}".to_string()),
                        Err(e) => serde_json::json!({"error": e.to_string()}).to_string(),
                    };
                    current_messages.push(ChatMessage::tool_result(&tool_call.id, &result_json));
                }
                
                rounds += 1;
                
                // 调用 LLM
                let next_response = self.llm.chat_with_tools(&current_messages, self.tools.list_tools()).await?;
                
                if next_response.tool_calls.is_empty() {
                    final_response_text = next_response.content.unwrap_or_else(|| "无响应".to_string());
                    break;
                }
                
                response = next_response;
            }
            
            // 更新历史
            {
                let mut history = self.conversation_history.lock();
                history.push(ChatMessage::assistant(&final_response_text));
            }
            
            return Ok(final_response_text);
        }
        
        // 没有更多工具调用，返回文本响应
        let response_text = response.content.unwrap_or_else(|| "无响应".to_string());
        log::info!("[Agent] 最终响应: {} 字节", response_text.len());
        
        // 更新会话历史
        {
            let mut history = self.conversation_history.lock();
            history.push(ChatMessage::assistant(&response_text));
        }
        
        // 存储到长期记忆
        {
            let mut memory = self.memory.lock();
            memory.add_message("assistant", &response_text)?;
        }
        
        Ok(response_text)
    }
    
    /// 获取内核状态
    pub fn status(&self) -> KernelStatus {
        KernelStatus {
            initialized: self.initialized,
            data_dir: self.config.data_dir.to_string_lossy().to_string(),
            memory_entries: self.memory.lock().count(),
            tools_count: self.tools.count(),
            history_length: self.conversation_history.lock().len(),
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
    pub history_length: usize,
}
