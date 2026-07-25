package com.example.personalmanager.ai.service;

import com.example.personalmanager.ai.config.AiProperties;
import com.example.personalmanager.ai.dto.AiChatResponse;
import com.example.personalmanager.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 智能助手核心服务，负责构造提示词、调用兼容 OpenAI 协议的大模型（DeepSeek），
 * 并处理多轮的工具（Function Calling）调用循环，最终返回助手回答。
 */
@Slf4j
@Service
public class AiAssistantService {

    // 工具调用的最大轮次，防止模型陷入无限的工具调用循环
    private static final int MAX_TOOL_ROUNDS = 5;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final AiChatLogService aiChatLogService;
    private final AiToolExecutor toolExecutor;

    public AiAssistantService(AiProperties aiProperties,
                              ObjectMapper objectMapper,
                              AiChatLogService aiChatLogService,
                              AiToolExecutor toolExecutor) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.aiChatLogService = aiChatLogService;
        this.toolExecutor = toolExecutor;
    }

    /**
     * 处理一次完整的 AI 对话：构造消息、循环调用大模型并执行工具，直至获得最终回答。
     *
     * @param username  当前用户名
     * @param sessionId 会话 ID，为空时自动创建新会话
     * @param message   用户输入的消息
     * @param context   前端提供的上下文数据，可为空
     * @return 包含回答、模型及会话 ID 的响应
     */
    public AiChatResponse chat(String username, String sessionId, String message, String context) {
        validateConfig();
        // 确保会话存在（不存在则创建），返回有效会话 ID
        String sid = aiChatLogService.ensureSession(username, sessionId, message);

        // 构造对话消息列表：系统提示词（含当前日期） + 用户消息
        List<Map<String, Object>> messages = new ArrayList<>();
        String dateNote = "当前日期：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")) + "。";
        messages.add(Map.of("role", "system", "content", dateNote + " " + aiProperties.getSystemPrompt()));
        messages.add(Map.of("role", "user", "content", buildUserPrompt(message, context)));

        String finalAnswer = null;
        // 多轮工具调用循环：模型可能多次请求工具，直到给出最终文本回答
        for (int turn = 0; turn < MAX_TOOL_ROUNDS; turn++) {
            Map<String, Object> payload = buildPayload(messages);
            JsonNode root = callDeepSeek(payload);

            JsonNode choice = root.path("choices").get(0);
            if (choice == null) {
                throw new BusinessException("AI 服务返回格式异常：缺少 choices");
            }
            JsonNode msgNode = choice.path("message");
            JsonNode toolCallsNode = msgNode.path("tool_calls");

            // 若模型返回了工具调用请求，则执行工具并将结果回填到消息列表后继续下一轮
            if (toolCallsNode.isArray() && toolCallsNode.size() > 0) {
                appendAssistantWithToolCalls(messages, msgNode, toolCallsNode);
                executeAndAppendToolResults(messages, toolCallsNode);
                continue;
            }

            // 模型未请求工具，得到最终回答，结束循环
            finalAnswer = msgNode.path("content").asText("");
            break;
        }

        if (finalAnswer == null) {
            throw new BusinessException("AI 工具调用超过最大轮次 (" + MAX_TOOL_ROUNDS + ")");
        }

        String answer = finalAnswer.trim();
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException("AI 服务返回内容为空");
        }

        AiChatResponse resp = AiChatResponse.builder()
                .answer(answer)
                .model(aiProperties.getModel())
                .sessionId(sid)
                .build();
        // 记录成功调用的日志与对话历史
        aiChatLogService.logSuccess(username, sid, message, context, answer, aiProperties.getModel());
        return resp;
    }

    /* ---- payload construction ---- */

    // 构造发送给大模型的请求体，包含模型、消息、可用工具及采样参数
    private Map<String, Object> buildPayload(List<Map<String, Object>> messages) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", aiProperties.getModel());
        payload.put("messages", messages);
        payload.put("tools", Tools.TOOLS);
        payload.put("temperature", aiProperties.getTemperature());
        payload.put("max_tokens", aiProperties.getMaxTokens());
        return payload;
    }

    /* ---- HTTP call ---- */

    // 通过 HTTP 调用兼容 OpenAI 协议的 chat/completions 接口并解析响应
    private JsonNode callDeepSeek(Map<String, Object> payload) {
        String bodyText;
        try {
            bodyText = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException("构建 AI 请求失败：" + e.getMessage());
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(aiProperties.getBaseUrl()) + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiProperties.getApiKey())
                .timeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(bodyText, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String raw = response.body();
            JsonNode root = objectMapper.readTree(raw);
            // 状态码 >= 400 时优先抛出服务返回的错误信息
            if (status >= 400) {
                String err = root.path("error").path("message").asText("");
                throw new BusinessException(StringUtils.hasText(err) ? err : ("AI 服务返回错误状态码：" + status));
            }
            return root;
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("调用 AI 服务失败：" + e.getMessage());
        }
    }

    /* ---- tool-call handling ---- */

    // 将模型返回的带工具调用的 assistant 消息原样追加到消息列表，保持上下文完整
    private void appendAssistantWithToolCalls(List<Map<String, Object>> messages,
                                              JsonNode msgNode,
                                              JsonNode toolCallsNode) {
        Map<String, Object> assistantMsg = new LinkedHashMap<>();
        assistantMsg.put("role", "assistant");

        String textContent = msgNode.path("content").asText(null);
        assistantMsg.put("content", textContent);

        List<Map<String, Object>> tcList = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            Map<String, Object> tcMap = new LinkedHashMap<>();
            tcMap.put("id", tc.path("id").asText());
            tcMap.put("type", "function");

            Map<String, Object> funcMap = new LinkedHashMap<>();
            funcMap.put("name", tc.path("function").path("name").asText());
            funcMap.put("arguments", tc.path("function").path("arguments").asText());
            tcMap.put("function", funcMap);

            tcList.add(tcMap);
        }
        assistantMsg.put("tool_calls", tcList);
        messages.add(assistantMsg);
    }

    // 逐个执行模型请求的工具调用，并将每个工具的执行结果以 tool 角色消息回填到消息列表
    private void executeAndAppendToolResults(List<Map<String, Object>> messages,
                                             JsonNode toolCallsNode) {
        for (JsonNode tc : toolCallsNode) {
            String fnName = tc.path("function").path("name").asText();
            String fnArgsJson = tc.path("function").path("arguments").asText();
            String callId = tc.path("id").asText();

            Map<String, Object> args;
            try {
                // 解析模型生成的工具参数 JSON 字符串
                args = objectMapper.readValue(fnArgsJson, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                // 参数解析失败时，回填错误信息让模型感知并自行纠正
                Map<String, Object> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", callId);
                toolMsg.put("content", "{\"error\":\"参数解析失败: " + e.getMessage() + "\"}");
                messages.add(toolMsg);
                continue;
            }

            String result = toolExecutor.execute(fnName, args);

            Map<String, Object> toolMsg = new LinkedHashMap<>();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", callId);
            toolMsg.put("content", result);
            messages.add(toolMsg);
        }
    }

    /* ---- helpers ---- */

    // 校验 AI 相关配置是否完整，缺失时抛出业务异常
    private void validateConfig() {
        if (!aiProperties.isEnabled()) {
            throw new BusinessException("AI 服务未启用，请设置 app.ai.enabled=true");
        }
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new BusinessException("缺少 AI API Key，请设置 app.ai.api-key");
        }
        if (!StringUtils.hasText(aiProperties.getBaseUrl())) {
            throw new BusinessException("缺少 AI 服务地址，请设置 app.ai.base-url");
        }
        if (!StringUtils.hasText(aiProperties.getModel())) {
            throw new BusinessException("缺少 AI 模型配置，请设置 app.ai.model");
        }
    }

    // 拼接用户提示词：有上下文时将其与用户问题组合，引导模型结合上下文作答
    private String buildUserPrompt(String message, String context) {
        String msg = StringUtils.hasText(message) ? message.trim() : "";
        if (!StringUtils.hasText(context)) {
            return msg;
        }
        return "以下为系统上下文数据，请结合这些信息回答用户问题。\n\n"
                + context.trim()
                + "\n\n用户问题：\n"
                + msg;
    }

    // 规范化服务基础地址，去除末尾多余的斜杠
    private String normalizeBaseUrl(String baseUrl) {
        String v = baseUrl.trim();
        if (v.endsWith("/")) {
            return v.substring(0, v.length() - 1);
        }
        return v;
    }

    /* ---- tool definitions (OpenAI-compatible schema) ---- */

    /**
     * 工具（Function Calling）定义集合，按 OpenAI 兼容的 schema 描述各可调用工具，
     * 供大模型在对话过程中按需选择调用。
     */
    private static class Tools {

        // 暴露给模型的全部工具列表
        static final List<Map<String, Object>> TOOLS = List.of(
                queryAccountingRecords(),
                getAccountingMonthlySummary(),
                queryPlans(),
                queryGoals()
        );

        // 构造单个工具的 schema 结构
        private static Map<String, Object> tool(String name, String description, Map<String, Object> params) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("type", "function");
            Map<String, Object> func = new LinkedHashMap<>();
            func.put("name", name);
            func.put("description", description);
            func.put("parameters", params);
            tool.put("function", func);
            return tool;
        }

        // 构造工具参数的 JSON Schema（object 类型及其属性）
        private static Map<String, Object> paramProps(Map<String, Map<String, Object>> props) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("type", "object");
            params.put("properties", new LinkedHashMap<>(props));
            params.put("required", List.of());
            return params;
        }

        // 工具定义：查询指定时间范围的账单明细
        private static Map<String, Object> queryAccountingRecords() {
            Map<String, Map<String, Object>> props = new LinkedHashMap<>();
            props.put("startDate", Map.of("type", "string",
                    "description", "开始日期。格式 yyyy-MM-dd（如2026-02-01）或 yyyy-MM（如2026-02表示该月），可选"));
            props.put("endDate", Map.of("type", "string",
                    "description", "结束日期。格式同 startDate，可选"));
            props.put("type", Map.of("type", "string",
                    "description", "交易类型：INCOME(收入) 或 EXPENSE(支出)，可选",
                    "enum", List.of("INCOME", "EXPENSE")));
            props.put("category", Map.of("type", "string",
                    "description", "分类关键词，可选，模糊匹配"));
            props.put("limit", Map.of("type", "integer",
                    "description", "返回最大条数，默认200，最大500"));
            return tool("query_accounting_records",
                    "查询指定时间范围的账单明细。传 startDate 和 endDate 限定范围；只传 startDate=yyyy-MM 则查整个月。",
                    paramProps(props));
        }

        // 工具定义：获取最近 N 个月的账单按月汇总
        private static Map<String, Object> getAccountingMonthlySummary() {
            Map<String, Map<String, Object>> props = new LinkedHashMap<>();
            props.put("months", Map.of("type", "integer",
                    "description", "返回最近N个月的汇总，默认12，最大60"));
            return tool("get_accounting_monthly_summary",
                    "获取最近N个月的账单按月汇总（收入、支出、结余）。适合回答各月对比、历史趋势等问题。",
                    paramProps(props));
        }

        // 工具定义：查询用户待办计划列表
        private static Map<String, Object> queryPlans() {
            Map<String, Map<String, Object>> props = new LinkedHashMap<>();
            props.put("status", Map.of("type", "string",
                    "description", "计划状态筛选，可选值：TODO(待办), DOING(进行中), DONE(已完成), ENDED(已结束)，可选"));
            props.put("limit", Map.of("type", "integer",
                    "description", "返回最大条数，默认50，最大200"));
            return tool("query_plans",
                    "查询用户待办计划列表，可按状态筛选。返回标题、状态、优先级、截止日期。",
                    paramProps(props));
        }

        // 工具定义：查询用户目标列表
        private static Map<String, Object> queryGoals() {
            Map<String, Map<String, Object>> props = new LinkedHashMap<>();
            props.put("status", Map.of("type", "string",
                    "description", "目标状态筛选，可选值：NOT_STARTED(未开始), ONGOING(进行中), ACHIEVED(已达成), ENDED(已结束)，可选"));
            props.put("limit", Map.of("type", "integer",
                    "description", "返回最大条数，默认50，最大200"));
            return tool("query_goals",
                    "查询用户目标列表，可按状态筛选。返回名称、状态、目标金额、当前金额、截止日期。",
                    paramProps(props));
        }
    }
}
