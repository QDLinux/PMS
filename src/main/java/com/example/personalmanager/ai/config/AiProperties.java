package com.example.personalmanager.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置属性，绑定 application 配置中以 app.ai 为前缀的参数，
 * 用于配置大模型调用的开关、服务地址、密钥、模型及相关运行参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Enable/disable real model calls.
     */
    // 是否启用真实的大模型调用
    private boolean enabled = false;

    /**
     * OpenAI-compatible base URL, e.g. https://api.openai.com
     */
    // 兼容 OpenAI 协议的服务基础地址
    private String baseUrl = "https://api.openai.com";

    /**
     * Provider API key.
     */
    // 大模型服务提供商的 API 密钥
    private String apiKey = "";

    /**
     * OpenAI-compatible model id, e.g. gpt-4o-mini / deepseek-chat.
     */
    // 兼容 OpenAI 协议的模型标识
    private String model = "gpt-4o-mini";

    // 采样温度，控制回答的随机性
    private double temperature = 0.5;
    // 单次回答的最大 token 数
    private int maxTokens = 800;
    // 调用大模型服务的超时时间（毫秒）
    private int timeoutMs = 30000;
    // 系统提示词，定义助手的角色与行为
    private String systemPrompt = "你是一个个人财务与事务管理助手。请基于用户给出的上下文，提供清晰、可执行、简洁的中文建议。";
    // AI 对话日志与历史记录的存储目录
    private String logDir = "logs/ai-chat";
}
