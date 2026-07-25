package com.example.personalmanager.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话会话响应 DTO，描述一个多轮对话会话的基本信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatSessionResponse {

    // 会话唯一标识
    private String sessionId;
    // 会话标题（通常取自首条用户消息）
    private String title;
    // 会话最近更新时间，格式 yyyy-MM-dd HH:mm:ss
    private String updatedAt;
}

