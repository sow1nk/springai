package com.xurx.springai.controller;

import com.xurx.springai.dto.ChatRequest;
import com.xurx.springai.entity.ChatRecord;
import com.xurx.springai.service.ChatRecordService;
import com.xurx.springai.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * GET 方式聊天接口，返回流式响应
     * @param
     * @return
     */
//    @GetMapping(produces = "text/html;charset=utf-8")
//    public Flux<String> chatGet(String prompt) {
//        log.info("Received GET request with prompt: {}", prompt);
//        return deepseekChatClient.prompt()
//                .user(prompt)
//                .stream()
//                .content();
//    }

    @PostMapping(produces = "text/html;charset=utf-8")
    public String chatPost(@RequestBody ChatRequest chatRequest) {
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

        // 模型路由选择
        ChatClient chatClient = chatClientMap.get(model);
        if (chatClient == null) {
            log.error("模型 {} 未找到，使用默认模型", model);
            return "模型未找到，请检查模型名称。";
        } else {
            String finalSessionId = sessionId;
            log.info("使用模型 {} 处理请求，sessionId={}", model, finalSessionId);
            String response = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                    .system(p -> p.param("name", "xrx").param("identity", "postgraduate"))
                    .user(message)
                    .call()
                    .content();

            // 保存AI响应
            if (userId != null && !userId.isEmpty()) {
                chatRecordService.saveChatRecord(userId, sessionId, "assistant", response, model, null);
            }

            return response;
        }
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

}
