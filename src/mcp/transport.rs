// src/mcp/transport.rs
// MCP 传输层

use anyhow::Result;
use std::collections::HashMap;

/// MCP 传输层
#[derive(Debug, Clone)]
pub enum McpTransport {
    /// Stdio 传输
    Stdio(StdioTransport),
    
    /// HTTP 传输
    Http(HttpTransport),
}

/// Stdio 传输配置
#[derive(Debug, Clone)]
pub struct StdioTransport {
    pub command: String,
    pub args: Vec<String>,
    pub env: HashMap<String, String>,
    pub timeout: u64,
}

/// HTTP 传输配置
#[derive(Debug, Clone)]
pub struct HttpTransport {
    pub url: String,
    pub headers: HashMap<String, String>,
    pub timeout: u64,
}

impl McpTransport {
    /// 创建 Stdio 传输
    pub fn stdio(command: String, args: Vec<String>) -> Self {
        Self::Stdio(StdioTransport {
            command,
            args,
            env: HashMap::new(),
            timeout: 120,
        })
    }
    
    /// 创建 HTTP 传输
    pub fn http(url: String) -> Self {
        Self::Http(HttpTransport {
            url,
            headers: HashMap::new(),
            timeout: 120,
        })
    }
}