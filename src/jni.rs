//! JNI 接口 - Android 平台支持

use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jboolean, jstring};
use parking_lot::Mutex;
use std::sync::{OnceLock, Arc};
use tokio::runtime::Runtime;

use crate::agent::AgentKernel;
use crate::types::KernelConfig;

/// 全局内核实例
static KERNEL: OnceLock<Mutex<Option<AgentKernel>>> = OnceLock::new();

/// 全局 Tokio Runtime
static RUNTIME: OnceLock<Runtime> = OnceLock::new();

/// 获取或初始化 Runtime
fn get_runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| Runtime::new().expect("Failed to create tokio runtime"))
}

/// 设置 API Key 和 Base URL
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeSetApiKey(
    mut env: JNIEnv,
    _class: JClass,
    api_key: JString,
    base_url: JString,
    model: JString,
) -> jboolean {
    let api_key: String = match env.get_string(&api_key) {
        Ok(s) => s.into(),
        Err(_) => {
            log::error!("[JNI] 获取 API Key 失败");
            return false as jboolean;
        }
    };
    
    let base_url: String = match env.get_string(&base_url) {
        Ok(s) => s.into(),
        Err(_) => "https://oneapi.xintiandi.online/v1".to_string(),
    };
    
    let model: String = match env.get_string(&model) {
        Ok(s) => s.into(),
        Err(_) => "glm-5".to_string(),
    };
    
    log::info!("[JNI] 设置 API Key: {}..., Base URL: {}, Model: {}", api_key.chars().take(10).collect::<String>(), base_url, model);
    
    // 获取内核
    let cell = KERNEL.get_or_init(|| Mutex::new(None));
    let mut guard = cell.lock();
    
    if let Some(kernel) = guard.as_mut() {
        // 使用 Arc::make_mut 获取可变引用
        let llm = Arc::make_mut(&mut kernel.llm);
        llm.update_config(&api_key, &base_url, &model);
        log::info!("[JNI] API Key 已更新");
        true as jboolean
    } else {
        log::error!("[JNI] 错误: 内核未初始化，无法设置 API Key");
        false as jboolean
    }
}

/// 初始化内核
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeInit(
    mut env: JNIEnv,
    _class: JClass,
    data_dir: JString,
) -> jboolean {
    let data_dir: String = match env.get_string(&data_dir) {
        Ok(s) => s.into(),
        Err(_) => {
            log::error!("[JNI] 获取数据目录失败");
            return false as jboolean;
        }
    };

    log::info!("[JNI] 初始化内核，数据目录: {}", data_dir);

    // 初始化 Android 日志
    #[cfg(target_os = "android")]
    {
        let _ = android_logger::init_once(
            android_logger::Config::default()
                .with_max_level(log::LevelFilter::Debug)
                .with_tag("ChenyiKernel"),
        );
    }

    // 创建配置
    let config = KernelConfig {
        data_dir: std::path::PathBuf::from(data_dir),
        ..Default::default()
    };

    // 初始化内核
    match AgentKernel::new(config) {
        Ok(kernel) => {
            let cell = KERNEL.get_or_init(|| Mutex::new(None));
            let mut guard = cell.lock();
            *guard = Some(kernel);
            log::info!("[JNI] 内核初始化成功");
            true as jboolean
        }
        Err(e) => {
            log::error!("[JNI] 内核初始化失败: {}", e);
            false as jboolean
        }
    }
}

/// 注册工具执行回调（已弃用）
/// 当前架构下，工具执行由 Kotlin 端的 chatWithTools 自动处理
#[deprecated(note = "当前架构下未使用，工具执行由 Kotlin 端处理")]
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeRegisterToolCallback(
    _env: JNIEnv,
    _class: JClass,
    _callback_obj: JObject,
) -> jboolean {
    log::warn!("[JNI] nativeRegisterToolCallback 已弃用（当前架构下未使用）");
    true as jboolean
}

/// 发送消息
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeChat(
    mut env: JNIEnv,
    _class: JClass,
    message: JString,
) -> jstring {
    let message: String = match env.get_string(&message) {
        Ok(s) => s.into(),
        Err(_) => return error_response(&mut env, "获取消息失败"),
    };

    log::info!("[JNI] 收到消息: {}", message);

    // 获取内核
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => {
            log::error!("[JNI] 错误: 内核未初始化");
            return error_response(&mut env, "内核未初始化");
        }
    };

    log::info!("[JNI] 开始执行 chat");

    // 使用全局 tokio runtime 执行异步
    let rt = get_runtime();
    
    // 获取内核的 Arc 克隆
    let kernel = {
        let guard = cell.lock();
        match guard.as_ref() {
            Some(k) => k.clone(),
            None => {
                log::error!("[JNI] 错误: 内核未初始化");
                return error_response(&mut env, "内核未初始化");
            }
        }
    };
    
    // 执行 chat
    let result = rt.block_on(kernel.chat(&message));

    log::info!("[JNI] chat 执行完成");

    match result {
        Ok(response) => {
            log::info!("[JNI] 响应成功: {} 字节", response.len());
            let json = serde_json::json!({
                "success": true,
                "response": response
            });
            json_to_jstring(&mut env, &json)
        }
        Err(e) => {
            log::error!("[JNI] 错误: {}", e);
            error_response(&mut env, &e.to_string())
        }
    }
}

