package com.xurx.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    /**
     * 选择的模型标识（如：deepseek、qwen）
     */
    private String model;

    /**
     * 用户输入的提示词
     */
    private String message;

    /**
     * 用户唯一标识UUID
     */
    private String userId;

    /**
     * 会话ID（用于关联同一对话中的多条消息）
     */
    private String sessionId;
}
