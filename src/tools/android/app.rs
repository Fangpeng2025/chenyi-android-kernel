//! 应用管理工具

use super::{AndroidTools, ToolResult};

impl AndroidTools {
    /// 打开应用
    pub fn open_app(&self, package: &str) -> ToolResult {
        self.call_native(
            "open_app",
            &serde_json::json!({ "package": package }).to_string(),
        )
    }

    /// 关闭当前应用
    pub fn close_app(&self) -> ToolResult {
        self.call_native("close_app", "{}")
    }

    /// 获取当前应用
    pub fn current_app(&self) -> ToolResult {
        self.call_native("current_app", "{}")
    }

    /// 列出所有应用
    pub fn list_apps(&self) -> ToolResult {
        self.call_native("list_apps", "{}")
    }
}