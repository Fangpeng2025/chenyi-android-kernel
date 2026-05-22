// src/agent/orchestrator.rs
// Agent 编排器 - 完全复刻 Hermes 的编排逻辑

use crate::skills::{SkillMatcher, SkillRegistry};
use crate::tools::ToolRegistry;
use crate::llm::{LlmClient, ToolDefinition};
use crate::config::HermesConfig;
use crate::mcp::ToolDiscovery;
use anyhow::Result;
use std::sync::Arc;

/// Agent 编排器
pub struct Orchestrator {
    /// 配置
    config: HermesConfig,
    
    /// 技能注册表
    skill_registry: Arc<SkillRegistry>,
    
    /// 技能匹配器
    skill_matcher: Arc<SkillMatcher>,
    
    /// 工具注册表
    tool_registry: Arc<ToolRegistry>,
    
    /// LLM 客户端
    llm_client: Arc<LlmClient>,
    
    /// MCP 工具发现器
    tool_discovery: Option<Arc<ToolDiscovery>>,
}

impl Orchestrator {
    /// 创建新的编排器
    pub fn new(config: HermesConfig) -> Result<Self> {
        // 初始化技能注册表
        let skill_registry = Arc::new(SkillRegistry::from_dir(config.skills_dir.clone())?);
        log::info!("[Orchestrator] 已加载 {} 个技能", skill_registry.count());
        
        // 初始化技能匹配器
        let skill_matcher = Arc::new(SkillMatcher::new(skill_registry.clone()));
        
        // 初始化工具注册表
        let tool_registry = Arc::new(ToolRegistry::new());
        log::info!("[Orchestrator] 已注册 {} 个工具", tool_registry.count());
        
        // 初始化 LLM 客户端
        let llm_config = crate::types::LlmConfig::default();
        let llm_client = Arc::new(LlmClient::new(llm_config)?);
        
        Ok(Self {
            config,
            skill_registry,
            skill_matcher,
            tool_registry,
            llm_client,
            tool_discovery: None,
        })
    }
    
    /// 初始化 MCP 工具
    pub async fn initialize_mcp(&mut self) -> Result<()> {
        log::info!("[Orchestrator] 初始化 MCP 工具");
        
        // TODO: 完整实现 MCP 初始化
        log::warn!("[Orchestrator] MCP 初始化待完善");
        
        Ok(())
    }
    
    /// 执行用户请求（核心流程）
    pub async fn execute(&self, user_input: &str) -> Result<String> {
        log::info!("[Orchestrator] 执行用户请求: {}", user_input);
        
        // 1. 尝试匹配技能
        if let Some(match_result) = self.skill_matcher.match_skill(user_input) {
            log::info!("[Orchestrator] 匹配到技能: {} (分数: {})", 
                match_result.skill.name(), match_result.score);
            
            // 执行技能工作流
            return self.execute_skill(&match_result.skill, user_input).await;
        }
        
        // 2. 没有匹配到技能，使用 LLM + 工具循环
        log::info!("[Orchestrator] 未匹配到技能，使用 LLM + 工具循环");
        self.chat_with_tools(user_input).await
    }
    
    /// 执行技能
    async fn execute_skill(&self, skill: &crate::skills::Skill, user_input: &str) -> Result<String> {
        log::info!("[Orchestrator] 执行技能: {}", skill.name());
        
        // TODO: 将技能内容作为系统提示词注入
        // 目前简化处理：直接调用 chat_with_tools
        log::warn!("[Orchestrator] 技能执行待完善，暂时使用基础对话");
        self.chat_with_tools(user_input).await
    }
    
    /// LLM + 工具循环（Hermes 核心）
    async fn chat_with_tools(&self, user_input: &str) -> Result<String> {
        log::info!("[Orchestrator] 开始 chat_with_tools 循环");
        
        // 构建消息历史（包含系统提示）
        let mut messages = vec![
            crate::llm::ChatMessage::system("你是一个智能助手，可以使用工具来完成用户请求。当需要调用工具时，直接返回工具调用。"),
            crate::llm::ChatMessage::user(user_input),
        ];
        
        // 获取工具定义
        let tools = self.tool_registry.list_tools();
        log::info!("[Orchestrator] 可用工具数量: {}", tools.len());
        
        // 工具循环（最多10轮）
        const MAX_ROUNDS: usize = 10;
        for round in 1..=MAX_ROUNDS {
            log::info!("[Orchestrator] 第 {} 轮对话", round);
            
            // 调用 LLM
            let response = self.llm_client.chat_with_tools(&messages, tools.clone()).await?;
            
            // 检查是否有工具调用
            if response.tool_calls.is_empty() {
                // 没有工具调用，返回最终响应
                log::info!("[Orchestrator] LLM 返回最终响应");
                return Ok(response.content.unwrap_or_else(|| "无响应内容".to_string()));
            }
            
            log::info!("[Orchestrator] LLM 请求调用 {} 个工具", response.tool_calls.len());
            
            // 添加助手消息到历史
            messages.push(crate::llm::ChatMessage::assistant_with_tools(
                &response.content.clone().unwrap_or_default(),
                response.tool_calls.clone(),
            ));
            
            // 执行所有工具调用
            for tool_call in &response.tool_calls {
                let tool_name = &tool_call.function.name;
                let tool_args = &tool_call.function.arguments;
                
                log::info!("[Orchestrator] 执行工具: {} 参数: {}", tool_name, tool_args);
                
                // 执行工具
                let result = self.tool_registry.execute(tool_name, tool_args)?;
                let result_str = serde_json::to_string(&result)?;
                
                log::info!("[Orchestrator] 工具返回: {}", result_str);
                
                // 添加工具结果到历史
                messages.push(crate::llm::ChatMessage::tool_result(&tool_call.id, &result_str));
            }
        }
        
        // 达到最大轮数，返回最后一条消息
        log::warn!("[Orchestrator] 达到最大工具调用轮数: {}", MAX_ROUNDS);
        Ok("达到最大工具调用轮数，请简化请求".to_string())
    }
    
    /// 获取技能列表
    pub fn list_skills(&self) -> Vec<crate::skills::SkillInfo> {
        self.skill_registry.list_skills()
    }
    
    /// 获取工具列表
    pub fn list_tools(&self) -> Vec<crate::llm::ToolDefinition> {
        self.tool_registry.list_tools()
    }
}