// src/skills/loader.rs
// 技能加载器 - 从目录加载所有 SKILL.md 文件

use crate::skills::{Skill, SkillError};
use std::collections::HashMap;
use std::fs;
use std::path::{Path, PathBuf};
use walkdir::WalkDir;

/// 技能加载器
pub struct SkillLoader {
    /// 技能根目录（如 ~/.hermes/skills/）
    skills_dir: PathBuf,
    
    /// 是否递归加载子目录
    recursive: bool,
}

impl SkillLoader {
    /// 创建新的技能加载器
    pub fn new(skills_dir: PathBuf) -> Self {
        Self {
            skills_dir,
            recursive: true,
        }
    }
    
    /// 设置是否递归加载
    pub fn with_recursive(mut self, recursive: bool) -> Self {
        self.recursive = recursive;
        self
    }
    
    /// 加载所有技能
    pub fn load_all(&self) -> Result<Vec<Skill>, SkillError> {
        log::info!("[SkillLoader] 开始从 {} 加载技能", self.skills_dir.display());
        
        if !self.skills_dir.exists() {
            log::warn!("[SkillLoader] 技能目录不存在: {}", self.skills_dir.display());
            return Ok(Vec::new());
        }
        
        let mut skills = Vec::new();
        
        // 查找所有 SKILL.md 文件
        let skill_files = self.find_skill_files()?;
        
        for file_path in skill_files {
            match Skill::from_file(file_path.clone()) {
                Ok(skill) => {
                    log::info!("[SkillLoader] 成功加载技能: {} ({})", skill.name(), file_path.display());
                    skills.push(skill);
                }
                Err(e) => {
                    log::error!("[SkillLoader] 加载技能失败 {}: {}", file_path.display(), e);
                    // 继续加载其他技能，不中断
                }
            }
        }
        
        log::info!("[SkillLoader] 共加载 {} 个技能", skills.len());
        Ok(skills)
    }
    
    /// 加载单个技能
    pub fn load_skill(&self, skill_name: &str) -> Result<Option<Skill>, SkillError> {
        // 查找技能文件
        let skill_files = self.find_skill_files()?;
        
        for file_path in skill_files {
            if let Ok(skill) = Skill::from_file(file_path) {
                if skill.name() == skill_name {
                    return Ok(Some(skill));
                }
            }
        }
        
        Ok(None)
    }
    
    /// 查找所有 SKILL.md 文件
    fn find_skill_files(&self) -> Result<Vec<PathBuf>, SkillError> {
        let mut files = Vec::new();
        
        if self.recursive {
            // 递归查找
            for entry in WalkDir::new(&self.skills_dir)
                .follow_links(true)
                .into_iter()
                .filter_map(|e| e.ok())
            {
                let path = entry.path();
                if path.file_name().map(|n| n == "SKILL.md").unwrap_or(false) {
                    files.push(path.to_path_buf());
                }
            }
        } else {
            // 只查找当前目录
            for entry in fs::read_dir(&self.skills_dir)
                .map_err(|e| SkillError::LoadError(format!("无法读取目录: {}", e)))?
            {
                let entry = entry.map_err(|e| SkillError::LoadError(format!("读取条目失败: {}", e)))?;
                let path = entry.path();
                if path.file_name().map(|n| n == "SKILL.md").unwrap_or(false) {
                    files.push(path);
                }
            }
        }
        
        Ok(files)
    }
    
    /// 获取技能目录结构
    pub fn get_skill_structure(&self) -> Result<HashMap<String, Vec<String>>, SkillError> {
        let mut structure = HashMap::new();
        
        // 遍历技能目录的第一层子目录（分类）
        for entry in fs::read_dir(&self.skills_dir)
            .map_err(|e| SkillError::LoadError(format!("无法读取目录: {}", e)))?
        {
            let entry = entry.map_err(|e| SkillError::LoadError(format!("读取条目失败: {}", e)))?;
            let path = entry.path();
            
            if path.is_dir() {
                let category = path.file_name()
                    .and_then(|n| n.to_str())
                    .unwrap_or("unknown")
                    .to_string();
                
                // 查找该分类下的所有技能
                let mut skills_in_category = Vec::new();
                
                for skill_entry in WalkDir::new(&path)
                    .max_depth(2)  // 只查找一层
                    .into_iter()
                    .filter_map(|e| e.ok())
                {
                    let skill_path = skill_entry.path();
                    if skill_path.file_name().map(|n| n == "SKILL.md").unwrap_or(false) {
                        if let Ok(skill) = Skill::from_file(skill_path.to_path_buf()) {
                            skills_in_category.push(skill.name().to_string());
                        }
                    }
                }
                
                if !skills_in_category.is_empty() {
                    structure.insert(category, skills_in_category);
                }
            }
        }
        
        Ok(structure)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_skill_loader() {
        let loader = SkillLoader::new(PathBuf::from("test_skills"));
        // 测试代码
    }
}
