package com.xurx.springai.controller;

import com.xurx.springai.advisor.MetricAdvisor;
import com.xurx.springai.dto.ChatRequest;
import com.xurx.springai.entity.ChatRecord;
import com.xurx.springai.service.ChatRecordService;
import com.xurx.springai.service.IntentGraphService;
import com.xurx.springai.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequestMapping("/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    // 模型路由
    private final Map<String, ChatClient> chatClientMap;
    private final UserService userService;
    private final ChatRecordService chatRecordService;
    private final IntentGraphService intentGraphService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatPost(@RequestBody ChatRequest chatRequest) {
        String message = chatRequest.getMessage();
        String model = chatRequest.getModel();
        String userId = chatRequest.getUserId();
        String sessionId = chatRequest.getSessionId();

        log.info("请求参数 message: {}  model: {}  userId: {}  sessionId: {}", message, model, userId, sessionId);

        // 确保用户存在
        if (userId != null && !userId.isEmpty()) {
            userService.getOrCreateUser(userId);
        }

        // 如果没有提供sessionId，则生成新的会话ID
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            log.info("生成新的会话ID: {}", sessionId);
        }

        // 保存用户消息
        if (userId != null && !userId.isEmpty()) {
            chatRecordService.saveChatRecord(userId, sessionId, "user", message, model, null);
        }

        // 检查模型是否存在
        if (!chatClientMap.containsKey(model)) {
            log.error("模型 {} 未找到", model);
            return Flux.just(ServerSentEvent.<String>builder()
                .event("response")
                .data("{\"content\":\"模型未找到，请检查模型名称。\"}")
                .build());
        }

        String finalSessionId = sessionId;
        String finalUserId = userId;
        ObjectMapper mapper = new ObjectMapper();

        // 通过 Graph 编排执行意图识别 + 条件路由（SSE 分步推送）
        StringBuilder responseAccumulator = new StringBuilder();

        return intentGraphService.executeWithSteps(message, model)
            .doOnNext(sse -> {
                if ("token".equals(sse.event())) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, String> data = mapper.readValue(sse.data(), Map.class);
                        String tokenContent = data.get("content");
                        if (tokenContent != null) {
                            responseAccumulator.append(tokenContent);
                        }
                    } catch (Exception e) {
                        log.error("解析 token 事件失败", e);
                    }
                }
            })
            .doOnComplete(() -> {
                String fullResponse = responseAccumulator.toString();
                if (!fullResponse.isEmpty() && finalUserId != null && !finalUserId.isEmpty()) {
                    chatRecordService.saveChatRecord(finalUserId, finalSessionId, "assistant", fullResponse, model, null);
                }
            });
    }

    /**
     * 获取用户的聊天记录
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatRecord>> getChatHistory(@RequestParam String userId) {
        log.info("获取用户聊天记录: userId={}", userId);
        List<ChatRecord> records = chatRecordService.getChatRecordsByUserId(userId);
        log.info("找到 {} 条聊天记录", records.size());
        return ResponseEntity.ok(records);
    }

    /**
     * 获取指定会话的聊天记录
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatRecord>> getChatHistoryBySession(
            @PathVariable String sessionId,
            @RequestParam String userId) {
        log.info("获取会话聊天记录: userId={}, sessionId={}", userId, sessionId);
        List<ChatRecord> records = chatRecordService.getChatRecordsByUserIdAndSessionId(userId, sessionId);
        return ResponseEntity.ok(records);
    }

    /**
     * 删除指定会话的聊天记录
     */
    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String sessionId) {
        log.info("删除会话聊天记录: sessionId={}", sessionId);
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        chatRecordService.deleteChatRecordsBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 统计 token 使用情况接口
     */
    @PostMapping("/chatWithMetric")
    public Map<String, Object> chatWithMetric(@RequestBody ChatRequest chatRequest) {
        String prompt = chatRequest.getMessage();
        String model = chatRequest.getModel();
        log.info("统计 token 使用情况接口: prompt={}", prompt);

        MetricAdvisor metricAdvisor = new MetricAdvisor();
        ChatClient chatClient = chatClientMap.get(model);
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(metricAdvisor)
                .user(prompt)
                .call()
                .chatResponse();
        Usage usage = chatResponse.getMetadata().getUsage();

        return Map.of(
                "content", chatResponse.getResult().getOutput().getText(),
                "promptTokens", usage.getPromptTokens(),
                "completionTokens", usage.getCompletionTokens(),
                "totalTokens", usage.getTotalTokens()
        );
    }
}
