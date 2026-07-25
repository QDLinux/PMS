package com.example.personalmanager.ai.controller;

import com.example.personalmanager.ai.dto.AiChatRequest;
import com.example.personalmanager.ai.dto.AiChatResponse;
import com.example.personalmanager.ai.dto.AiChatHistoryItemResponse;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.ai.service.AiAssistantService;
import com.example.personalmanager.ai.service.AiChatLogService;
import com.example.personalmanager.ai.dto.AiChatSessionResponse;
import com.example.personalmanager.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * AI 智能助手控制器，提供对话、历史记录查询及会话管理的 REST 接口。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiChatLogService aiChatLogService;
    private final CurrentUserService currentUserService;

    /**
     * 发起一次 AI 对话。
     *
     * @param request 对话请求，包含消息内容、会话 ID 及可选上下文
     * @return 大模型生成的回答
     */
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String username = currentUserService.getCurrentUsername();
        AiChatResponse response = aiAssistantService.chat(username, request.getSessionId(), request.getMessage(), request.getContext());
        return ApiResponse.ok(response);
    }

    /**
     * 查询当前用户的对话历史记录。
     *
     * @param sessionId 会话 ID，为空时查询全部
     * @param limit     返回的最大条数，默认 100
     * @return 历史消息列表
     */
    @GetMapping("/history")
    public ApiResponse<List<AiChatHistoryItemResponse>> history(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        String username = currentUserService.getCurrentUsername();
        return ApiResponse.ok(aiChatLogService.listHistory(username, sessionId, limit));
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param limit 返回的最大会话数，默认 50
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionResponse>> sessions(@RequestParam(defaultValue = "50") int limit) {
        String username = currentUserService.getCurrentUsername();
        return ApiResponse.ok(aiChatLogService.listSessions(username, limit));
    }

    /**
     * 创建一个新的对话会话。
     *
     * @param title 会话标题，可选
     * @return 新建的会话信息
     */
    @PutMapping("/sessions/new")
    public ApiResponse<AiChatSessionResponse> createSession(@RequestParam(required = false) String title) {
        String username = currentUserService.getCurrentUsername();
        return ApiResponse.ok(aiChatLogService.createSession(username, title));
    }

    /**
     * 删除指定会话及其关联的历史记录。
     *
     * @param sessionId 待删除的会话 ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        String username = currentUserService.getCurrentUsername();
        aiChatLogService.deleteSession(username, sessionId);
        return ApiResponse.ok(null);
    }
}
