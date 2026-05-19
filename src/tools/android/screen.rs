//! 截图工具

use super::{AndroidTools, ToolResult};
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize)]
struct ScreenshotParams {
    #[serde(skip_serializing_if = "Option::is_none")]
    region: Option<Region>,
}

#[derive(Debug, Serialize)]
struct Region {
    x: i32,
    y: i32,
    width: i32,
    height: i32,
}

#[derive(Debug, Deserialize)]
struct ScreenshotData {
    path: String,
    width: i32,
    height: i32,
}

impl AndroidTools {
    /// 截取全屏
    pub fn screenshot(&self) -> ToolResult {
        self.call_native("screenshot", "{}")
    }

    /// 截取指定区域
    pub fn screenshot_region(&self, x: i32, y: i32, w: i32, h: i32) -> ToolResult {
        let params = ScreenshotParams {
            region: Some(Region {
                x,
                y,
                width: w,
                height: h,
            }),
        };
        self.call_native("screenshot", &serde_json::to_string(&params).unwrap())
    }
}