//! 工具注册表

use crate::types::{Result, Error};
use std::collections::HashMap;
use std::sync::Arc;
use parking_lot::Mutex;

/// JNI 回调类型
type JniCallback = fn(tool: &str, params: &str) -> Result<serde_json::Value>;

/// 工具注册表
pub struct ToolRegistry {
    tools: HashMap<String, Tool>,
    jni_callback: Arc<Mutex<Option<JniCallback>>>,
}

/// 工具定义
#[derive(Debug, Clone)]
pub struct Tool {
    pub name: String,
    pub description: String,
    pub parameters: serde_json::Value,
}

impl ToolRegistry {
    pub fn new() -> Self {
        let mut registry = Self {
            tools: HashMap::new(),
            jni_callback: Arc::new(Mutex::new(None)),
        };
        
        // 注册 Android 工具
        registry.register_android_tools();
        
        registry
    }
    
    /// 注册 Android 工具
    fn register_android_tools(&mut self) {
        // 截图
        self.tools.insert("screenshot".to_string(), Tool {
            name: "screenshot".to_string(),
            description: "截取当前屏幕".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "region": {
                        "type": "object",
                        "description": "可选的截图区域",
                        "properties": {
                            "x": {"type": "integer"},
                            "y": {"type": "integer"},
                            "width": {"type": "integer"},
                            "height": {"type": "integer"}
                        }
                    }
                }
            }),
        });
        
        // 点击
        self.tools.insert("tap".to_string(), Tool {
            name: "tap".to_string(),
            description: "点击屏幕指定坐标".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "x": {"type": "integer", "description": "X 坐标"},
                    "y": {"type": "integer", "description": "Y 坐标"}
                },
                "required": ["x", "y"]
            }),
        });
        
        // 长按
        self.tools.insert("long_press".to_string(), Tool {
            name: "long_press".to_string(),
            description: "长按屏幕指定坐标".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "x": {"type": "integer", "description": "X 坐标"},
                    "y": {"type": "integer", "description": "Y 坐标"},
                    "duration": {"type": "integer", "description": "持续时间（毫秒）", "default": 500}
                },
                "required": ["x", "y"]
            }),
        });
        
        // 滑动
        self.tools.insert("swipe".to_string(), Tool {
            name: "swipe".to_string(),
            description: "滑动屏幕".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "start_x": {"type": "integer", "description": "起始 X 坐标"},
                    "start_y": {"type": "integer", "description": "起始 Y 坐标"},
                    "end_x": {"type": "integer", "description": "结束 X 坐标"},
                    "end_y": {"type": "integer", "description": "结束 Y 坐标"},
                    "duration": {"type": "integer", "description": "持续时间（毫秒）", "default": 300}
                },
                "required": ["start_x", "start_y", "end_x", "end_y"]
            }),
        });
        
        // 输入文本
        self.tools.insert("type_text".to_string(), Tool {
            name: "type_text".to_string(),
            description: "在当前输入框中输入文本".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "text": {"type": "string", "description": "要输入的文本"}
                },
                "required": ["text"]
            }),
        });
        
        // 按键
        self.tools.insert("press_key".to_string(), Tool {
            name: "press_key".to_string(),
            description: "按下按键".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "keycode": {"type": "integer", "description": "按键代码"}
                },
                "required": ["keycode"]
            }),
        });
        
        // 打开应用
        self.tools.insert("open_app".to_string(), Tool {
            name: "open_app".to_string(),
            description: "打开指定应用".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "package": {"type": "string", "description": "应用包名"}
                },
                "required": ["package"]
            }),
        });
        
        // 关闭应用
        self.tools.insert("close_app".to_string(), Tool {
            name: "close_app".to_string(),
            description: "关闭当前应用（返回桌面）".to_string(),
            parameters: serde_json::json!({"type": "object"}),
        });
        
        // 获取当前应用
        self.tools.insert("current_app".to_string(), Tool {
            name: "current_app".to_string(),
            description: "获取当前前台应用的包名".to_string(),
            parameters: serde_json::json!({"type": "object"}),
        });
        
        // 列出应用
        self.tools.insert("list_apps".to_string(), Tool {
            name: "list_apps".to_string(),
            description: "列出所有已安装的应用".to_string(),
            parameters: serde_json::json!({"type": "object"}),
        });
        
        // OCR
        self.tools.insert("ocr".to_string(), Tool {
            name: "ocr".to_string(),
            description: "识别屏幕文字".to_string(),
            parameters: serde_json::json!({"type": "object"}),
        });
        
        // 查找文本
        self.tools.insert("find_text".to_string(), Tool {
            name: "find_text".to_string(),
            description: "查找屏幕上的文本位置".to_string(),
            parameters: serde_json::json!({
                "type": "object",
                "properties": {
                    "text": {"type": "string", "description": "要查找的文本"}
                },
                "required": ["text"]
            }),
        });
    }
    
    /// 设置 JNI 回调
    pub fn set_jni_callback(&self, callback: JniCallback) {
        *self.jni_callback.lock() = Some(callback);
    }
    
    /// 执行工具
    pub fn execute(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        let tool = self.tools.get(name)
            .ok_or_else(|| Error::Tool(format!("未知工具: {}", name)))?;
        
        // 使用 JNI 回调执行
        if let Some(callback) = *self.jni_callback.lock() {
            callback(name, params)
        } else {
            Err(Error::Tool("JNI 回调未注册".to_string()))
        }
    }
    
    /// 列出所有工具（LLM 格式）
    pub fn list_tools(&self) -> Vec<crate::llm::ToolDefinition> {
        self.tools.values().map(|tool| {
            crate::llm::ToolDefinition::new(
                &tool.name,
                &tool.description,
                tool.parameters.clone(),
            )
        }).collect()
    }
    
    /// 工具数量
    pub fn count(&self) -> usize {
        self.tools.len()
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