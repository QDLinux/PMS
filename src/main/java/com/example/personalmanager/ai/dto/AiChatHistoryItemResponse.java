package com.example.personalmanager.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话历史记录条目 DTO，表示一条历史消息（用户或助手）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistoryItemResponse {

    // 消息角色：user(用户) 或 assistant(助手)
    private String role;
    // 消息文本内容
    private String text;
    // 消息时间，格式 yyyy-MM-dd HH:mm:ss
    private String time;
}