/// 执行工具（直接调用，用于测试）
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeExecuteTool(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString,
    params: JString,
) -> jstring {
    let tool: String = match env.get_string(&tool) {
        Ok(s) => s.into(),
        Err(_) => return error_response(&mut env, "获取工具名失败"),
    };

    let params: String = match env.get_string(&params) {
        Ok(s) => s.into(),
        Err(_) => return error_response(&mut env, "获取参数失败"),
    };

    log::info!("[JNI] 执行工具: {} 参数: {}", tool, params);
    
    // 获取内核
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => return error_response(&mut env, "内核未初始化"),
    };
    
    let guard = cell.lock();
    match guard.as_ref() {
        Some(kernel) => {
            match kernel.execute_tool(&tool, &params) {
                Ok(result) => {
                    let json = serde_json::json!({
                        "success": true,
                        "data": result
                    });
                    json_to_jstring(&mut env, &json)
                }
                Err(e) => error_response(&mut env, &e.to_string())
            }
        }
        None => error_response(&mut env, "内核未初始化")
    }
}

/// 获取内核状态
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeGetStatus(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => {
            let json = serde_json::json!({
                "initialized": false,
                "status": "not_initialized"
            });
            return json_to_jstring(&mut env, &json);
        }
    };

    let guard = cell.lock();
    match guard.as_ref() {
        Some(kernel) => {
            let status = kernel.status();
            json_to_jstring(&mut env, &serde_json::to_value(status).unwrap())
        }
        None => {
            let json = serde_json::json!({
                "initialized": false,
                "status": "not_initialized"
            });
            json_to_jstring(&mut env, &json)
        }
    }
}

/// 获取内核版本
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeGetKernelVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    // 返回版本号（从 Cargo.toml 读取）
    let version = env!("CARGO_PKG_VERSION");
    match env.new_string(version) {
        Ok(jstr) => jstr.into_raw(),
        Err(_) => match env.new_string("unknown") {
            Ok(jstr) => jstr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
    }
}

/// 清除会话历史
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeClearHistory(
    mut _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => return false as jboolean,
    };
    
    let guard = cell.lock();
    if let Some(kernel) = guard.as_ref() {
        kernel.clear_history();
        log::info!("[JNI] 会话历史已清除");
        true as jboolean
    } else {
        false as jboolean
    }
}

/// 提交工具执行结果，继续对话
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeSubmitToolResults(
    mut env: JNIEnv,
    _class: JClass,
    tool_results_json: JString,
) -> jstring {
    let json_str: String = match env.get_string(&tool_results_json) {
        Ok(s) => s.into(),
        Err(_) => return error_response(&mut env, "获取工具结果失败"),
    };

    log::info!("[JNI] 提交工具结果: {} 字节", json_str.len());

    // 解析工具结果 JSON
    // 格式: [{"tool_call_id": "xxx", "result": "..."}, ...]
    let tool_results: Vec<(String, String)> = match serde_json::from_str(&json_str) {
        Ok(arr) => {
            let arr: Vec<serde_json::Value> = arr;
            arr.iter().filter_map(|v| {
                let id = v.get("tool_call_id")?.as_str()?.to_string();
                let result = v.get("result")?.as_str()?.to_string();
                Some((id, result))
            }).collect()
        }
        Err(e) => {
            log::error!("[JNI] 解析工具结果失败: {}", e);
            return error_response(&mut env, &format!("解析工具结果失败: {}", e));
        }
    };

    // 获取内核
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => return error_response(&mut env, "内核未初始化"),
    };

    // 获取内核克隆
    let kernel = {
        let guard = cell.lock();
        match guard.as_ref() {
            Some(k) => k.clone(),
            None => return error_response(&mut env, "内核未初始化"),
        }
    };

    // 执行继续对话
    let rt = get_runtime();
    let result = rt.block_on(kernel.continue_with_tool_results(tool_results));

    match result {
        Ok(response) => {
            log::info!("[JNI] 继续对话成功: {} 字节", response.len());
            let json = serde_json::json!({
                "success": true,
                "response": response
            });
            json_to_jstring(&mut env, &json)
        }
        Err(e) => {
            log::error!("[JNI] 继续对话失败: {}", e);
            error_response(&mut env, &e.to_string())
        }
    }
}

/// 销毁内核
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeDestroy(_env: JNIEnv, _class: JClass) {
    log::info!("[JNI] 销毁内核");

    if let Some(cell) = KERNEL.get() {
        let mut guard = cell.lock();
        *guard = None;
    }

    log::info!("[JNI] 内核已销毁");
}

