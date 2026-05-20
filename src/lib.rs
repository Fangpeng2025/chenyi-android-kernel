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
