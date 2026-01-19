package com.xurx.springai.controller;

import com.xurx.springai.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理控制器
 * 处理文档上传、列表查询、删除等操作
 */
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    private final VectorStore vectorStore;

    /**
     * 上传并导入文档到RAG向量数据库
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @param chunkSize 分块大小（可选，默认800）
     * @param chunkOverlap 分块重叠大小（可选，默认200）
     * @return 导入结果
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap
    ) {
        try {
            log.info("接收文档上传请求 - 文件名: {}, 用户: {}, 分块大小: {}, 重叠: {}",
                    file.getOriginalFilename(), userId, chunkSize, chunkOverlap);

            Map<String, Object> result = documentService.processAndImportDocument(
                    file, userId, chunkSize, chunkOverlap
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("文档验证失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (IOException e) {
            log.error("文档处理IO异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "文件处理失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);

        } catch (Exception e) {
            log.error("文档处理异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取用户上传的文档列表
     *
     * @param userId 用户ID
     * @return 文档列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDocuments(
            @RequestParam("userId") String userId
    ) {
        try {
            log.info("获取文档列表请求 - 用户: {}", userId);

            List<Map<String, Object>> documents = documentService.listUserDocuments(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("count", documents.size());
            response.put("documents", documents);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("获取文档列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "获取文档列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除用户上传的文档
     *
     * @param userId 用户ID
     * @param fileName 文件名
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @RequestParam("userId") String userId,
            @RequestParam("fileName") String fileName
    ) {
        try {
            log.info("删除文档请求 - 用户: {}, 文件: {}", userId, fileName);

            boolean deleted = documentService.deleteDocument(userId, fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("message", deleted ? "文档删除成功" : "文档不存在");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("删除文档失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "删除文档失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "Document Service");
        return ResponseEntity.ok(response);
    }
}
