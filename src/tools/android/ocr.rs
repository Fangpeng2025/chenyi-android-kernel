//! OCR 工具

use super::{AndroidTools, ToolResult};
use serde::Deserialize;

/// OCR 识别结果
#[derive(Debug, Clone, Deserialize)]
pub struct OcrWord {
    pub text: String,
    pub confidence: f32,
    pub x: i32,
    pub y: i32,
    pub width: i32,
    pub height: i32,
}

/// OCR 完整结果
#[derive(Debug, Clone, Deserialize)]
pub struct OcrResult {
    pub words: Vec<OcrWord>,
    pub full_text: String,
}

impl AndroidTools {
    /// OCR 识别当前屏幕
    pub fn ocr(&self) -> ToolResult {
        self.call_native("ocr", "{}")
    }

    /// OCR 识别指定图片
    pub fn ocr_image(&self, image_path: &str) -> ToolResult {
        self.call_native(
            "ocr",
            &serde_json::json!({ "image": image_path }).to_string(),
        )
    }

    /// 查找文本位置
    pub fn find_text(&self, text: &str) -> ToolResult {
        self.call_native(
            "find_text",
            &serde_json::json!({ "text": text }).to_string(),
        )
    }

    /// 点击文本（OCR 定位后点击）
    pub fn tap_text(&self, text: &str) -> ToolResult {
        // 1. 先 OCR
        let ocr_result = self.ocr();
        if !ocr_result.success {
            return ocr_result;
        }

        // 2. 解析结果，查找文本
        if let Some(data) = &ocr_result.data {
            if let Some(words) = data.get("words").and_then(|w| w.as_array()) {
                for word in words {
                    let word_text = word.get("text").and_then(|t| t.as_str()).unwrap_or("");
                    if word_text.contains(text) {
                        // 3. 点击文本中心
                        let x = word.get("x").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                        let y = word.get("y").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                        let w = word.get("width").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                        let h = word.get("height").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                        return self.tap(x + w / 2, y + h / 2);
                    }
                }
            }
        }

        ToolResult::err(format!("未找到文本: {}", text))
    }
}