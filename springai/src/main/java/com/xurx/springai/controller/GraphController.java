package com.xurx.springai.controller;

import com.xurx.springai.service.IntentGraphService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/graph")
@AllArgsConstructor
public class GraphController {

    private final IntentGraphService intentGraphService;

    @GetMapping("/test")
    public String test(@RequestParam(defaultValue = "你好") String message,
                       @RequestParam(defaultValue = "qwen") String model) {
        return intentGraphService.execute(message, model);
    }
}
