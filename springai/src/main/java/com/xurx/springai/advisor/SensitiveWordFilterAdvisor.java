package com.xurx.springai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

import java.util.List;

/**
 * 敏感词过滤顾问
 * 在用户输入中检测并拦截敏感词
 */
@Slf4j
public class SensitiveWordFilterAdvisor implements BaseAdvisor {

    private final List<String> sensitiveWords;

    public SensitiveWordFilterAdvisor(List<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String content = chatClientRequest.prompt().getContents();
        log.debug("检查敏感词，原始内容: {}", content);

        // 检测敏感词
        for (String word : sensitiveWords) {
            if (content.contains(word)) {
                log.warn("检测到敏感词: {}", word);
                throw new IllegalArgumentException("请求包含敏感词汇，已被拒绝处理。");
            }
        }

        // 没有敏感词，直接返回原始请求
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return -100; // 设置较高优先级，在其他 Advisor 之前执行
    }
}
