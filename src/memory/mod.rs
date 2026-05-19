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
        let mut engine = Self {
            config,
            storage,
            entries: Vec::new(),
        };
        
        // 从数据库加载历史记录
        engine.load_from_db()?;
        
        log::info!("[Memory] 初始化完成，加载 {} 条历史记录", engine.entries.len());
        
        Ok(engine)
    }
    
    /// 从数据库加载历史记录
    fn load_from_db(&mut self) -> Result<()> {
        let conn = self.storage.conn();
        let mut stmt = conn.prepare(
            "SELECT id, role, content, timestamp FROM memory ORDER BY timestamp DESC LIMIT ?"
        )?;
        
        let rows = stmt.query_map([self.config.max_entries as i64], |row| {
            Ok(MemoryEntry {
                id: row.get(0)?,
                role: row.get(1)?,
                content: row.get(2)?,
                timestamp: row.get(3)?,
            })
        })?;
        
        for row in rows {
            self.entries.push(row?);
        }
        
        // 反转顺序，使时间从早到晚
        self.entries.reverse();
        
        Ok(())
    }
    
    /// 添加消息
    pub fn add_message(&mut self, role: &str, content: &str) -> Result<()> {
        let entry = MemoryEntry {
            id: uuid::Uuid::new_v4().to_string(),
            role: role.to_string(),
            content: content.to_string(),
            timestamp: chrono::Utc::now().timestamp(),
        };
        
        // 保存到数据库
        {
            let conn = self.storage.conn();
            conn.execute(
                "INSERT INTO memory (id, role, content, timestamp) VALUES (?1, ?2, ?3, ?4)",
                rusqlite::params![&entry.id, &entry.role, &entry.content, entry.timestamp],
            )?;
        }
        
        // 添加到内存
        self.entries.push(entry);
        
        // 限制条目数量
        if self.entries.len() > self.config.max_entries {
            let removed = self.entries.remove(0);
            // 从数据库删除最旧的记录
            let conn = self.storage.conn();
            conn.execute("DELETE FROM memory WHERE id = ?1", [&removed.id])?;
        }
        
        log::debug!("[Memory] 添加消息: {} (总数: {})", role, self.entries.len());
        
        Ok(())
    }
    
    /// 获取上下文
    pub fn get_context(&self, _query: &str) -> Result<String> {
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
