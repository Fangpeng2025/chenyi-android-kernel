//! LLM 客户端

use crate::types::{LlmConfig, Result};
use serde::{Deserialize, Serialize};

/// LLM 错误
#[derive(Debug, thiserror::Error)]
pub enum LlmError {
    #[error("HTTP 错误: {0}")]
    Http(String),
    
    #[error("API 错误: {0}")]
    Api(String),
    
    #[error("解析错误: {0}")]
    Parse(String),
}

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
    
    pub fn assistant_with_tool_use(content: &str, tool_call_id: &str, tool_name: &str, tool_args: &str) -> Self {
        Self {
            role: "assistant".to_string(),
            content: if content.is_empty() { None } else { Some(content.to_string()) },
            tool_calls: Some(vec![ToolCall {
                id: tool_call_id.to_string(),
                r#type: "function".to_string(),
                function: FunctionCall {
                    name: tool_name.to_string(),
                    arguments: tool_args.to_string(),
                },
            }]),
            tool_call_id: None,
        }
    }
    
    pub fn tool_result(tool_call_id: &str, content: &str) -> Self {
        Self {
            role: "tool".to_string(),
            content: Some(content.to_string()),
            tool_calls: None,
            tool_call_id: Some(tool_call_id.to_string()),
        }
    }
}

/// 工具调用
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolCall {
    pub id: String,
    pub r#type: String,
    pub function: FunctionCall,
}

/// 函数调用
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FunctionCall {
    pub name: String,
    pub arguments: String,
}

/// 工具定义
#[derive(Debug, Clone, Serialize)]
pub struct ToolDefinition {
    pub r#type: String,
    pub function: ToolFunction,
}

#[derive(Debug, Clone, Serialize)]
pub struct ToolFunction {
    pub name: String,
    pub description: String,
    pub parameters: serde_json::Value,
}

/// LLM 响应
#[derive(Debug, Clone, Deserialize)]
pub struct ChatResponse {
    pub choices: Vec<Choice>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Choice {
    pub message: ResponseMessage,
    pub finish_reason: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ResponseMessage {
    pub role: String,
    pub content: Option<String>,
    #[serde(default)]
    pub tool_calls: Vec<ToolCall>,
}

/// LLM 客户端
pub struct LlmClient {
    config: LlmConfig,
    client: reqwest::Client,
}

impl LlmClient {
    pub fn new(config: LlmConfig) -> Self {
        Self {
            config,
            client: reqwest::Client::builder()
                .timeout(std::time::Duration::from_secs(60))
                .build()
                .unwrap(),
        }
    }
    
    /// 发送聊天请求（带工具支持）
    pub async fn chat_with_tools(&self, messages: &[ChatMessage], tools: Vec<ToolDefinition>) -> Result<ResponseMessage> {
        #[derive(Serialize)]
        struct Request {
            model: String,
            messages: Vec<ChatMessage>,
            #[serde(skip_serializing_if = "Vec::is_empty")]
            tools: Vec<ToolDefinition>,
        }
        
        let request = Request {
            model: self.config.model.clone(),
            messages: messages.to_vec(),
            tools,
        };
        
        let url = format!("{}/chat/completions", self.config.endpoint);
        
        eprintln!("[LLM] 请求: {}", url);
        
        let response = self.client
            .post(&url)
            .header("Authorization", format!("Bearer {}", self.config.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .map_err(|e| crate::types::Error::Other(format!("HTTP 错误: {}", e)))?;
        
        let status = response.status();
        let body = response.text().await
            .map_err(|e| crate::types::Error::Other(format!("读取响应失败: {}", e)))?;
        
        if !status.is_success() {
            eprintln!("[LLM] 错误响应: {}", body);
            return Err(crate::types::Error::Other(format!("API 错误 ({}): {}", status, body)));
        }
        
        let result: ChatResponse = serde_json::from_str(&body)
            .map_err(|e| crate::types::Error::Other(format!("解析响应失败: {} - {}", e, body)))?;
        
        Ok(result.choices.first()
            .map(|c| c.message.clone())
            .unwrap_or_else(|| ResponseMessage {
                role: "assistant".to_string(),
                content: Some("无响应".to_string()),
                tool_calls: vec![],
            }))
    }
    
    /// 简单聊天（无工具）
    pub async fn chat(&self, message: &str, context: &str) -> Result<String> {
        let messages = vec![
            ChatMessage::system("你是晨翼Agent，一个智能助手。"),
            ChatMessage::user(&format!("{}\n\n{}", context, message)),
        ];
        
        let response = self.chat_with_tools(&messages, vec![]).await?;
        Ok(response.content.unwrap_or_else(|| "无响应".to_string()))
    }
}