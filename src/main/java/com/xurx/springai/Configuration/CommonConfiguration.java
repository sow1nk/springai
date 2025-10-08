package com.xurx.springai.Configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                  ToolCallbackProvider mathTool) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你的名字是悟空")
                .defaultToolCallbacks(mathTool.getToolCallbacks())
                .build();
    }
}
