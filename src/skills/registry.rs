// src/skills/registry.rs
// 技能注册表 - 管理所有已加载的技能

use crate::skills::{Skill, SkillError, SkillLoader};
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::{Arc, RwLock};

/// 技能注册表
pub struct SkillRegistry {
    /// 技能映射（名称 -> 技能）
    skills: Arc<RwLock<HashMap<String, Skill>>>,
    
    /// 技能分类（分类 -> 技能名称列表）
    categories: Arc<RwLock<HashMap<String, Vec<String>>>>,
}

impl SkillRegistry {
    /// 创建空的技能注册表
    pub fn new() -> Self {
        Self {
            skills: Arc::new(RwLock::new(HashMap::new())),
            categories: Arc::new(RwLock::new(HashMap::new())),
        }
    }
    
    /// 从目录加载技能
    pub fn from_dir(skills_dir: PathBuf) -> Result<Self, SkillError> {
        let registry = Self::new();
        registry.load_skills(skills_dir)?;
        Ok(registry)
    }
    
    /// 加载技能
    pub fn load_skills(&self, skills_dir: PathBuf) -> Result<(), SkillError> {
        let loader = SkillLoader::new(skills_dir);
        let skills = loader.load_all()?;
        
        // 获取目录结构
        let structure = loader.get_skill_structure()?;
        
        // 注册技能
        {
            let mut skills_map = self.skills.write().unwrap();
            for skill in skills {
                skills_map.insert(skill.name().to_string(), skill);
            }
        }
        
        // 注册分类
        {
            let mut categories_map = self.categories.write().unwrap();
            for (category, skill_names) in structure {
                categories_map.insert(category, skill_names);
            }
        }
        
        Ok(())
    }
    
    /// 注册单个技能
    pub fn register(&self, skill: Skill) {
        let mut skills = self.skills.write().unwrap();
        skills.insert(skill.name().to_string(), skill);
    }
    
    /// 获取技能
    pub fn get_skill(&self, name: &str) -> Option<Skill> {
        let skills = self.skills.read().unwrap();
        skills.get(name).cloned()
    }
    
    /// 获取所有技能
    pub fn get_all_skills(&self) -> Vec<Skill> {
        let skills = self.skills.read().unwrap();
        skills.values().cloned().collect()
    }
    
    /// 获取技能数量
    pub fn count(&self) -> usize {
        let skills = self.skills.read().unwrap();
        skills.len()
    }
    
    /// 获取所有分类
    pub fn get_categories(&self) -> Vec<String> {
        let categories = self.categories.read().unwrap();
        categories.keys().cloned().collect()
    }
    
    /// 获取分类下的技能
    pub fn get_skills_by_category(&self, category: &str) -> Vec<Skill> {
        let categories = self.categories.read().unwrap();
        let skills = self.skills.read().unwrap();
        
        if let Some(skill_names) = categories.get(category) {
            skill_names
                .iter()
                .filter_map(|name| skills.get(name).cloned())
                .collect()
        } else {
            Vec::new()
        }
    }
    
    /// 列出所有技能（简要信息）
    pub fn list_skills(&self) -> Vec<SkillInfo> {
        let skills = self.skills.read().unwrap();
        skills
            .values()
            .map(|skill| SkillInfo {
                name: skill.name().to_string(),
                description: skill.description().to_string(),
                version: skill.metadata.version.clone(),
            })
            .collect()
    }
}

impl Default for SkillRegistry {
    fn default() -> Self {
        Self::new()
    }
}

/// 技能简要信息
#[derive(Debug, Clone, serde::Serialize)]
pub struct SkillInfo {
    pub name: String,
    pub description: String,
    pub version: String,
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_skill_registry() {
        let registry = SkillRegistry::new();
        assert_eq!(registry.count(), 0);
    }
}
