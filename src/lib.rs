//! 晨翼Agent Android 内核
//! 
//! 基于 Rust 的高性能 AI Agent 内核，专为 Android 平台优化
//! 
//! # 架构
//! 
//! ```text
//! ┌─────────────────────────────────────┐
//! │         Kotlin UI (Compose)          │
//! └─────────────────────────────────────┘
//!                    │ JNI
//!                    ▼
//! ┌─────────────────────────────────────┐
//! │           Rust 内核                  │
//! │  ┌─────────┐ ┌─────────┐ ┌────────┐│
//! │  │  Agent  │ │  Memory │ │  LLM   ││
//! │  │  Loop   │ │  Engine │ │ Client ││
//! │  └─────────┘ └─────────┘ └────────┘│
//! │  ┌─────────────────────────────────┐│
//! │  │     Android Native Tools        ││
//! │  │  Screen │ Input │ App │ OCR     ││
//! │  └─────────────────────────────────┘│
//! └─────────────────────────────────────┘
//! ```

pub mod agent;
pub mod memory;
pub mod llm;
pub mod storage;
pub mod tools;
pub mod types;

#[cfg(feature = "android")]
pub mod jni;

// 导出常用类型
pub use types::{Result, Error, KernelConfig};
pub use agent::AgentKernel;

use once_cell::sync::OnceCell;
use parking_lot::Mutex;

/// 全局内核实例
static KERNEL: OnceCell<Mutex<AgentKernel>> = OnceCell::new();

/// 初始化内核
pub fn init(config: KernelConfig) -> Result<()> {
    let kernel = AgentKernel::new(config)?;
    KERNEL.set(Mutex::new(kernel))
        .map_err(|_| Error::AlreadyInitialized)?;
    Ok(())
}

/// 获取内核实例
pub fn get_kernel() -> Option<&'static Mutex<AgentKernel>> {
    KERNEL.get()
}
