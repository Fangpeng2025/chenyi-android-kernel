// src/config/mod.rs
// 配置系统 - 复刻 Hermes 的配置架构

use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;

/// Hermes 配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HermesConfig {
    /// 模型名称
    pub model: String,
    
    /// 提供商
    pub provider: String,
    
    /// 自定义提供商
    #[serde(default)]
    pub custom_providers: HashMap<String, CustomProvider>,
    
    /// MCP 服务器配置
    #[serde(default)]
    pub mcp_servers: HashMap<String, McpServerConfig>,
    
    /// 技能目录
    #[serde(default = "default_skills_dir")]
    pub skills_dir: PathBuf,
}

/// 自定义提供商
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CustomProvider {
    pub base_url: String,
    pub api_key: String,
}

/// MCP 服务器配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct McpServerConfig {
    /// 命令（stdio 传输）
    #[serde(default)]
    pub command: Option<String>,
    
    /// 参数
    #[serde(default)]
    pub args: Vec<String>,
    
    /// 工作目录
    #[serde(default)]
    pub cwd: Option<PathBuf>,
    
    /// URL（HTTP 传输）
    #[serde(default)]
    pub url: Option<String>,
    
    /// 环境变量
    #[serde(default)]
    pub env: HashMap<String, String>,
    
    /// 超时（秒）
    #[serde(default = "default_timeout")]
    pub timeout: u64,
}

fn default_skills_dir() -> PathBuf {
    PathBuf::from("~/.hermes/skills")
}

fn default_timeout() -> u64 {
    120
}

impl HermesConfig {
    /// 从文件加载配置
    pub fn from_file(path: PathBuf) -> anyhow::Result<Self> {
        let content = std::fs::read_to_string(&path)?;
        let config: Self = serde_yaml::from_str(&content)?;
        Ok(config)
    }
    
    /// 从默认路径加载
    pub fn load_default() -> anyhow::Result<Self> {
        let config_path = dirs::config_dir()
            .unwrap_or_else(|| PathBuf::from("~/.hermes"))
            .join("config.yaml");
        
        if config_path.exists() {
            Self::from_file(config_path)
        } else {
            Ok(Self::default())
        }
    }
}

impl Default for HermesConfig {
    fn default() -> Self {
        Self {
            model: "glm-5".to_string(),
            provider: "custom:oneapi".to_string(),
            custom_providers: HashMap::new(),
            mcp_servers: HashMap::new(),
            skills_dir: default_skills_dir(),
        }
    }
}