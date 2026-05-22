// src/skills/skill.rs
// 技能定义 - 复刻 Hermes 的 SKILL.md 格式

use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// 技能定义（对应 Hermes 的 SKILL.md）
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Skill {
    /// YAML frontmatter 元数据
    pub metadata: SkillMetadata,
    
    /// Markdown 内容（技能说明、工作流程等）
    pub content: String,
    
    /// 技能文件路径
    pub file_path: PathBuf,
}

/// 技能元数据（YAML frontmatter）
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SkillMetadata {
    /// 技能名称
    pub name: String,
    
    /// 技能描述
    pub description: String,
    
    /// 版本号
    pub version: String,
    
    /// 作者
    #[serde(default)]
    pub author: String,
    
    /// 许可证
    #[serde(default = "default_license")]
    pub license: String,
    
    /// 支持的平台
    #[serde(default)]
    pub platforms: Vec<String>,
    
    /// 扩展元数据
    #[serde(default)]
    pub metadata: serde_json::Value,
}

fn default_license() -> String {
    "MIT".to_string()
}

impl Skill {
    /// 从 SKILL.md 文件加载技能
    pub fn from_file(path: PathBuf) -> Result<Self, SkillError> {
        let content = std::fs::read_to_string(&path)
            .map_err(|e| SkillError::LoadError(format!("无法读取文件 {}: {}", path.display(), e)))?;
        
        Self::from_markdown(content, path)
    }
    
    /// 从 Markdown 内容解析技能
    pub fn from_markdown(content: String, file_path: PathBuf) -> Result<Self, SkillError> {
        // 解析 YAML frontmatter
        let (metadata, body) = parse_frontmatter(&content)?;
        
        Ok(Skill {
            metadata,
            content: body,
            file_path,
        })
    }
    
    /// 获取技能名称
    pub fn name(&self) -> &str {
        &self.metadata.name
    }
    
    /// 获取技能描述
    pub fn description(&self) -> &str {
        &self.metadata.description
    }
    
    /// 检查是否支持指定平台
    pub fn supports_platform(&self, platform: &str) -> bool {
        self.metadata.platforms.is_empty() || self.metadata.platforms.contains(&platform.to_string())
    }
    
    /// 获取工作流程内容（Markdown中的特定部分）
    pub fn get_workflow(&self) -> Option<String> {
        // 查找 ## Workflow 或 ## 工作流程 部分
        let workflow: String = self.content.lines()
            .skip_while(|line| !line.starts_with("## Workflow") && !line.starts_with("## 工作流程"))
            .skip(1)
            .take_while(|line| !line.starts_with("## "))
            .collect::<Vec<_>>()
            .join("\n");
        
        if workflow.is_empty() {
            None
        } else {
            Some(workflow)
        }
    }
}

/// 解析 YAML frontmatter
fn parse_frontmatter(content: &str) -> Result<(SkillMetadata, String), SkillError> {
    // 查找 YAML frontmatter 的边界
    let lines = content.lines().collect::<Vec<_>>();
    
    if lines.is_empty() || lines[0] != "---" {
        return Err(SkillError::ParseError("缺少 YAML frontmatter 开始标记 ---".to_string()));
    }
    
    // 查找结束的 ---
    let end_index = lines.iter().position(|line| *line == "---").unwrap_or(0);
    if end_index == 0 {
        return Err(SkillError::ParseError("缺少 YAML frontmatter 结束标记 ---".to_string()));
    }
    
    // 提取 YAML 内容
    let yaml_content = lines[1..end_index].join("\n");
    
    // 解析 YAML
    let metadata: SkillMetadata = serde_yaml::from_str(&yaml_content)
        .map_err(|e| SkillError::ParseError(format!("YAML 解析错误: {}", e)))?;
    
    // 提取 Markdown 内容（去掉 frontmatter）
    let markdown_content = lines[end_index + 1..].join("\n");
    
    Ok((metadata, markdown_content))
}

/// 技能错误类型
#[derive(Debug, thiserror::Error)]
pub enum SkillError {
    #[error("加载错误: {0}")]
    LoadError(String),
    
    #[error("解析错误: {0}")]
    ParseError(String),
    
    #[error("执行错误: {0}")]
    ExecutionError(String),
}