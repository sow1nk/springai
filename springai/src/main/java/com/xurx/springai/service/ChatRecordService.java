package com.xurx.springai.service;

import com.xurx.springai.entity.ChatRecord;
import com.xurx.springai.mapper.ChatRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecordService {

    private final ChatRecordMapper chatRecordMapper;

    /**
     * 保存聊天记录
     */
    public void saveChatRecord(String userId, String sessionId, String role, String content, String model, Integer tokens) {
        ChatRecord chatRecord = new ChatRecord();
        chatRecord.setUserId(userId);
        chatRecord.setSessionId(sessionId);
        chatRecord.setRole(role);
        chatRecord.setContent(content);
        chatRecord.setModel(model);
        chatRecord.setTokens(tokens);
        chatRecordMapper.insert(chatRecord);
        log.debug("保存聊天记录: userId={}, sessionId={}, role={}", userId, sessionId, role);
    }

    /**
     * 根据userId查询聊天记录
     */
    public List<ChatRecord> getChatRecordsByUserId(String userId) {
        return chatRecordMapper.selectByUserId(userId);
    }

    /**
     * 根据sessionId查询聊天记录
     */
    public List<ChatRecord> getChatRecordsBySessionId(String sessionId) {
        return chatRecordMapper.selectBySessionId(sessionId);
    }

    /**
     * 根据userId和sessionId查询聊天记录
     */
    public List<ChatRecord> getChatRecordsByUserIdAndSessionId(String userId, String sessionId) {
        return chatRecordMapper.selectByUserIdAndSessionId(userId, sessionId);
    }

    /**
     * 删除指定会话的聊天记录
     */
    public int deleteChatRecordsBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("删除聊天记录失败，sessionId为空");
            return 0;
        }
        int deleted = chatRecordMapper.deleteBySessionId(sessionId);
        log.debug("删除聊天记录: sessionId={}, 删除条数={}", sessionId, deleted);
        return deleted;
    }

    /**
     * 生成新的会话ID
     */
    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
