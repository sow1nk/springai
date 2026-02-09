package com.xurx.springai.service;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

@Service
@Slf4j
public class IntentGraphService {

    private final Map<String, ChatClient> chatClientMap;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final ToolCallbackProvider mcpToolCallbackProvider;

    @Value("classpath:prompts/intent-classification.txt")
    private Resource intentPromptResource;

    private CompiledGraph intentGraph;

    private static final Set<String> VALID_INTENTS = Set.of("chitchat", "rag", "tool_call");

    public IntentGraphService(Map<String, ChatClient> chatClientMap,
                              RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                              ToolCallbackProvider mcpToolCallbackProvider) {
        this.chatClientMap = chatClientMap;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    @PostConstruct
    public void init() throws GraphStateException {
        // 定义键策略工厂，指定每个键的替换策略
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                "message", new ReplaceStrategy(),
                "model", new ReplaceStrategy(),
                "intent", new ReplaceStrategy(),
                "response", new ReplaceStrategy()
        );

        // 创建状态图
        StateGraph stateGraph = new StateGraph("intentGraph", keyStrategyFactory);

        // intent_recognition 节点：意图分类
        stateGraph.addNode("intent_recognition", node_async(state -> {
            String message = state.value("message", String.class).orElse("");
            String model = state.value("model", String.class).orElse("qwen");
            log.info("[意图识别] 用户输入={}, 模型={}", message, model);

            ChatClient chatClient = chatClientMap.get(model);

            PromptTemplate template = new PromptTemplate(intentPromptResource);
            String intentPrompt = template.render(Map.of("message", message));

            String intent = chatClient.prompt()
                    .user(intentPrompt)
                    .call()
                    .content()
                    .trim()
                    .toLowerCase();

            // 兜底逻辑：如果模型返回了无法识别的意图，则默认降级为闲聊
            if (!VALID_INTENTS.contains(intent)) {
                log.warn("[意图识别] 无法识别 '{}'，降级为闲聊", intent);
                intent = "chitchat";
            }

            log.info("[意图识别] 分类结果：{}", intent);
            return Map.of("intent", intent);
        }));

        // chitchat 节点：闲聊
        stateGraph.addNode("chitchat", node_async(state -> {
            String message = state.value("message", String.class).orElse("");
            String model = state.value("model", String.class).orElse("qwen");
            log.info("[闲聊] 处理消息：{}", message);

            ChatClient chatClient = chatClientMap.get(model);
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            log.info("[闲聊] 回复已生成");
            return Map.of("response", response);
        }));

        // rag 节点：知识库检索
        stateGraph.addNode("rag", node_async(state -> {
            String message = state.value("message", String.class).orElse("");
            String model = state.value("model", String.class).orElse("qwen");
            log.info("[知识库检索] 处理消息：{}", message);

            ChatClient chatClient = chatClientMap.get(model);
            String response = chatClient.prompt()
                    .advisors(retrievalAugmentationAdvisor)
                    .user(message)
                    .call()
                    .content();

            log.info("[知识库检索] 回复已生成");
            return Map.of("response", response);
        }));

        // tool_call 节点：工具调用
        stateGraph.addNode("tool_call", node_async(state -> {
            String message = state.value("message", String.class).orElse("");
            String model = state.value("model", String.class).orElse("qwen");
            log.info("[工具调用] 处理消息：{}", message);

            ChatClient chatClient = chatClientMap.get(model);
            String response = chatClient.prompt()
                    .toolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                    .user(message)
                    .call()
                    .content();

            log.info("[工具调用] 回复已生成");
            return Map.of("response", response);
        }));

        // 边定义
        stateGraph.addEdge(StateGraph.START, "intent_recognition");

        // 条件路由：根据 intent 分发到不同节点
        stateGraph.addConditionalEdges(
                "intent_recognition",
                edge_async(state -> {
                    String intent = state.value("intent", String.class).orElse("chitchat");
                    log.info("[路由] 意图={}", intent);
                    return intent;
                }),
                Map.of("chitchat", "chitchat", "rag", "rag", "tool_call", "tool_call")
        );

        stateGraph.addEdge("chitchat", StateGraph.END);
        stateGraph.addEdge("rag", StateGraph.END);
        stateGraph.addEdge("tool_call", StateGraph.END);

        this.intentGraph = stateGraph.compile();
        log.info("意图识别图编译完成");
    }

