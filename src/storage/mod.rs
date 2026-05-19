//! 存储引擎

use crate::types::Result;
use rusqlite::Connection;
use std::path::{Path, PathBuf};

/// 存储错误
#[derive(Debug, thiserror::Error)]
pub enum StorageError {
    #[error("SQLite 错误: {0}")]
    Sqlite(#[from] rusqlite::Error),
    
    #[error("路径错误: {0}")]
    Path(String),
}

/// 存储引擎
pub struct StorageEngine {
    conn: parking_lot::Mutex<Connection>,
    data_dir: PathBuf,
}

impl StorageEngine {
    pub fn new(data_dir: &Path) -> Result<Self> {
        let db_path = data_dir.join("chenyi.db");
        
        let conn = Connection::open(&db_path)?;
        
        // 创建表
        conn.execute_batch(
            r#"
            CREATE TABLE IF NOT EXISTS memory (
                id TEXT PRIMARY KEY,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS sessions (
                id TEXT PRIMARY KEY,
                name TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS config (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            "#,
        )?;
        
        Ok(Self {
            conn: parking_lot::Mutex::new(conn),
            data_dir: data_dir.to_path_buf(),
        })
    }
    
    /// 获取数据目录
    pub fn data_dir(&self) -> &Path {
        &self.data_dir
    }
    
    /// 获取连接
    pub fn conn(&self) -> parking_lot::MutexGuard<Connection> {
        self.conn.lock()
    }
}
