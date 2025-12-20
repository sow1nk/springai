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
}
