//! 上下文压缩引擎
//! 
//! 实现 Hermes 风格的上下文压缩功能：
//! - Token 计数（基于字符估算）
//! - 阈值触发压缩
//! - LLM 摘要压缩策略

use crate::types::{Result, Error, CompressionConfig};
use crate::llm::{LlmClient, ChatMessage};
use std::sync::Arc;
use parking_lot::Mutex;

/// 上下文压缩器
pub struct ContextCompressor {
    config: CompressionConfig,
    llm_client: Arc<LlmClient>,
    stats: Mutex<CompressionStats>,
}

/// 压缩统计信息
#[derive(Debug, Default, Clone, serde::Serialize, serde::Deserialize)]
pub struct CompressionStats {
    /// 总压缩次数
    pub total_compressions: u64,
    /// 总节省的 token 数
    pub tokens_saved: u64,
    /// 原始 token 总数
    pub original_tokens: u64,
    /// 压缩后 token 总数
    pub compressed_tokens: u64,
}

impl ContextCompressor {
    pub fn new(config: CompressionConfig, llm_client: Arc<LlmClient>) -> Self {
        Self {
            config,
            llm_client,
            stats: Mutex::new(CompressionStats::default()),
        }
    }
    
    /// 估算消息的 token 数量
    /// 
    /// 使用简化估算：中文约 1.5 字符/token，英文约 4 字符/token
    pub fn estimate_tokens(messages: &[ChatMessage]) -> usize {
        let mut total_tokens = 0;
        
        for msg in messages {
            let content_str = msg.content.as_deref().unwrap_or("");
            let chinese_chars = content_str.chars().filter(|c| '\u{4e00}' <= *c && *c <= '\u{9fff}').count();
            let other_chars = content_str.chars().count().saturating_sub(chinese_chars);
            
            // 中文: 约 1.5 字符/token
            // 英文/其他: 约 4 字符/token
            let estimated_tokens = (chinese_chars as f64 / 1.5).ceil() as usize 
                + (other_chars as f64 / 4.0).ceil() as usize
                + 4; // role 和格式开销
            
            total_tokens += estimated_tokens;
        }
        
        total_tokens
    }
    
    /// 检查是否需要压缩
    pub fn needs_compression(&self, messages: &[ChatMessage]) -> bool {
        if !self.config.enabled {
            return false;
        }
        
        let token_count = Self::estimate_tokens(messages);
        token_count > self.config.compression_threshold
    }
    
