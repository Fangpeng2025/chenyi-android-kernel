// src/mcp/mod.rs
// MCP 协议模块 - 完全复刻 Hermes 的 MCP 架构

pub mod client;
pub mod protocol;
pub mod transport;
pub mod discovery;

pub use client::McpClient;
pub use protocol::{McpMessage, McpTool};
pub use transport::{McpTransport, StdioTransport, HttpTransport};
pub use discovery::ToolDiscovery;
