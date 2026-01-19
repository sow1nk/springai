package com.xurx.springai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.features", name = "rag-controller-enabled", havingValue = "true")
public class RagController {

    private final VectorStore vectorStore;

    @PostMapping("/importData")
    public String importData(String data) {
        Document document = Document.builder().text(data).build();
        vectorStore.add(List.of(document));
        return "Data imported successfully";
    }
}
