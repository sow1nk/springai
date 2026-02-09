package com.xurx.springai.advisor;

import io.micrometer.core.instrument.Metrics;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;

public class MetricAdvisor implements CallAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 执行实际的调用
        ChatClientResponse clientResponse = callAdvisorChain.nextCall(chatClientRequest);
        // 获取响应的使用情况
        Usage usage = clientResponse.chatResponse().getMetadata().getUsage();
        // 输入 Prompt 的 Token 数量
        Integer promptTokens = usage.getPromptTokens();
        // 输出 Completion 的 Token 数量
        Integer completionTokens = usage.getCompletionTokens();
        // 总 Token 数量
        Integer totalTokens = usage.getTotalTokens();
        Metrics.counter("ai.prompt.tokens").increment(promptTokens);
        Metrics.counter("ai.completion.tokens").increment(completionTokens);
        Metrics.counter("ai.total.tokens").increment(totalTokens);
        return clientResponse;
    }

    @Override
    public String getName() {
        return "metric-advisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
