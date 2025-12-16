package com.xurx.springai.Configuration;

import com.xurx.springai.Advisor.ReReadingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfiguration {

    // 敏感词过滤顾问
    private static final List<String> sensitiveWords = List.of("TMD");

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                 ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("""
                        你是一位数据分析AI专家。
                        当前用户：
                        姓名：{name}, 身份：{identity}。
                        """)
                .defaultAdvisors(new SimpleLoggerAdvisor(), new ReReadingAdvisor())
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();
    }
}
