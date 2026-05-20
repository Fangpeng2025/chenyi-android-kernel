//! LLM 客户端

use crate::types::{LlmConfig, Result, Error};
use serde::{Deserialize, Serialize};
use std::time::Duration;

/// 聊天消息
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatMessage {
    pub role: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_calls: Option<Vec<ToolCall>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_call_id: Option<String>,
}

impl ChatMessage {
    pub fn system(content: &str) -> Self {
        Self {
            role: "system".to_string(),
            content: Some(content.to_string()),
            tool_calls: None,
            tool_call_id: None,
        }
    }
    
    pub fn user(content: &str) -> Self {
        Self {
            role: "user".to_string(),
            content: Some(content.to_string()),
            tool_calls: None,
            tool_call_id: None,
        }
    }
    
    pub fn assistant(content: &str) -> Self {
        Self {
            role: "assistant".to_string(),
            content: Some(content.to_string()),
            tool_calls: None,
            tool_call_id: None,
        }
    }
    
    pub fn assistant_with_tools(content: &str, tool_calls: Vec<ToolCall>) -> Self {
        Self {
            role: "assistant".to_string(),
            content: if content.is_empty() { None } else { Some(content.to_string()) },
            tool_calls: Some(tool_calls),
            tool_call_id: None,
        }
    }
    
    pub fn tool_result(id: &str, result: &str) -> Self {
        Self {
            role: "tool".to_string(),
            content: Some(result.to_string()),
            tool_calls: None,
            tool_call_id: Some(id.to_string()),
        }
    }
}

/// 工具定义
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolDefinition {
    #[serde(rename = "type")]
    pub tool_type: String,
    pub function: ToolFunction,
}

impl ToolDefinition {
    pub fn new(name: &str, description: &str, parameters: serde_json::Value) -> Self {
        Self {
            tool_type: "function".to_string(),
            function: ToolFunction {
                name: name.to_string(),
                description: description.to_string(),
                parameters,
            },
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolFunction {
    pub name: String,
    pub description: String,
    pub parameters: serde_json::Value,
}

/// 工具调用
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolCall {
    pub id: String,
    #[serde(rename = "type")]
    pub call_type: String,
    pub function: ToolCallFunction,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolCallFunction {
    pub name: String,
    pub arguments: String,
}

/// 响应消息
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResponseMessage {
    pub role: String,
    #[serde(default)]
    pub content: Option<String>,
    #[serde(default)]
    pub tool_calls: Vec<ToolCall>,
}

/// 聊天响应
#[derive(Debug, Deserialize)]
struct ChatResponse {
    choices: Vec<Choice>,
}

#[derive(Debug, Deserialize)]
struct Choice {
    message: ResponseMessage,
}

/// LLM 客户端
pub struct LlmClient {
    pub config: LlmConfig,
    client: reqwest::Client,
}

impl LlmClient {
    pub fn new(config: LlmConfig) -> Result<Self> {
        let client = reqwest::Client::builder()
            .timeout(Duration::from_secs(120))  // 增加超时时间
            .connect_timeout(Duration::from_secs(10))
            .build()
            .map_err(|e| Error::Other(format!("创建 HTTP 客户端失败: {}", e)))?;
        
        Ok(Self { config, client })
    }
    
    /// 发送聊天请求（带工具支持和自动重试）
    pub async fn chat_with_tools(&self, messages: &[ChatMessage], tools: Vec<ToolDefinition>) -> Result<ResponseMessage> {
        const MAX_RETRIES: usize = 3;
        const RETRY_DELAY_MS: u64 = 1000;
        
        for attempt in 1..=MAX_RETRIES {
            let result = self.chat_with_tools_internal(messages, tools.clone()).await;
            
            match result {
                Ok(response) => return Ok(response),
                Err(e) => {
                    // 检查是否是可重试的错误
                    let error_msg = e.to_string();
                    let should_retry = error_msg.contains("timeout") 
                        || error_msg.contains("connection")
                        || error_msg.contains("network")
                        || error_msg.contains("503")
                        || error_msg.contains("502");
                    
                    if should_retry && attempt < MAX_RETRIES {
                        log::warn!("[LLM] 第 {} 次请求失败: {}，{}ms 后重试", 
                            attempt, error_msg, RETRY_DELAY_MS);
                        tokio::time::sleep(Duration::from_millis(RETRY_DELAY_MS)).await;
                        continue;
                    }
                    
                    return Err(e);
                }
            }
        }
        
        Err(Error::Other("达到最大重试次数".to_string()))
    }
    
    /// 内部聊天请求实现
    async fn chat_with_tools_internal(&self, messages: &[ChatMessage], tools: Vec<ToolDefinition>) -> Result<ResponseMessage> {
        #[derive(Serialize)]
        struct Request {
            model: String,
            messages: Vec<ChatMessage>,
            #[serde(skip_serializing_if = "Vec::is_empty")]
            tools: Vec<ToolDefinition>,
            max_tokens: u32,
            temperature: f32,
        }
        
        let request = Request {
            model: self.config.model.clone(),
            messages: messages.to_vec(),
            tools,
            max_tokens: self.config.max_tokens,
            temperature: self.config.temperature,
        };
        
        let url = format!("{}/chat/completions", self.config.endpoint);
        
        log::info!("[LLM] 请求 URL: {}", url);
        log::debug!("[LLM] 请求 Body: {}", serde_json::to_string_pretty(&request).unwrap_or_default());
        
        let response = self.client
            .post(&url)
            .header("Authorization", format!("Bearer {}", self.config.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .map_err(|e| Error::Other(format!("HTTP 请求失败: {}", e)))?;
        
        let status = response.status();
        log::info!("[LLM] 响应状态: {}", status);
        
        let body = response.text().await
            .map_err(|e| Error::Other(format!("读取响应失败: {}", e)))?;
        
        log::debug!("[LLM] 响应 Body: {}", body.chars().take(500).collect::<String>());
        
        if !status.is_success() {
            log::error!("[LLM] API 错误: {} - {}", status, body);
            return Err(Error::Other(format!("API 错误 ({}): {}", status, body)));
        }
        
        // 尝试解析响应
        let result: ChatResponse = match serde_json::from_str(&body) {
            Ok(r) => r,
            Err(e) => {
                log::error!("[LLM] 解析响应失败: {} - 响应内容: {}", e, body.chars().take(500).collect::<String>());
                return Err(Error::Other(format!("解析响应失败: {} - 响应内容: {}", e, body.chars().take(200).collect::<String>())));
            }
        };
        
        let message = result.choices.first()
            .map(|c| c.message.clone())
            .unwrap_or_else(|| {
                log::warn!("[LLM] 响应中没有 choices，返回空响应");
                ResponseMessage {
                    role: "assistant".to_string(),
                    content: Some("无响应".to_string()),
                    tool_calls: vec![],
                }
            });
        
        log::info!("[LLM] 响应成功: content={}, tool_calls={}", 
            message.content.as_ref().map(|c| c.len()).unwrap_or(0),
            message.tool_calls.len()
        );
        
        Ok(message)
    }
    
    /// 简单聊天（无工具）
    pub async fn chat(&self, messages: &[ChatMessage]) -> Result<String> {
        let response = self.chat_with_tools(messages, vec![]).await?;
        response.content.ok_or_else(|| Error::Other("无响应内容".to_string()))
    }
}
