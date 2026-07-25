package com.example.personalmanager.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话响应 DTO，封装大模型返回给前端的回答内容及相关元信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    // AI 生成的回答文本
    private String answer;
    // 实际使用的模型标识
    private String model;
    // 所属会话 ID
    private String sessionId;
}
