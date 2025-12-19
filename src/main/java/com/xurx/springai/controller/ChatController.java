package com.xurx.springai.controller;

import com.xurx.springai.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RequestMapping("/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    // 模型路由
    private final Map<String, ChatClient> chatClientMap;

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
        log.info("请求参数 message: {}  model: {}", message, model);
        // 模型路由选择
        ChatClient chatClient = chatClientMap.get(model);
        if (chatClient == null) {
            log.error("模型 {} 未找到，使用默认模型", model);
            return "模型未找到，请检查模型名称。";
        } else {
            return chatClient.prompt()
                    .system(p -> p.param("name", "xrx").param("identity", "postgraduate"))
                    .user(message)
                    .call()
                    .content();
        }
    }
}
