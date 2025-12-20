package com.xurx.springai.controller;

import com.xurx.springai.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final VectorStore vectorStore;
    private final Map<String, ChatClient> chatClientMap;


    @PostMapping("/importData")
    public String importData() {
        try {
            ClassPathResource resource = new ClassPathResource("QA.csv");
            InputStreamReader reader = new InputStreamReader(resource.getInputStream(), "UTF-8");

            // 通过Apache Commons CSV 解析csv文件，忽略BOM字符
            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);
            List<Document> documents = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                // 使用列索引而不是列名，避免BOM字符问题
                String question = record.get(0);  // 第一列：问题
                String answer = record.get(1);     // 第二列：回答

                String content = "问题：" + question + "\n回答：" + answer;
                Document document = new Document(content);
                documents.add(document);
            }

            csvParser.close();

            vectorStore.add(documents);

            return "成功导入 " + documents.size() + " 条菜谱问答数据。";

        } catch (IOException e) {
            e.printStackTrace();
            return "导入数据时发生错误：" + e.getMessage();
        }
    }

    @PostMapping("/askQuestion")
    public String ragAskQuestion(ChatRequest chatRequest) {
        String model = chatRequest.getModel();
        String message = chatRequest.getMessage();
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

    @PostMapping("/search")
    public List<Document> search(@RequestParam("query") String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(10)
                .query(query)
                .similarityThreshold(0.8)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents;
    }
}