    /// 压缩对话历史
    /// 
    /// # 策略
    /// 1. 保留系统提示词和最近的消息
    /// 2. 对中间消息进行摘要压缩
    /// 3. 使用 LLM 生成压缩摘要
    pub async fn compress(&self, messages: Vec<ChatMessage>) -> Result<Vec<ChatMessage>> {
        if !self.config.enabled {
            log::info!("[Compressor] 压缩已禁用，返回原始消息");
            return Ok(messages);
        }
        
        let original_tokens = Self::estimate_tokens(&messages);
        log::info!(
            "[Compressor] 开始压缩 {} 条消息，原始 token: {}",
            messages.len(),
            original_tokens
        );
        
        // 如果消息太少，不压缩
        if messages.len() < 4 {
            log::info!("[Compressor] 消息数量少于 4，跳过压缩");
            return Ok(messages);
        }
        
        // 分离系统消息和对话消息
        let (system_msgs, mut conversation): (Vec<_>, Vec<_>) = messages
            .into_iter()
            .partition(|m| m.role == "system");
        
        // 保留最近的消息（基于配置的目标比例）
        let target_count = (conversation.len() as f64 * self.config.target_ratio).ceil() as usize;
        let keep_recent = (target_count as f64 * 0.6).ceil() as usize; // 60% 保留最近
        let keep_earlier = target_count.saturating_sub(keep_recent);
        
        log::info!(
            "[Compressor] 目标保留 {} 条消息（最近 {} 条 + 早期 {} 条）",
            target_count, keep_recent, keep_earlier
        );
        
        // 如果要保留的消息数量已经足够少，不压缩
        if conversation.len() <= target_count {
            log::info!("[Compressor] 消息数量已在目标范围内，跳过压缩");
            return Ok([system_msgs, conversation].concat());
        }
        
        // 分割消息
        let recent_msgs: Vec<_> = conversation.drain(conversation.len().saturating_sub(keep_recent)..).collect();
        let earlier_msgs = conversation;
        
        // 如果早期消息太少，直接保留
        if earlier_msgs.len() <= keep_earlier {
            log::info!("[Compressor] 早期消息数量较少，直接保留");
            let mut result = system_msgs;
            result.extend(earlier_msgs);
            result.extend(recent_msgs);
            return Ok(result);
        }
        
        // 对早期消息进行摘要压缩
        let compressed_summary = self.compress_messages(&earlier_msgs).await?;
        
        // 构建压缩后的消息列表
        let mut compressed = system_msgs;
        
        // 添加摘要消息
        compressed.push(ChatMessage {
            role: "system".to_string(),
            content: Some(format!("[历史摘要]\n{}", compressed_summary)),
            tool_calls: None,
            tool_call_id: None,
        });
        
        // 添加保留的早期消息（如果还有）
        if keep_earlier > 0 {
            let preserved: Vec<_> = earlier_msgs.into_iter().take(keep_earlier).collect();
            compressed.extend(preserved);
        }
        
        // 添加最近的消息
        compressed.extend(recent_msgs);
        
        // 更新统计
        let compressed_tokens = Self::estimate_tokens(&compressed);
        let tokens_saved = original_tokens.saturating_sub(compressed_tokens);
        
        {
            let mut stats = self.stats.lock();
            stats.total_compressions += 1;
            stats.tokens_saved += tokens_saved as u64;
            stats.original_tokens += original_tokens as u64;
            stats.compressed_tokens += compressed_tokens as u64;
        }
        
        log::info!(
            "[Compressor] 压缩完成: {} -> {} tokens (节省 {})",
            original_tokens, compressed_tokens, tokens_saved
        );
        
        Ok(compressed)
    }
    
    /// 使用 LLM 压缩消息
    async fn compress_messages(&self, messages: &[ChatMessage]) -> Result<String> {
        // 构建压缩提示词
        let conversation_text = messages
            .iter()
            .map(|m| {
                let content = m.content.as_deref().unwrap_or("");
                format!("{}: {}", m.role, content)
            })
            .collect::<Vec<_>>()
            .join("\n\n");
        
        let compression_prompt = format!(
            "请将以下对话历史压缩为简洁的摘要，保留关键信息和上下文。要求：\n\
            1. 保留用户的核心意图和需求\n\
            2. 保留重要的决策和结论\n\
            3. 保留关键的技术细节\n\
            4. 使用简洁的语言，避免冗余\n\
            5. 摘要长度不超过原文的 30%\n\n\
            对话历史：\n{}\n\n\
            摘要：",
            conversation_text
        );
        
        // 调用 LLM 生成摘要
        let summary = self.llm_client.compress(&compression_prompt).await?;
        
        Ok(summary)
    }
    
    /// 获取压缩统计信息
    pub fn get_stats(&self) -> CompressionStats {
        self.stats.lock().clone()
    }
    
    /// 重置统计信息
    pub fn reset_stats(&self) {
        *self.stats.lock() = CompressionStats::default();
    }
    
    /// 更新配置
    pub fn update_config(&mut self, config: CompressionConfig) {
        self.config = config;
        log::info!("[Compressor] 配置已更新");
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_estimate_tokens() {
        let messages = vec![
            ChatMessage {
                role: "user".to_string(),
                content: "你好，这是一个测试消息".to_string(),
                tool_calls: None,
                tool_call_id: None,
            },
            ChatMessage {
                role: "assistant".to_string(),
                content: "收到，我会帮你处理。This is a test message.".to_string(),
                tool_calls: None,
                tool_call_id: None,
            },
        ];
        
        let tokens = ContextCompressor::estimate_tokens(&messages);
        println!("Estimated tokens: {}", tokens);
        assert!(tokens > 0);
    }
}
