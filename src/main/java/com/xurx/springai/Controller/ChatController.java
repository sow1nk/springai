package com.xurx.springai.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RequestMapping("/chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    @GetMapping(produces = "text/html;charset=utf-8")
    public Flux<String> chatGet(String prompt) {
        log.info("Received GET request with prompt: {}", prompt);
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @PostMapping(produces = "text/html;charset=utf-8")
    public Flux<String> chatPost(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        log.info("Received POST request with prompt: {}", prompt);
        return chatClient.prompt()
                .system(p -> p.param("name", "xrx").param("identity", "postgraduate"))
                .user(prompt)
                .stream()
                .content();
    }
}
