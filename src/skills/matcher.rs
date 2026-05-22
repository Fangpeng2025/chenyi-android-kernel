// src/skills/matcher.rs
// 技能匹配器 - 根据用户意图匹配合适的技能

use crate::skills::{Skill, SkillRegistry};
use std::sync::Arc;

/// 技能匹配器
pub struct SkillMatcher {
    /// 技能注册表
    registry: Arc<SkillRegistry>,
    
    /// 匹配策略
    strategy: MatchStrategy,
}

/// 匹配策略
#[derive(Debug, Clone)]
pub enum MatchStrategy {
    /// 关键词匹配（默认）
    Keyword,
    /// 语义匹配（需要嵌入模型）
    Semantic,
    /// 混合匹配（关键词 + 语义）
    Hybrid,
}

/// 匹配结果
#[derive(Debug, Clone)]
pub struct MatchResult {
    /// 匹配的技能
    pub skill: Skill,
    
    /// 匹配分数（0.0 - 1.0）
    pub score: f32,
    
    /// 匹配原因
    pub reason: String,
}

impl SkillMatcher {
    /// 创建新的技能匹配器
    pub fn new(registry: Arc<SkillRegistry>) -> Self {
        Self {
            registry,
            strategy: MatchStrategy::Keyword,
        }
    }
    
    /// 设置匹配策略
    pub fn with_strategy(mut self, strategy: MatchStrategy) -> Self {
        self.strategy = strategy;
        self
    }
    
    /// 根据用户输入匹配技能
    pub fn match_skill(&self, user_input: &str) -> Option<MatchResult> {
        match self.strategy {
            MatchStrategy::Keyword => self.match_by_keyword(user_input),
            MatchStrategy::Semantic => self.match_by_semantic(user_input),
            MatchStrategy::Hybrid => self.match_by_hybrid(user_input),
        }
    }
    
    /// 匹配多个技能（按分数排序）
    pub fn match_skills(&self, user_input: &str, top_k: usize) -> Vec<MatchResult> {
        let mut results = Vec::new();
        
        let skills = self.registry.get_all_skills();
        for skill in skills {
            if let Some(result) = self.match_skill_with_score(user_input, &skill) {
                results.push(result);
            }
        }
        
        // 按分数降序排序
        results.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap_or(std::cmp::Ordering::Equal));
        
        // 返回 top_k 个结果
        results.into_iter().take(top_k).collect()
    }
    
    /// 关键词匹配
    fn match_by_keyword(&self, user_input: &str) -> Option<MatchResult> {
        let input_lower = user_input.to_lowercase();
        let mut best_match: Option<MatchResult> = None;
        
        let skills = self.registry.get_all_skills();
        for skill in skills {
            let score = self.calculate_keyword_score(&input_lower, &skill);
            
            if score > 0.3 {
                // 阈值
                let result = MatchResult {
                    skill: skill.clone(),
                    score,
                    reason: format!("关键词匹配: {}", skill.name()),
                };
                
                if let Some(ref best) = best_match {
                    if score > best.score {
                        best_match = Some(result);
                    }
                } else {
                    best_match = Some(result);
                }
            }
        }
        
        best_match
    }
    
    /// 计算关键词匹配分数
    fn calculate_keyword_score(&self, input: &str, skill: &Skill) -> f32 {
        let mut score = 0.0;
        
        // 1. 检查技能名称是否在输入中
        let skill_name_lower = skill.name().to_lowercase();
        if input.contains(&skill_name_lower) {
            score += 0.5;
        }
        
        // 2. 检查描述中的关键词
        let desc_lower = skill.description().to_lowercase();
        let desc_keywords: Vec<&str> = desc_lower.split_whitespace().collect();
        let input_words: Vec<&str> = input.split_whitespace().collect();
        
        let mut keyword_matches = 0;
        for keyword in &desc_keywords {
            if input_words.contains(keyword) {
                keyword_matches += 1;
            }
        }
        
        if !desc_keywords.is_empty() {
            score += 0.3 * (keyword_matches as f32 / desc_keywords.len() as f32);
        }
        
        // 3. 检查技能内容中的关键词
        let content_lower = skill.content.to_lowercase();
        let important_keywords = self.extract_important_keywords(&content_lower);
        
        for keyword in important_keywords {
            if input.contains(&keyword) {
                score += 0.2;
            }
        }
        
        // 限制最大分数为 1.0
        score.min(1.0)
    }
    
    /// 从技能内容中提取重要关键词
    fn extract_important_keywords(&self, content: &str) -> Vec<String> {
        let mut keywords = Vec::new();
        
        // 查找标题中的关键词
        for line in content.lines() {
            if line.starts_with("## ") || line.starts_with("### ") {
                let title = line.trim_start_matches('#').trim();
                keywords.push(title.to_lowercase());
            }
        }
        
        // 查找代码块中的命令
        if content.contains("```") {
            let in_code = content
                .lines()
                .skip_while(|line| !line.starts_with("```"))
                .skip(1)
                .take_while(|line| !line.starts_with("```"))
                .collect::<Vec<_>>()
                .join(" ");
            
            // 提取命令关键词
            for word in in_code.split_whitespace() {
                if word.len() > 3 && !word.starts_with("--") && !word.starts_with("-") {
                    keywords.push(word.to_lowercase());
                }
            }
        }
        
        keywords
    }
    
    /// 语义匹配（需要嵌入模型，暂未实现）
    fn match_by_semantic(&self, _user_input: &str) -> Option<MatchResult> {
        log::warn!("[SkillMatcher] 语义匹配尚未实现，使用关键词匹配代替");
        None
    }
    
    /// 混合匹配
    fn match_by_hybrid(&self, user_input: &str) -> Option<MatchResult> {
        // 目前只使用关键词匹配
        // 未来可以结合语义匹配
        self.match_by_keyword(user_input)
    }
    
    /// 计算指定技能的匹配分数
    fn match_skill_with_score(&self, user_input: &str, skill: &Skill) -> Option<MatchResult> {
        let input_lower = user_input.to_lowercase();
        let score = self.calculate_keyword_score(&input_lower, skill);
        
        if score > 0.3 {
            Some(MatchResult {
                skill: skill.clone(),
                score,
                reason: format!("关键词匹配: {}", skill.name()),
            })
        } else {
            None
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_skill_matcher() {
        let registry = Arc::new(SkillRegistry::new());
        let matcher = SkillMatcher::new(registry);
        
        // 测试代码
    }
}
