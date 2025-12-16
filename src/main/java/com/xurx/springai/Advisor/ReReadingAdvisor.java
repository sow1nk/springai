package com.xurx.springai.Advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReReadingAdvisor implements BaseAdvisor {

    private static final String DEFAULT_USER_TEXT_ADVISE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
            ==============
            """;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        // 用户输入内容
        String contents = chatClientRequest.prompt().getContents();
        log.info("Original contents: {}", contents);

        // 修改用户输入内容，加入"请重新阅读问题"提示
        PromptTemplate re2InputQuery = PromptTemplate.builder()
                .template(DEFAULT_USER_TEXT_ADVISE)
                .variables(Map.of("re2_input_query", contents))
                .build();

        // 渲染模板获取实际内容
        String renderedContent = re2InputQuery.render();
        log.info("Transformed to: {}", renderedContent);

        // 更新请求中的提示内容
        return chatClientRequest.mutate()
                .prompt(new Prompt(renderedContent))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
