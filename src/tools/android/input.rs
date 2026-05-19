//! 输入控制工具

use super::{AndroidTools, ToolResult};
use serde::Serialize;

#[derive(Debug, Serialize)]
struct TapParams {
    x: i32,
    y: i32,
}

#[derive(Debug, Serialize)]
struct SwipeParams {
    start_x: i32,
    start_y: i32,
    end_x: i32,
    end_y: i32,
    duration: u32,
}

#[derive(Debug, Serialize)]
struct TypeParams {
    text: String,
}

impl AndroidTools {
    /// 点击坐标
    pub fn tap(&self, x: i32, y: i32) -> ToolResult {
        let params = TapParams { x, y };
        self.call_native("tap", &serde_json::to_string(&params).unwrap())
    }

    /// 长按
    pub fn long_press(&self, x: i32, y: i32, duration_ms: u32) -> ToolResult {
        let params = SwipeParams {
            start_x: x,
            start_y: y,
            end_x: x,
            end_y: y,
            duration: duration_ms,
        };
        self.call_native("long_press", &serde_json::to_string(&params).unwrap())
    }

    /// 滑动
    pub fn swipe(&self, sx: i32, sy: i32, ex: i32, ey: i32, duration_ms: u32) -> ToolResult {
        let params = SwipeParams {
            start_x: sx,
            start_y: sy,
            end_x: ex,
            end_y: ey,
            duration: duration_ms,
        };
        self.call_native("swipe", &serde_json::to_string(&params).unwrap())
    }

    /// 输入文本
    pub fn type_text(&self, text: &str) -> ToolResult {
        let params = TypeParams {
            text: text.to_string(),
        };
        self.call_native("type_text", &serde_json::to_string(&params).unwrap())
    }

    /// 按键
    pub fn press_key(&self, keycode: i32) -> ToolResult {
        self.call_native("press_key", &serde_json::json!({ "keycode": keycode }).to_string())
    }
}