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

/// LLM 客户端
pub struct LlmClient {
    config: LlmConfig,
    client: reqwest::Client,
}

impl LlmClient {
    pub fn new(config: LlmConfig) -> Self {
        Self {
            config,
            client: reqwest::Client::new(),
        }
    }
    
    /// 发送聊天请求
    pub async fn chat(&self, message: &str, context: &str) -> Result<String> {
        let request = ChatRequest {
            model: self.config.model.clone(),
            messages: vec![
                Message { role: "system".to_string(), content: "你是晨翼Agent，一个智能助手。".to_string() },
                Message { role: "user".to_string(), content: format!("{}\n\n{}", context, message) },
            ],
            max_tokens: self.config.max_tokens,
            temperature: self.config.temperature,
        };
        
        let response = self.client
            .post(format!("{}/chat/completions", self.config.endpoint))
            .header("Authorization", format!("Bearer {}", self.config.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .map_err(|e| crate::types::Error::Other(e.to_string()))?;
        
        let result: ChatResponse = response
            .json()
            .await
            .map_err(|e| crate::types::Error::Other(e.to_string()))?;
        
        Ok(result.choices.first()
            .map(|c| c.message.content.clone())
            .unwrap_or_else(|| "无响应".to_string()))
    }
}

/// 聊天请求
#[derive(Debug, Serialize)]
struct ChatRequest {
    model: String,
    messages: Vec<Message>,
    max_tokens: u32,
    temperature: f32,
}

/// 消息
#[derive(Debug, Serialize, Deserialize)]
struct Message {
    role: String,
    content: String,
}

/// 聊天响应
#[derive(Debug, Deserialize)]
struct ChatResponse {
    choices: Vec<Choice>,
}

#[derive(Debug, Deserialize)]
struct Choice {
    message: Message,
}