package com.xurx.springai.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RequestMapping("/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping(produces = "text/html;charset=utf-8")
    public Flux<String> chatGet(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @PostMapping(produces = "text/html;charset=utf-8")
    public Flux<String> chatPost(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }
}
