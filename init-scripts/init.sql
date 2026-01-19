-- SpringAI 项目数据库初始化脚本
-- 创建时间：2025-01-15

-- ========================================
-- 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户唯一标识UUID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================
-- 聊天记录表
-- ========================================
CREATE TABLE IF NOT EXISTS `chat_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `model` VARCHAR(50) COMMENT '使用的模型',
    `tokens` INT COMMENT '消耗的token数',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天记录表';

-- ========================================
-- Spring AI 聊天记忆表
-- ========================================
CREATE TABLE IF NOT EXISTS `SPRING_AI_CHAT_MEMORY` (
    `conversation_id` VARCHAR(36) NOT NULL COMMENT '会话ID',
    `content` TEXT NOT NULL COMMENT '内容',
    `type` VARCHAR(10) NOT NULL COMMENT '类型',
    `timestamp` TIMESTAMP NOT NULL COMMENT '时间戳',
    INDEX `SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX` (`conversation_id`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Spring AI聊天记忆表';

-- ========================================
-- 插入测试数据（可选）
-- ========================================
-- 插入测试用户（密码：123456，已使用BCrypt加密）
-- INSERT INTO `user` (`user_id`, `username`, `password`, `email`) VALUES
-- ('test-user-001', 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'test@example.com');
