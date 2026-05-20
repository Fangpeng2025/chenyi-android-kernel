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
        
        // 限制条目数量（使用事务确保一致性）
        if self.entries.len() > self.config.max_entries {
            let remove_count = self.entries.len() - self.config.max_entries;
            let removed_ids: Vec<String> = self.entries.iter()
                .take(remove_count)
                .map(|e| e.id.clone())
                .collect();
            
            // 从内存移除
            self.entries.drain(0..remove_count);
            
            // 从数据库批量删除
            let conn = self.storage.conn();
            for id in removed_ids {
                if let Err(e) = conn.execute("DELETE FROM memory WHERE id = ?1", [&id]) {
                    log::warn!("[Memory] 删除旧记录失败 {}: {}", id, e);
                }
            }
            
            log::debug!("[Memory] 清理了 {} 条旧记录", remove_count);
        }
        
        log::debug!("[Memory] 添加消息: {} (总数: {})", role, self.entries.len());
        
        Ok(())
    }
    
    /// 获取上下文（返回最近 N 条对话，按时间排序）
    /// 
    /// # Arguments
    /// * `query` - 查询字符串（当前未使用，保留用于未来语义搜索）
    /// 
    /// # Note
    /// 当前实现返回最近的对话记录。未来可以实现基于 embedding 的语义搜索。
    #[allow(unused_variables)]
    pub fn get_context(&self, query: &str) -> Result<String> {
        // 简单实现：返回最近的对话记录
        // TODO: 未来可以实现基于 embedding 的语义搜索
        
        let recent_count = 10.min(self.entries.len());
        if recent_count == 0 {
            return Ok(String::new());
        }
        
        // 获取最近的记录（entries 已按时间排序）
        let start = self.entries.len().saturating_sub(recent_count);
        let recent: Vec<_> = self.entries.iter().skip(start).collect();
        
        let context = recent.iter()
            .map(|e| format!("{}: {}", e.role, e.content))
            .collect::<Vec<_>>()
            .join("\n");
        
        Ok(context)
    }
    
    /// 获取条目数量
    pub fn count(&self) -> usize {
        self.entries.len()
    }
    
    /// 清除所有记忆（用于测试）
    #[allow(dead_code)]
    pub fn clear(&mut self) -> Result<()> {
        let conn = self.storage.conn();
        conn.execute("DELETE FROM memory", [])?;
        self.entries.clear();
        log::info!("[Memory] 已清除所有记忆");
        Ok(())
    }
}
