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

/// JNI 工具回调（存储 Kotlin 端传递的回调函数指针）
static JNI_TOOL_CALLBACK: OnceLock<Mutex<Option<usize>>> = OnceLock::new();

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
            eprintln!("[JNI] 获取数据目录失败");
            return false as jboolean;
        }
    };

    eprintln!("[JNI] 初始化内核，数据目录: {}", data_dir);

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
            eprintln!("[JNI] 内核初始化成功");
            true as jboolean
        }
        Err(e) => {
            eprintln!("[JNI] 内核初始化失败: {}", e);
            false as jboolean
        }
    }
}

/// 注册工具回调
#[no_mangle]
pub extern "system" fn Java_com_chenyi_agent_Kernel_nativeRegisterToolCallback(
    _env: JNIEnv,
    _class: JClass,
    callback_ptr: usize,
) {
    eprintln!("[JNI] 注册工具回调: {}", callback_ptr);
    let cell = JNI_TOOL_CALLBACK.get_or_init(|| Mutex::new(None));
    *cell.lock() = Some(callback_ptr);
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

    // 获取内核
    let cell = match KERNEL.get() {
        Some(c) => c,
        None => return error_response(&mut env, "内核未初始化"),
    };

    let guard = cell.lock();
    let kernel = match guard.as_ref() {
        Some(k) => k,
        None => return error_response(&mut env, "内核未初始化"),
    };

    // 使用全局 tokio runtime 执行异步
    let rt = get_runtime();
    let result = rt.block_on(kernel.chat(&message));

    match result {
        Ok(response) => {
            let json = serde_json::json!({
                "success": true,
                "response": response
            });
            json_to_jstring(&mut env, &json)
        }
        Err(e) => error_response(&mut env, &e.to_string()),
    }
}

/// 执行工具
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

    let guard = cell.lock();
    let kernel = match guard.as_ref() {
        Some(k) => k,
        None => return error_response(&mut env, "内核未初始化"),
    };

    // 执行工具
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
    let json = serde_json::json!({
        "success": false,
        "error": msg
    });
    json_to_jstring(env, &json)
}