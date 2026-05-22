// src/mcp/client.rs
// MCP 客户端 - 连接 MCP 服务器并发现工具

use crate::mcp::{McpTransport, McpTool, McpMessage};
use anyhow::{Result, Context};
use tokio::process::{Child, Command};
use std::sync::atomic::{AtomicU64, Ordering};

/// MCP 客户端
pub struct McpClient {
    /// 服务器名称
    server_name: String,
    
    /// 传输层
    transport: McpTransport,
    
    /// 已发现的工具
    discovered_tools: Vec<McpTool>,
    
    /// 请求 ID 计数器
    request_id: AtomicU64,
    
    /// Stdio 进程（如果是 Stdio 传输）
    #[allow(dead_code)]
    process: Option<Child>,
}

impl McpClient {
    /// 创建新的 MCP 客户端
    pub fn new(server_name: String, transport: McpTransport) -> Self {
        Self {
            server_name,
            transport,
            discovered_tools: Vec::new(),
            request_id: AtomicU64::new(1),
            process: None,
        }
    }
    
    /// 连接服务器
    pub async fn connect(&mut self) -> Result<()> {
        log::info!("[McpClient] 连接 MCP 服务器: {}", self.server_name);
        
        match &self.transport {
            McpTransport::Stdio(stdio) => {
                // 启动子进程
                let mut cmd = Command::new(&stdio.command);
                cmd.args(&stdio.args);
                
                for (key, value) in &stdio.env {
                    cmd.env(key, value);
                }
                
                cmd.stdin(std::process::Stdio::piped())
                    .stdout(std::process::Stdio::piped())
                    .stderr(std::process::Stdio::null());
                
                let child = cmd.spawn()
                    .with_context(|| format!("启动 MCP 服务器失败: {}", stdio.command))?;
                
                self.process = Some(child);
                log::info!("[McpClient] Stdio 进程已启动");
            }
            
            McpTransport::Http(http) => {
                // HTTP 连接不需要预启动
                log::info!("[McpClient] HTTP 端点: {}", http.url);
            }
        }
        
        // 发送初始化请求
        self.send_initialize().await?;
        
        Ok(())
    }
    
    /// 发送初始化请求
    async fn send_initialize(&mut self) -> Result<()> {
        let id = self.next_id();
        let request = McpMessage::Request {
            id,
            method: "initialize".to_string(),
            params: serde_json::json!({
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {
                    "name": "chenyi-agent",
                    "version": "0.1.0"
                }
            }),
        };
        
        let _response = self.send_message(&request).await?;
        log::info!("[McpClient] 初始化完成");
        Ok(())
    }
    
    /// 发现工具
    pub async fn discover_tools(&mut self) -> Result<Vec<McpTool>> {
        log::info!("[McpClient] 发现工具: {}", self.server_name);
        
        let id = self.next_id();
        let request = McpMessage::Request {
            id,
            method: "tools/list".to_string(),
            params: serde_json::json!({}),
        };
        
        let response = self.send_message(&request).await?;
        
        // 解析工具列表
        let tools = match response {
            McpMessage::Response { result: Some(result), .. } => {
                // 尝试解析为 ToolsListResponse
                if let Ok(tools_response) = serde_json::from_value::<ToolsListResponse>(result.clone()) {
                    tools_response.tools
                } else {
                    // 尝试直接解析为 Vec<McpTool>
                    if let Ok(tools) = serde_json::from_value::<Vec<McpTool>>(result) {
                        tools
                    } else {
                        log::warn!("[McpClient] 无法解析工具列表，返回空列表");
                        Vec::new()
                    }
                }
            }
            McpMessage::Response { error: Some(err), .. } => {
                return Err(anyhow::anyhow!("MCP 错误: {} - {}", err.code, err.message));
            }
            _ => {
                return Err(anyhow::anyhow!("无效响应"));
            }
        };
        
        log::info!("[McpClient] 发现 {} 个工具", tools.len());
        self.discovered_tools = tools.clone();
        Ok(tools)
    }
    
    /// 调用工具
    pub async fn call_tool(&self, name: &str, args: serde_json::Value) -> Result<serde_json::Value> {
        log::info!("[McpClient] 调用工具: {} - {}", self.server_name, name);
        
        let id = self.next_id();
        let request = McpMessage::Request {
            id,
            method: "tools/call".to_string(),
            params: serde_json::json!({
                "name": name,
                "arguments": args
            }),
        };
        
        let response = self.send_message_readonly(&request).await?;
        
        match response {
            McpMessage::Response { result: Some(result), .. } => {
                Ok(result)
            }
            McpMessage::Response { error: Some(err), .. } => {
                Err(anyhow::anyhow!("MCP 错误: {} - {}", err.code, err.message))
            }
            _ => {
                Err(anyhow::anyhow!("无效响应"))
            }
        }
    }
    
    /// 发送消息（可变引用版本）
    async fn send_message(&mut self, message: &McpMessage) -> Result<McpMessage> {
        let url = match &self.transport {
            McpTransport::Stdio(_) => {
                // TODO: 实现真实的 Stdio 通信
                // 目前返回模拟响应
                return self.mock_response(message);
            }
            McpTransport::Http(http) => http.url.clone(),
        };
        
        self.send_http_message(&url, message).await
    }
    
    /// 发送消息（只读版本，用于 call_tool）
    async fn send_message_readonly(&self, message: &McpMessage) -> Result<McpMessage> {
        let url = match &self.transport {
            McpTransport::Stdio(_) => {
                // TODO: 实现真实的 Stdio 通信
                return self.mock_response(message);
            }
            McpTransport::Http(http) => http.url.clone(),
        };
        
        self.send_http_message(&url, message).await
    }
    
    /// HTTP 发送消息
    async fn send_http_message(&self, url: &str, message: &McpMessage) -> Result<McpMessage> {
        let client = reqwest::Client::new();
        
        let response = client
            .post(url)
            .json(message)
            .timeout(std::time::Duration::from_secs(120))
            .send()
            .await
            .with_context(|| format!("HTTP 请求失败: {}", url))?;
        
        let message = response.json::<McpMessage>().await
            .with_context(|| "解析响应失败")?;
        
        Ok(message)
    }
    
    /// 模拟响应（用于测试）
    fn mock_response(&self, message: &McpMessage) -> Result<McpMessage> {
        match message {
            McpMessage::Request { id, method, .. } => {
                match method.as_str() {
                    "tools/list" => {
                        Ok(McpMessage::Response {
                            id: *id,
                            result: Some(serde_json::json!({
                                "tools": []
                            })),
                            error: None,
                        })
                    }
                    "tools/call" => {
                        Ok(McpMessage::Response {
                            id: *id,
                            result: Some(serde_json::json!({
                                "content": "工具执行成功"
                            })),
                            error: None,
                        })
                    }
                    _ => {
                        Ok(McpMessage::Response {
                            id: *id,
                            result: Some(serde_json::json!({})),
                            error: None,
                        })
                    }
                }
            }
            _ => Err(anyhow::anyhow!("无效消息类型")),
        }
    }
    
    /// 获取下一个请求 ID
    fn next_id(&self) -> u64 {
        self.request_id.fetch_add(1, Ordering::SeqCst)
    }
}

/// 工具列表响应
#[derive(Debug, serde::Deserialize)]
struct ToolsListResponse {
    #[serde(default)]
    tools: Vec<McpTool>,
}
