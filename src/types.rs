//! 公共类型定义

use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// 内核结果类型
pub type Result<T> = std::result::Result<T, Error>;

/// 内核错误
#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("内核已初始化")]
    AlreadyInitialized,

    #[error("内核未初始化")]
    NotInitialized,

    #[error("存储错误: {0}")]
    Storage(#[from] crate::storage::StorageError),

    #[error("LLM 错误: {0}")]
    Llm(#[from] crate::llm::LlmError),

    #[error("SQLite 错误: {0}")]
    Sqlite(#[from] rusqlite::Error),

    #[error("工具错误: {0}")]
    Tool(String),

    #[error("JSON 解析错误: {0}")]
    Json(#[from] serde_json::Error),

    #[error("IO 错误: {0}")]
    Io(#[from] std::io::Error),

    #[error("{0}")]
    Other(String),
}

/// 内核配置
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct KernelConfig {
    /// 数据目录
    pub data_dir: PathBuf,
    
    /// LLM 配置
    pub llm: LlmConfig,
    
    /// Memory 配置
    pub memory: MemoryConfig,
}

impl Default for KernelConfig {
    fn default() -> Self {
        Self {
            data_dir: PathBuf::from("."),
            llm: LlmConfig::default(),
            memory: MemoryConfig::default(),
        }
    }
}

/// LLM 配置
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct LlmConfig {
    /// API 端点
    pub endpoint: String,
    
    /// API Key
    pub api_key: String,
    
    /// 模型名称
    pub model: String,
    
    /// 最大 Token
    pub max_tokens: u32,
    
    /// 温度
    pub temperature: f32,
}

impl Default for LlmConfig {
    fn default() -> Self {
        Self {
            endpoint: "https://api.openai.com/v1".to_string(),
            api_key: String::new(),
            model: "gpt-4o-mini".to_string(),
            max_tokens: 4096,
            temperature: 0.7,
        }
    }
}

/// Memory 配置
#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct MemoryConfig {
    /// 最大记忆条数
    pub max_entries: usize,
    
    /// 嵌入维度
    pub embedding_dim: usize,
}

impl Default for MemoryConfig {
    fn default() -> Self {
        Self {
            max_entries: 10000,
            embedding_dim: 1536,
        }
    }
}
