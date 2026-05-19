//! 工具注册表

use crate::types::{Result, Error};
use std::collections::HashMap;

/// 工具注册表
pub struct ToolRegistry {
    tools: HashMap<String, Tool>,
}

/// 工具定义
#[derive(Debug, Clone)]
pub struct Tool {
    pub name: String,
    pub description: String,
    pub handler: ToolHandler,
}

/// 工具处理器
pub type ToolHandler = fn(&str) -> Result<serde_json::Value>;

impl ToolRegistry {
    pub fn new() -> Self {
        Self {
            tools: HashMap::new(),
        }
    }
    
    /// 注册工具
    pub fn register(&mut self, name: &str, description: &str, handler: ToolHandler) {
        self.tools.insert(name.to_string(), Tool {
            name: name.to_string(),
            description: description.to_string(),
            handler,
        });
    }
    
    /// 执行工具
    pub fn execute(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        let tool = self.tools.get(name)
            .ok_or_else(|| Error::Tool(format!("未知工具: {}", name)))?;
        
        (tool.handler)(params)
    }
    
    /// 列出所有工具
    pub fn list(&self) -> Vec<&Tool> {
        self.tools.values().collect()
    }
}

impl Default for ToolRegistry {
    fn default() -> Self {
        Self::new()
    }
}