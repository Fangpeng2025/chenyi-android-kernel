//! 工具注册表

use crate::types::{Result, Error};
use std::collections::HashMap;
use std::sync::Arc;
use parking_lot::Mutex;

/// 工具执行回调类型
/// 参数：(tool_name, params_json) -> Result<serde_json::Value>
pub type ToolCallback = Arc<dyn Fn(&str, &str) -> Result<serde_json::Value> + Send + Sync>;

/// 工具注册表
pub struct ToolRegistry {
    tools: HashMap<String, Tool>,
    callback: Mutex<Option<ToolCallback>>,
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
            callback: Mutex::new(None),
        };
        
        // 注册 Android 工具
        registry.register_android_tools();
        
        log::info!("[Tools] 已注册 {} 个工具", registry.tools.len());
        
        registry
    }
    
    /// 设置工具执行回调
    pub fn set_callback(&self, callback: ToolCallback) {
        let mut guard = self.callback.lock();
        *guard = Some(callback);
        log::info!("[Tools] 已设置工具执行回调");
    }
    
    /// 检查是否有回调
    pub fn has_callback(&self) -> bool {
        self.callback.lock().is_some()
    }
    
    /// 注册 Android 工具
    fn register_android_tools(&mut self) {
        // 截图
        self.register("screenshot", "截取当前屏幕，返回图片路径和尺寸", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // 点击
        self.register("tap", "点击屏幕指定坐标", serde_json::json!({
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "X 坐标"},
                "y": {"type": "integer", "description": "Y 坐标"}
            },
            "required": ["x", "y"]
        }));
        
        // 长按
        self.register("long_press", "长按屏幕指定坐标", serde_json::json!({
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "X 坐标"},
                "y": {"type": "integer", "description": "Y 坐标"},
                "duration": {"type": "integer", "description": "持续时间（毫秒）", "default": 1000}
            },
            "required": ["x", "y"]
        }));
        
        // 滑动
        self.register("swipe", "从起点滑动到终点", serde_json::json!({
            "type": "object",
            "properties": {
                "start_x": {"type": "integer", "description": "起点 X 坐标"},
                "start_y": {"type": "integer", "description": "起点 Y 坐标"},
                "end_x": {"type": "integer", "description": "终点 X 坐标"},
                "end_y": {"type": "integer", "description": "终点 Y 坐标"},
                "duration": {"type": "integer", "description": "持续时间（毫秒）", "default": 300}
            },
            "required": ["start_x", "start_y", "end_x", "end_y"]
        }));
        
        // 输入文本
        self.register("type_text", "在当前焦点输入框输入文本", serde_json::json!({
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "要输入的文本"}
            },
            "required": ["text"]
        }));
        
        // 按键
        self.register("press_key", "按下系统按键", serde_json::json!({
            "type": "object",
            "properties": {
                "key": {"type": "string", "description": "按键名称：home, back, recent, notifications, quick_settings"}
            },
            "required": ["key"]
        }));
        
        // 打开应用
        self.register("open_app", "打开指定应用", serde_json::json!({
            "type": "object",
            "properties": {
                "package": {"type": "string", "description": "应用包名"}
            },
            "required": ["package"]
        }));
        
        // 关闭应用
        self.register("close_app", "关闭当前应用（返回桌面）", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // 获取当前应用
        self.register("current_app", "获取当前前台应用信息（包名）", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // 列出应用
        self.register("list_apps", "列出已安装的应用（返回包名列表）", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // OCR
        self.register("ocr", "识别屏幕上的文字，返回文本列表和坐标", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // 查找文本
        self.register("find_text", "在屏幕上查找指定文本的位置", serde_json::json!({
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "要查找的文本"}
            },
            "required": ["text"]
        }));
        
        // 等待
        self.register("wait", "等待指定时间", serde_json::json!({
            "type": "object",
            "properties": {
                "duration": {"type": "integer", "description": "等待时间（毫秒）"}
            },
            "required": ["duration"]
        }));
        
        // 获取屏幕尺寸
        self.register("get_screen_size", "获取屏幕尺寸（宽度和高度）", serde_json::json!({
            "type": "object",
            "properties": {}
        }));
        
        // 滚动
        self.register("scroll", "滚动屏幕（up, down, left, right）", serde_json::json!({
            "type": "object",
            "properties": {
                "direction": {"type": "string", "description": "滚动方向：up, down, left, right"},
                "distance": {"type": "integer", "description": "滚动距离（像素）", "default": 500}
            },
            "required": ["direction"]
        }));
    }
    
    /// 注册工具
    fn register(&mut self, name: &str, description: &str, parameters: serde_json::Value) {
        self.tools.insert(name.to_string(), Tool {
            name: name.to_string(),
            description: description.to_string(),
            parameters,
        });
    }
    
    /// 执行工具
    pub fn execute(&self, name: &str, params: &str) -> Result<serde_json::Value> {
        log::info!("[Tools] 执行工具: {} 参数: {}", name, params);
        
        // 检查工具是否存在
        if !self.tools.contains_key(name) {
            log::error!("[Tools] 未知工具: {}", name);
            return Err(Error::Tool(format!("未知工具: {}", name)));
        }
        
        // 验证参数格式
        let params_value: serde_json::Value = match serde_json::from_str(params) {
            Ok(v) => v,
            Err(e) => {
                log::error!("[Tools] 参数 JSON 解析失败: {}", e);
                return Err(Error::Tool(format!("参数 JSON 解析失败: {}", e)));
            }
        };
        
        // 验证必需参数
        if let Some(tool) = self.tools.get(name) {
            if let Some(required) = tool.parameters.get("required").and_then(|r| r.as_array()) {
                if let Some(properties) = params_value.as_object() {
                    for req_field in required {
                        if let Some(field_name) = req_field.as_str() {
                            if !properties.contains_key(field_name) {
                                log::error!("[Tools] 缺少必需参数: {}", field_name);
                                return Err(Error::Tool(format!("缺少必需参数: {}", field_name)));
                            }
                        }
                    }
                }
            }
        }
        
        // 如果有回调，使用回调执行
        // 先克隆 Arc，避免在持有锁时调用（防止死锁）
        let callback_opt = {
            let guard = self.callback.lock();
            guard.clone()
        };
        
        if let Some(callback) = callback_opt {
            log::info!("[Tools] 使用回调执行工具");
            return callback(name, params);
        }
        
        // 没有回调，返回需要 Kotlin 执行的标记
        log::warn!("[Tools] 没有设置回调，返回待执行标记");
        Ok(serde_json::json!({
            "success": false,
            "needs_execution": true,
            "error": "工具需要在 Kotlin 端执行，请设置回调",
            "tool": name,
            "params": params
        }))
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
