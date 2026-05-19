//! Memory 引擎

use crate::types::{MemoryConfig, Result, Error};
use crate::storage::StorageEngine;
use std::sync::Arc;

/// 记忆引擎
pub struct MemoryEngine {
    config: MemoryConfig,
    storage: Arc<StorageEngine>,
    entries: Vec<MemoryEntry>,
}

/// 记忆条目
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct MemoryEntry {
    pub id: String,
    pub role: String,
    pub content: String,
    pub timestamp: i64,
}

impl MemoryEngine {
    pub fn new(config: MemoryConfig, storage: Arc<StorageEngine>) -> Result<Self> {
        Ok(Self {
            config,
            storage,
            entries: Vec::new(),
        })
    }
    
    /// 添加消息
    pub fn add_message(&mut self, role: &str, content: &str) -> Result<()> {
        let entry = MemoryEntry {
            id: uuid::Uuid::new_v4().to_string(),
            role: role.to_string(),
            content: content.to_string(),
            timestamp: chrono::Utc::now().timestamp(),
        };
        
        self.entries.push(entry);
        
        // 限制条目数量
        if self.entries.len() > self.config.max_entries {
            self.entries.remove(0);
        }
        
        Ok(())
    }
    
    /// 获取上下文
    pub fn get_context(&self, query: &str) -> Result<String> {
        // 简单实现：返回最近的对话
        let recent: Vec<_> = self.entries.iter().rev().take(10).collect();
        let context = recent.iter().rev()
            .map(|e| format!("{}: {}", e.role, e.content))
            .collect::<Vec<_>>()
            .join("\n");
        
        Ok(context)
    }
    
    /// 获取条目数量
    pub fn count(&self) -> usize {
        self.entries.len()
    }
}
