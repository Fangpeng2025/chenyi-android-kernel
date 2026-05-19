//! JNI 接口 - Android 平台支持

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jstring};
use parking_lot::Mutex;
use std::sync::OnceLock;
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
    
    // 直接在锁内执行（避免 async block）
    let guard = cell.lock();
    let kernel = match guard.as_ref() {
        Some(k) => k,
        None => {
            log::error!("[JNI] 错误: 内核未初始化");
            return error_response(&mut env, "内核未初始化");
        }
    };
    
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

    // 获取内核
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => return error_response(&mut env, "内核未初始化"),
    };

    // 执行工具
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
                Err(e) => error_response(&mut env, &e.to_string()),
            }
        }
        None => error_response(&mut env, "内核未初始化"),
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

/// 销毁内核
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
) {
    eprintln!("[JNI] 销毁内核");

    if let Some(cell) = KERNEL.get() {
        let mut guard = cell.lock();
        *guard = None;
    }

    eprintln!("[JNI] 内核已销毁");
}

// ============ 辅助函数 ============

fn json_to_jstring(env: &mut JNIEnv, json: &serde_json::Value) -> jstring {
    let s = serde_json::to_string(json).unwrap_or_else(|_| "{}".to_string());
    env.new_string(&s).unwrap().into_raw()
}

fn error_response(env: &mut JNIEnv, msg: &str) -> jstring {
    eprintln!("[JNI] 错误响应: {}", msg);
    let json = serde_json::json!({
        "success": false,
        "error": msg
    });
    json_to_jstring(env, &json)
}