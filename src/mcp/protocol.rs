// src/mcp/protocol.rs
// MCP 协议定义

use serde::{Deserialize, Serialize};

/// MCP 工具定义
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpTool {
    /// 工具名称
    pub name: String,
    
    /// 工具描述
    pub description: String,
    
    /// 输入参数Schema
    pub input_schema: serde_json::Value,
}

/// MCP 消息
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum McpMessage {
    /// 请求
    Request {
        id: u64,
        method: String,
        params: serde_json::Value,
    },
    
    /// 响应
    Response {
        id: u64,
        result: Option<serde_json::Value>,
        error: Option<McpError>,
    },
    
    /// 通知
    Notification {
        method: String,
        params: serde_json::Value,
    },
}

/// MCP 错误
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpError {
    pub code: i32,
    pub message: String,
    pub data: Option<serde_json::Value>,
}