    /**
     * 执行意图识别图，返回最终响应结果
     */
    public String execute(String message, String model) {
        log.info("[IntentGraphService] execute: message={}, model={}", message, model);
        try {
            Optional<OverAllState> result = intentGraph.invoke(Map.of("message", message, "model", model));
            if (result.isPresent()) {
                return result.get().value("response", String.class).orElse("抱歉，处理请求时出现问题。");
            }
            return "抱歉，处理请求时出现问题。";
        } catch (Exception e) {
            log.error("[IntentGraphService] 图执行异常", e);
            return "抱歉，处理请求时发生错误：" + e.getMessage();
        }
    }

    /**
     * 执行意图识别图，返回分步骤的 SSE 流（包括步骤事件、流式 token 和完成事件）
     */
    public Flux<ServerSentEvent<String>> executeWithSteps(String message, String model) {
        ObjectMapper mapper = new ObjectMapper();
        ChatClient chatClient = chatClientMap.get(model);

        log.info("[executeWithSteps] 开始处理, message={}, model={}", message, model);

        // Phase 1: 立即发出"正在识别意图"
        Flux<ServerSentEvent<String>> phase1 = Flux.just(
                buildStepEvent(mapper, "intent_recognition", "processing", null, "正在识别意图...")
        );

        // Phase 2: 意图识别(阻塞) → 步骤事件 → 流式 token → done
        Flux<ServerSentEvent<String>> phase2 = Mono.fromCallable(() -> {
                    log.info("[意图识别] 用户输入={}, 模型={}", message, model);
                    PromptTemplate template = new PromptTemplate(intentPromptResource);
                    String intentPrompt = template.render(Map.of("message", message));
                    String intent = chatClient.prompt()
                            .user(intentPrompt)
                            .call()
                            .content()
                            .trim()
                            .toLowerCase();
                    if (!VALID_INTENTS.contains(intent)) {
                        log.warn("[意图识别] 无法识别 '{}'，降级为闲聊", intent);
                        intent = "chitchat";
                    }
                    log.info("[意图识别] 分类结果：{}", intent);
                    return intent;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(intent -> {
                    String intentLabel = getIntentLabel(intent);
                    log.info("[{}] 处理消息：{}", intentLabel, message);

                    Flux<ServerSentEvent<String>> stepEvents = Flux.just(
                            buildStepEvent(mapper, "intent_recognition", "complete", intent, "意图识别完成：" + intentLabel),
                            buildStepEvent(mapper, intent, "processing", null, "正在" + intentLabel + "...")
                    );

                    Flux<String> tokenStream = switch (intent) {
                        case "rag" -> chatClient.prompt()
                                .advisors(retrievalAugmentationAdvisor)
                                .user(message).stream().content();
                        case "tool_call" -> chatClient.prompt()
                                .toolCallbacks(mcpToolCallbackProvider.getToolCallbacks())
                                .user(message).stream().content();
                        default -> chatClient.prompt()
                                .user(message).stream().content();
                    };

                    Flux<ServerSentEvent<String>> tokenEvents = tokenStream.map(token -> {
                        try {
                            return ServerSentEvent.<String>builder()
                                    .event("token")
                                    .data(mapper.writeValueAsString(Map.of("content", token)))
                                    .build();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                            ServerSentEvent.<String>builder().event("done").data("{}").build()
                    );

                    return Flux.concat(stepEvents, tokenEvents, doneEvent);
                });

        return Flux.concat(phase1, phase2)
                .doOnError(e -> log.error("[executeWithSteps] 流式处理异常", e))
                .onErrorResume(e -> {
                    try {
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data(mapper.writeValueAsString(Map.of("message", "处理请求时发生错误：" + e.getMessage())))
                                .build());
                    } catch (Exception ex) {
                        return Flux.empty();
                    }
                });
    }

    private ServerSentEvent<String> buildStepEvent(ObjectMapper mapper, String node, String status, String intent, String message) {
        try {
            Map<String, Object> data = new HashMap<>(Map.of("node", node, "status", status, "message", message));
            if (intent != null) data.put("intent", intent);
            return ServerSentEvent.<String>builder()
                    .event("step")
                    .data(mapper.writeValueAsString(data))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getIntentLabel(String intent) {
        return switch (intent) {
            case "chitchat" -> "闲聊";
            case "rag" -> "知识库检索";
            case "tool_call" -> "工具调用";
            default -> "处理";
        };
    }
}
