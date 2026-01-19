package com.xurx.springai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文档处理服务
 * 负责文档解析、分块和向量化存储
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final VectorStore vectorStore;
    private final Tika tika = new Tika();

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;
    private static final int MIN_CHUNK_SIZE = 100;
    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int MIN_CHUNK_OVERLAP = 0;
    private static final int MAX_CHUNK_OVERLAP = 500;

    // 支持的文档类型
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "application/vnd.ms-powerpoint", // .ppt
            "text/plain",
            "text/markdown",
            "text/html"
    );

    // 文档上传目录
    private static final String UPLOAD_DIR = "uploads/documents";

    private record ChunkParameters(int chunkSize, int chunkOverlap) {
    }

    /**
     * 校验分块参数并返回有效值
     */
    private ChunkParameters resolveChunkParameters(Integer chunkSize, Integer chunkOverlap) {
        int resolvedChunkSize = chunkSize != null ? chunkSize : DEFAULT_CHUNK_SIZE;
        int resolvedChunkOverlap = chunkOverlap != null ? chunkOverlap : DEFAULT_CHUNK_OVERLAP;

        if (resolvedChunkSize < MIN_CHUNK_SIZE || resolvedChunkSize > MAX_CHUNK_SIZE) {
            throw new IllegalArgumentException(String.format(
                    "分块大小必须在 %d - %d 之间", MIN_CHUNK_SIZE, MAX_CHUNK_SIZE));
        }

        if (resolvedChunkOverlap < MIN_CHUNK_OVERLAP || resolvedChunkOverlap > MAX_CHUNK_OVERLAP) {
            throw new IllegalArgumentException(String.format(
                    "分块重叠必须在 %d - %d 之间", MIN_CHUNK_OVERLAP, MAX_CHUNK_OVERLAP));
        }

        if (resolvedChunkOverlap >= resolvedChunkSize) {
            throw new IllegalArgumentException("分块重叠必须小于分块大小");
        }

        return new ChunkParameters(resolvedChunkSize, resolvedChunkOverlap);
    }

    /**
     * 处理上传的文档文件并导入到向量数据库
     * 流程: 文件验证 -> 本地保存 -> PDF解析 -> 文本分块 -> 元数据丰富 -> 向量化存储到Redis
     */
    public Map<String, Object> processAndImportDocument(
            MultipartFile file,
            String userId,
            Integer chunkSize,
            Integer chunkOverlap
    ) throws IOException {
        log.info("开始处理文档上传 - 文件: {}, 用户: {}, 分块参数: {}/{}",
                file.getOriginalFilename(), userId, chunkSize, chunkOverlap);

        validateUserId(userId);

        // 1. 验证文件（类型、大小）
        validateFile(file);

        // 2. 保存文件到本地磁盘
        Path savedFilePath = saveFile(file, userId);
        log.info("文件已保存到: {}", savedFilePath.toAbsolutePath());

        try {
            // 3. 使用TikaDocumentReader解析PDF文档，提取文本内容
            log.info("开始解析PDF文档...");
            List<Document> documents = parseDocument(savedFilePath, file.getOriginalFilename());

            if (documents == null || documents.isEmpty()) {
                throw new RuntimeException("PDF解析失败：未提取到任何文本内容");
            }

            // 打印解析后的文档内容摘要（用于调试）
            for (int i = 0; i < Math.min(documents.size(), 3); i++) {
                Document doc = documents.get(i);
                String preview = doc.getText().substring(0, Math.min(200, doc.getText().length()));
                log.debug("文档片段 {} 预览: {}...", i, preview);
            }

            // 4. 使用TokenTextSplitter进行文本分块
            ChunkParameters chunkParameters = resolveChunkParameters(chunkSize, chunkOverlap);
            int effectiveChunkSize = chunkParameters.chunkSize();
            int effectiveChunkOverlap = chunkParameters.chunkOverlap();

            log.info("开始文本分块 - chunkSize: {}, chunkOverlap: {}", effectiveChunkSize, effectiveChunkOverlap);
            List<Document> chunkedDocuments = splitDocuments(
                    documents,
                    effectiveChunkSize,
                    effectiveChunkOverlap
            );

            if (chunkedDocuments.isEmpty()) {
                throw new RuntimeException("文档分块失败：分块结果为空");
            }

            // 5. 为每个文档块添加元数据（用户ID、文件名等）
            log.info("添加元数据到 {} 个文档块...", chunkedDocuments.size());
            enrichMetadata(chunkedDocuments, file, userId, savedFilePath);

            // 6. 将文档块向量化并存储到Redis Vector Store
            log.info("开始向量化并存储到Redis...");
            vectorStore.add(chunkedDocuments);
            log.info("成功将 {} 个文档块存储到Redis向量数据库", chunkedDocuments.size());

            // 7. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("chunkCount", chunkedDocuments.size());
            result.put("filePath", savedFilePath.toString());
            result.put("uploadTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            result.put("message", String.format("成功导入文档，生成 %d 个向量块", chunkedDocuments.size()));

            log.info("文档导入完成 - 文件: {}, 分块数: {}", file.getOriginalFilename(), chunkedDocuments.size());
            return result;

        } catch (Exception e) {
            // 处理失败时删除已保存的文件
            log.error("文档处理失败，删除已保存文件: {}", savedFilePath, e);
            Files.deleteIfExists(savedFilePath);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证上传的文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 检测文件类型
        String mimeType = tika.detect(file.getBytes());
        log.info("检测到文件类型: {} for file: {}", mimeType, file.getOriginalFilename());

        if (!SUPPORTED_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "不支持的文件类型: " + mimeType + "。支持的类型: PDF, Word, PPT, TXT, Markdown, HTML"
            );
        }

        // 文件大小限制（50MB）
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小超过限制（最大50MB）");
        }
    }

    /**
     * 保存文件到本地
     */
    private Path saveFile(MultipartFile file, String userId) throws IOException {
        // 创建上传目录
        Path uploadPath = getUserUploadPath(userId);
        Files.createDirectories(uploadPath);

        // 生成唯一文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String fileName = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        log.info("文件已保存: {}", filePath);
        return filePath;
    }

    /**
     * 解析文档（支持PDF、Word、PPT等）
     * 使用Apache Tika提取文本内容并转换为Document对象
     */
    private List<Document> parseDocument(Path filePath, String fileName) {
        log.info("开始解析文档: {}, 路径: {}", fileName, filePath.toAbsolutePath());

        try {
            // 使用 TikaDocumentReader 解析文档
            // TikaDocumentReader 支持 PDF、Word、PPT、TXT、Markdown、HTML等多种格式
            TikaDocumentReader reader = new TikaDocumentReader(
                    new org.springframework.core.io.FileSystemResource(filePath)
            );

            // 执行解析，获取Document列表
            List<Document> documents = reader.get();

            if (documents == null || documents.isEmpty()) {
                log.warn("文档解析完成但未提取到内容: {}", fileName);
                return Collections.emptyList();
            }

            // 统计总字符数
            int totalChars = documents.stream()
                    .mapToInt(doc -> doc.getText().length())
                    .sum();

            log.info("文档解析成功 - 文件: {}, 提取到 {} 个文档对象, 总字符数: {}",
                    fileName, documents.size(), totalChars);

            // 打印第一个文档片段的前200个字符（用于调试）
            if (!documents.isEmpty()) {
                String firstContent = documents.get(0).getText();
                String preview = firstContent.substring(0, Math.min(200, firstContent.length()));
                log.debug("文档内容预览: {}...", preview.replaceAll("\\s+", " "));
            }

            return documents;

        } catch (Exception e) {
            log.error("文档解析失败: {}, 错误: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("无法解析文档 " + fileName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 分块处理文档
     * 使用TokenTextSplitter将长文本分割成适合向量化的小块
     */
    private List<Document> splitDocuments(List<Document> documents, int chunkSize, int chunkOverlap) {
        log.info("开始文档分块 - chunkSize: {}, chunkOverlap: {}, 原始文档数: {}",
                chunkSize, chunkOverlap, documents.size());

        try {
            // TokenTextSplitter参数说明:
            // - chunkSize: 每个块的目标token数量
            // - chunkOverlap: 相邻块之间的重叠token数量
            // - minChunkSizeChars: 最小块字符数（5）
            // - minChunkLengthToEmbed: 最小嵌入块长度（10000）
            // - maxNumChunks: 是否限制最大块数（true = 不限制）
            TokenTextSplitter splitter = new TokenTextSplitter(
                    chunkSize,
                    chunkOverlap,
                    5,      // minChunkSizeChars
                    10000,  // minChunkLengthToEmbed
                    true    // maxNumChunks
            );

            // 执行分块
            List<Document> chunkedDocuments = splitter.apply(documents);

            if (chunkedDocuments == null || chunkedDocuments.isEmpty()) {
                log.warn("分块结果为空，可能是文档内容太短");
                return Collections.emptyList();
            }

            // 统计分块后的信息
            int totalChars = chunkedDocuments.stream()
                    .mapToInt(doc -> doc.getText().length())
                    .sum();
            double avgChunkSize = chunkedDocuments.stream()
                    .mapToInt(doc -> doc.getText().length())
                    .average()
                    .orElse(0.0);

            log.info("文档分块完成 - 总分块数: {}, 总字符数: {}, 平均块大小: {} 字符",
                    chunkedDocuments.size(), totalChars, (int) avgChunkSize);

            return chunkedDocuments;

        } catch (Exception e) {
            log.error("文档分块失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档分块处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 丰富文档元数据
     * 为每个文档块添加用户ID、文件名、上传时间等信息，便于后续检索和管理
     */
    private void enrichMetadata(List<Document> documents,
                                MultipartFile file,
                                String userId,
                                Path savedFilePath) {
        String timestamp = LocalDateTime.now().toString();
        String fileName = file.getOriginalFilename();
        String storageFolder = sanitizeForFilesystem(userId);
        String storedFileName = savedFilePath.getFileName().toString();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> metadata = doc.getMetadata();

            // 基本信息
            metadata.put("userId", userId);
            metadata.put("storageFolder", storageFolder);
            metadata.put("fileName", fileName);
            metadata.put("fileSize", file.getSize());
            metadata.put("uploadTime", timestamp);

            // 分块信息
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", documents.size());

            // 文件路径
            metadata.put("filePath", savedFilePath.toString());
            metadata.put("storedFileName", storedFileName);

            // 文档类型（用于前端显示）
            metadata.put("fileType", getFileExtension(fileName));

            log.debug("文档块 {}/{} 元数据已添加", i + 1, documents.size());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }

    private Path getUserUploadPath(String userId) {
        return Paths.get(UPLOAD_DIR, sanitizeForFilesystem(userId));
    }

    private String sanitizeForFilesystem(String userId) {
        return userId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 删除用户的文档
     */
    public boolean deleteDocument(String userId, String fileName) throws IOException {
        validateUserId(userId);
        Path filePath = getUserUploadPath(userId).resolve(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("删除文档: {}", filePath);
            return true;
        }
        return false;
    }

    /**
     * 获取用户上传的文档列表
     */
    public List<Map<String, Object>> listUserDocuments(String userId) throws IOException {
        validateUserId(userId);
        Path userUploadPath = getUserUploadPath(userId);
        if (!Files.exists(userUploadPath)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> documents = new ArrayList<>();
        try (var stream = Files.list(userUploadPath)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Map<String, Object> docInfo = new HashMap<>();
                    docInfo.put("fileName", file.getFileName().toString());
                    docInfo.put("fileSize", Files.size(file));
                    docInfo.put("uploadTime", Files.getLastModifiedTime(file).toString());
                    documents.add(docInfo);
                } catch (IOException e) {
                    log.error("读取文件信息失败: {}", file.getFileName(), e);
                }
            });
        }

        return documents;
    }
}
