package com.xurx.springai.configuration;

import com.xurx.springai.advisor.SensitiveWordFilterAdvisor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfiguration {

    private final ZhiPuAiChatModel ZhiPuChatModel;
    private final DeepSeekChatModel deepSeekChatModel;
    private final DashScopeChatModel dashScopeModel;
    private final ZhiPuAiEmbeddingModel zhiPuAiEmbeddingModel;

    private final ToolCallbackProvider mcpToolCallbackProvider;


    // 默认系统提示词
    private static final String DEFAULT_SYSTEM_PROMPT = """
            你是一位数据分析AI专家。
            当前用户：
            姓名：{name}, 身份：{identity}。
            """;

    /**
     * 敏感词列表 Bean
     */
    @Bean
    public List<String> sensitiveWords() {
        return List.of("TMD");
    }

    /**
     * 敏感词过滤 Advisor
     *
     * @param sensitiveWords
     * @return
     */
    @Bean
    public SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor(List<String> sensitiveWords) {
        return new SensitiveWordFilterAdvisor(sensitiveWords);
    }

    /**
     * 聊天记忆组件
     *
     * @param jdbcChatMemoryRepository
     * @return
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(2)
                .build();
    }

    /**
     * 定义一个模型Map，将声明的模型加入其中，方便路由选择
     */
    @Bean
    public Map<String, ChatClient> chatClientMap(ChatClient dashscopeChatClient,
                                                 ChatClient zhipuChatClient,
                                                 ChatClient deepseekChatClient) {
        return Map.of(
                "qwen", dashscopeChatClient,
                "deepseek", deepseekChatClient,
                "zhipu", zhipuChatClient

        );
    }

    /**
     * 百炼 ChatClient 实例
     */
    @Bean
    public ChatClient dashscopeChatClient(SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor,
                                          ChatMemory chatMemory) {
        return ChatClient.builder(dashScopeModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultToolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                .defaultAdvisors(
                        sensitiveWordFilterAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * 智谱 ChatClient 实例
     *
     * @return
     */
    @Bean
    public ChatClient zhipuChatClient(SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor,
                                      ChatMemory chatMemory) {
        return ChatClient.builder(ZhiPuChatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultToolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                .defaultAdvisors(
                        sensitiveWordFilterAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * DeepSeek ChatClient 实例
     *
     * @return
     */
    @Bean
    public ChatClient deepseekChatClient(SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor,
                                         ChatMemory chatMemory) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        sensitiveWordFilterAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultToolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                .build();
    }


    /**
     * 默认的 Embedding 模型
     * 用于向量存储，指定使用DashScope的Embedding模型
     */
    @Bean
    @Primary
    public EmbeddingModel defaultEmbeddingModel() {
        return zhiPuAiEmbeddingModel;
    }
}
