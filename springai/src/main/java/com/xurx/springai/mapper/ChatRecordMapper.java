package com.xurx.springai.mapper;

import com.xurx.springai.entity.ChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordMapper {

    /**
     * 插入聊天记录
     */
    int insert(ChatRecord chatRecord);

    /**
     * 根据userId查询聊天记录
     */
    List<ChatRecord> selectByUserId(@Param("userId") String userId);

    /**
     * 根据sessionId查询聊天记录
     */
    List<ChatRecord> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据userId和sessionId查询聊天记录
     */
    List<ChatRecord> selectByUserIdAndSessionId(@Param("userId") String userId, @Param("sessionId") String sessionId);

    /**
     * 删除指定用户的聊天记录
     */
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 删除指定会话的聊天记录
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
