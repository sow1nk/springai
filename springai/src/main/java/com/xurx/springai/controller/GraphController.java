package com.xurx.springai.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/graph")
@AllArgsConstructor
public class GraphController {

    private CompiledGraph quickGraph;

    @GetMapping("/quick")
    public String quickTest() {
        quickGraph.invoke(Map.of());
        return "ok";
    }
}
