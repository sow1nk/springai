package com.xurx.springai.configuration;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.xurx.springai.advisor.SensitiveWordFilterAdvisor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 多模型配置类
 * 根据配置文件初始化多个 AI 模型实例
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ModelArgsProperties.class)
@RequiredArgsConstructor
public class MultiModelConfiguration {

    private final ModelArgsProperties modelArgsProperties;

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
        return List.of("TMD", "傻逼", "草泥马");
    }

    /**
     * 敏感词过滤 Advisor
     */
    @Bean
    public SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor(List<String> sensitiveWords) {
        return new SensitiveWordFilterAdvisor(sensitiveWords);
    }

    /**
     * 定义一个模型Map，将声明的模型加入其中，方便路由选择
     */
    @Bean
    public Map<String, ChatClient> chatClientMap(ChatClient deepseekChatClient,
                                                 ChatClient dashscopeChatClient) {
        return Map.of(
                "deepseek", deepseekChatClient,
                "qwen", dashscopeChatClient
        );
    }

    /**
     * 聊天记忆组件
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
     * DeepSeek 模型
     * @return
     */
    @Bean(name = "deepseekChatClient")
    public ChatClient deepseekChatClient(ToolCallbackProvider toolCallbackProvider,
                                         ChatMemory chatMemory,
                                         SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor,
                                         RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        // 获取 DeepSeekApi 实例
        ModelArgsProperties.ModelCfg deepseek = modelArgsProperties.getModelCfg("deepseek");
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(deepseek.getBaseUrl())
                .apiKey(deepseek.getApiKey())
                .build();
        // 创建 DeepSeekChatModel 实例
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(DeepSeekChatOptions.builder().model(deepseek.getName()).build())
                .build();
        log.info("成功创建 DeepSeekChatModel: {}", deepseek.getName());
        // 创建 ChatClient 实例并返回
        return ChatClient.builder(deepSeekChatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        sensitiveWordFilterAdvisor, // 敏感词过滤
                        retrievalAugmentationAdvisor, // RAG 检索增强
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // 聊天记忆
                )
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()) // 设置工具回调
                .build();
    }

    /**
     * 阿里百炼模型
     * @return
     */
    @Bean(name = "dashscopeChatClient")
    public ChatClient dashscopeChatClient(ToolCallbackProvider toolCallbackProvider,
                                          ChatMemory chatMemory,
                                          SensitiveWordFilterAdvisor sensitiveWordFilterAdvisor,
                                          RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        ModelArgsProperties.ModelCfg qwenCfg = modelArgsProperties.getModelCfg("qwen");
        // 获取 DashScopeApi 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .baseUrl(qwenCfg.getBaseUrl())
                .apiKey(qwenCfg.getApiKey())
                .build();
        // 创建 DashScopeModel 实例
        DashScopeChatModel dashScopeChatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder().model(qwenCfg.getName()).build())
                .build();
        // 创建 ChatClient 实例并返回
        log.info("成功创建 DashScopeChatModel: {}", qwenCfg.getName());
        return ChatClient.builder(dashScopeChatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        sensitiveWordFilterAdvisor, // 敏感词过滤
                        retrievalAugmentationAdvisor, // RAG 检索增强
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // 聊天记忆
                )
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks()) // 设置工具回调
                .build();
    }

    /**
     * 向量检索器
     */
    @Bean(name = "vectorStoreDocumentRetriever")
    public VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore)  {
        return VectorStoreDocumentRetriever.builder()
                .topK(3)
                .vectorStore(vectorStore)
                .similarityThreshold(0.8)
                .build();
    }

    /**
     *  检索增强 RAG 顾问
     */
    @Bean(name = "retrievalAugmentationAdvisor")
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .build();
    }

}