/// 更新内核配置
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeUpdateConfig(
    mut env: JNIEnv,
    _class: JClass,
    config_json: JString,
) -> jboolean {
    log::info!("[JNI] 更新内核配置");
    
    let config_str: String = match env.get_string(&config_json) {
        Ok(s) => s.into(),
        Err(e) => {
            log::error!("[JNI] 获取配置字符串失败: {:?}", e);
            return false as jboolean;
        }
    };
    
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => {
            log::error!("[JNI] 内核未初始化");
            return false as jboolean;
        }
    };
    
    let mut guard = cell.lock();
    match guard.as_mut() {
        Some(kernel) => {
            // 解析配置 JSON
            match serde_json::from_str::<serde_json::Value>(&config_str) {
                Ok(config) => {
                    // 更新 LLM 配置
                    let mut needs_reinit = false;
                    
                    // 获取当前配置的可变引用（通过 Arc::make_mut）
                    // 注意：由于 LlmClient 在 Arc 中，我们需要重新创建
                    
                    if let Some(endpoint) = config.get("endpoint").and_then(|v| v.as_str()) {
                        log::info!("[JNI] 更新 endpoint: {}", endpoint);
                        needs_reinit = true;
                    }
                    if let Some(api_key) = config.get("api_key").and_then(|v| v.as_str()) {
                        let preview: String = api_key.chars().take(10).collect();
                        log::info!("[JNI] 更新 api_key: {}***", preview);
                        needs_reinit = true;
                    }
                    if let Some(model) = config.get("model").and_then(|v| v.as_str()) {
                        log::info!("[JNI] 更新 model: {}", model);
                    }
                    
                    // 如果需要重新初始化 LLM 客户端
                    if needs_reinit {
                        log::info!("[JNI] 配置已更改，需要重新初始化 LLM 客户端");
                        // TODO: 实现 LLM 客户端重新初始化
                        // 当前先记录警告
                        log::warn!("[JNI] LLM 客户端重新初始化功能待实现");
                    }
                    
                    // 重新初始化 LLM 客户端
                    if needs_reinit {
                        log::info!("[JNI] 重新初始化 LLM 客户端");
                        
                        // 创建新的配置
                        let mut new_config = kernel.llm.config.clone();
                        
                        if let Some(endpoint) = config.get("endpoint").and_then(|v| v.as_str()) {
                            new_config.endpoint = endpoint.to_string();
                        }
                        if let Some(api_key) = config.get("api_key").and_then(|v| v.as_str()) {
                            new_config.api_key = api_key.to_string();
                        }
                        if let Some(model) = config.get("model").and_then(|v| v.as_str()) {
                            new_config.model = model.to_string();
                        }
                        if let Some(max_tokens) = config.get("max_tokens").and_then(|v| v.as_i64()) {
                            // 验证范围
                            if max_tokens > 0 && max_tokens <= 100000 {
                                new_config.max_tokens = max_tokens as u32;
                            } else {
                                log::warn!("[JNI] max_tokens 超出范围: {}, 使用默认值", max_tokens);
                            }
                        }
                        if let Some(temperature) = config.get("temperature").and_then(|v| v.as_f64()) {
                            // 验证范围
                            if temperature >= 0.0 && temperature <= 2.0 {
                                new_config.temperature = temperature as f32;
                            } else {
                                log::warn!("[JNI] temperature 超出范围: {}, 使用默认值", temperature);
                            }
                        }
                        
                        log::info!("[JNI] 新配置 - endpoint: {}, model: {}", new_config.endpoint, new_config.model);
                        
                        match crate::llm::LlmClient::new(new_config) {
                            Ok(new_client) => {
                                kernel.llm = std::sync::Arc::new(new_client);
                                log::info!("[JNI] LLM 客户端重新初始化成功");
                            }
                            Err(e) => {
                                log::error!("[JNI] LLM 客户端重新初始化失败: {}", e);
                                return false as jboolean;
                            }
                        }
                    }
                    
                    true as jboolean
                }
                Err(e) => {
                    log::error!("[JNI] 解析配置失败: {}", e);
                    false as jboolean
                }
            }
        }
        None => {
            log::error!("[JNI] 内核未初始化");
            false as jboolean
        }
    }
}

// ============ 辅助函数 ============

fn json_to_jstring(env: &mut JNIEnv, json: &serde_json::Value) -> jstring {
    let s = serde_json::to_string(json).unwrap_or_else(|_| "{}".to_string());
    match env.new_string(&s) {
        Ok(jstr) => jstr.into_raw(),
        Err(e) => {
            log::error!("[JNI] 创建字符串失败: {:?}", e);
            env.new_string("{}").unwrap().into_raw()
        }
    }
}

fn error_response(env: &mut JNIEnv, msg: &str) -> jstring {
    log::error!("[JNI] 错误响应: {}", msg);
    let json = serde_json::json!({
        "success": false,
        "error": msg
    });
    json_to_jstring(env, &json)
}
