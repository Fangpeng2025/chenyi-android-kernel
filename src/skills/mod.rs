// src/skills/mod.rs
// 技能系统 - 完全复刻 Hermes Agent 的技能架构

pub mod skill;
pub mod loader;
pub mod registry;
pub mod matcher;

pub use skill::{Skill, SkillMetadata, SkillError};
pub use loader::SkillLoader;
pub use registry::{SkillRegistry, SkillInfo};
pub use matcher::{SkillMatcher, MatchStrategy, MatchResult};
