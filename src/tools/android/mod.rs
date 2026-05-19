//! 安卓原生工具模块

pub mod screen;
pub mod input;
pub mod app;
pub mod ocr;

use serde::{Deserialize, Serialize};

/// 工具执行结果
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ToolResult {
    pub success: bool,
    pub data: Option<serde_json::Value>,
    pub error: Option<String>,
}

impl ToolResult {
    pub fn ok(data: serde_json::Value) -> Self {
        Self {
            success: true,
            data: Some(data),
            error: None,
        }
    }

    pub fn err(msg: impl Into<String>) -> Self {
        Self {
            success: false,
            data: None,
            error: Some(msg.into()),
        }
    }
}

/// JNI 回调类型
pub type JniCallback = unsafe extern "C" fn(tool: *const i8, params: *const i8) -> *const i8;

/// 安卓工具集合
pub struct AndroidTools {
    jni_callback: Option<JniCallback>,
}

impl AndroidTools {
    pub fn new() -> Self {
        Self { jni_callback: None }
    }

    /// 设置 JNI 回调
    pub fn set_callback(&mut self, callback: JniCallback) {
        self.jni_callback = Some(callback);
    }

    /// 调用原生方法
    pub fn call_native(&self, tool: &str, params: &str) -> ToolResult {
        if let Some(callback) = self.jni_callback {
            let tool_c = std::ffi::CString::new(tool).unwrap();
            let params_c = std::ffi::CString::new(params).unwrap();

            let result_ptr = unsafe { callback(tool_c.as_ptr(), params_c.as_ptr()) };

            let result_c = unsafe { std::ffi::CStr::from_ptr(result_ptr) };
            let result_str = result_c.to_string_lossy().into_owned();

            serde_json::from_str(&result_str).unwrap_or_else(|_| ToolResult::err("解析结果失败"))
        } else {
            ToolResult::err("JNI 回调未注册")
        }
    }
}

impl Default for AndroidTools {
    fn default() -> Self {
        Self::new()
    }
}