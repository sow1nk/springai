package com.xurx.springai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatRecord {
    private Long id;
    private String userId;
    private String sessionId;
    private String role;
    private String content;
    private String model;
    private Integer tokens;
    private LocalDateTime createTime;
}
