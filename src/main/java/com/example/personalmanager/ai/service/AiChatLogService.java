package com.example.personalmanager.ai.service;

import com.example.personalmanager.ai.config.AiProperties;
import com.example.personalmanager.ai.dto.AiChatHistoryItemResponse;
import com.example.personalmanager.ai.dto.AiChatSessionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 对话日志服务，基于文件系统持久化会话元数据、对话历史（JSONL）及调用日志，
 * 提供会话的创建、查询、删除以及成功/失败调用的落盘记录能力。
 * 所有写操作均加锁，避免并发读写文件造成数据损坏。
 */
@Service
@RequiredArgsConstructor
public class AiChatLogService {

    // 时间戳格式
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 日期格式（用于按天分割日志文件）
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    /**
     * 确保会话存在：已存在则更新其时间与标题，不存在则新建，返回有效的会话 ID。
     *
     * @param username  用户名
     * @param sessionId 会话 ID，为空时自动生成
     * @param seedTitle 用于生成会话标题的种子文本（通常是首条用户消息）
     * @return 有效的会话 ID
     */
    public synchronized String ensureSession(String username, String sessionId, String seedTitle) {
        List<SessionMeta> sessions = loadSessions(username);
        String now = LocalDateTime.now().format(TS_FMT);
        String sid = StringUtils.hasText(sessionId) ? sessionId.trim() : "";
        // 未传会话 ID 时生成一个去掉连字符的 UUID
        if (!StringUtils.hasText(sid)) {
            sid = UUID.randomUUID().toString().replace("-", "");
        }

        // 查找是否已存在同 ID 的会话
        SessionMeta exists = null;
        for (SessionMeta s : sessions) {
            if (sid.equals(s.getSessionId())) {
                exists = s;
                break;
            }
        }
        // 不存在则创建新会话并置于列表首位
        if (exists == null) {
            SessionMeta created = new SessionMeta();
            created.setSessionId(sid);
            created.setTitle(buildSessionTitle(seedTitle));
            created.setUpdatedAt(now);
            sessions.add(0, created);
            saveSessions(username, sessions);
            return sid;
        }

        // 已存在则刷新更新时间，标题为空或仍是默认值时用种子文本回填
        exists.setUpdatedAt(now);
        if (!StringUtils.hasText(exists.getTitle()) || "新对话".equals(exists.getTitle())) {
            exists.setTitle(buildSessionTitle(seedTitle));
        }
        sortSessions(sessions);
        saveSessions(username, sessions);
        return sid;
    }

    /**
     * 创建一个全新的会话。
     *
     * @param username 用户名
     * @param title    会话标题，可为空
     * @return 新建会话的响应信息
     */
    public synchronized AiChatSessionResponse createSession(String username, String title) {
        List<SessionMeta> sessions = loadSessions(username);
        SessionMeta created = new SessionMeta();
        created.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        created.setTitle(buildSessionTitle(title));
        created.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        sessions.add(0, created);
        saveSessions(username, sessions);
        return toSessionResponse(created);
    }

    /**
     * 查询用户的会话列表，按更新时间倒序返回。
     *
     * @param username 用户名
     * @param limit    返回上限（最终限制在 1~200）
     * @return 会话列表
     */
    public synchronized List<AiChatSessionResponse> listSessions(String username, int limit) {
        int safeLimit = Math.max(1, Math.min(200, limit));
        List<SessionMeta> sessions = loadSessions(username);
        sortSessions(sessions);
        return sessions.stream().limit(safeLimit).map(this::toSessionResponse).collect(Collectors.toList());
    }

    /**
     * 删除指定会话及其在历史文件中的所有相关消息。
     *
     * @param username  用户名
     * @param sessionId 待删除的会话 ID
     */
    public synchronized void deleteSession(String username, String sessionId) {
        if (!StringUtils.hasText(sessionId)) return;
        String targetSessionId = sessionId.trim();

        // 先从会话元数据中移除该会话
        List<SessionMeta> sessions = loadSessions(username);
        sessions.removeIf(s -> targetSessionId.equals(s.getSessionId()));
        saveSessions(username, sessions);

        // 再逐行过滤历史文件，保留不属于该会话的记录后重写
        Path historyFile = resolveHistoryFile(username);
        if (!Files.exists(historyFile)) return;
        try {
            List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
            List<String> kept = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (!StringUtils.hasText(line)) continue;
                try {
                    JsonNode node = objectMapper.readTree(line);
                    String sid = node.path("sessionId").asText("");
                    if (isHistoryLineOfSession(targetSessionId, sid)) {
                        continue;
                    }
                    kept.add(line);
                } catch (JsonProcessingException ignored) {
                    // 无法解析的行予以保留，避免误删
                    kept.add(line);
                }
            }
            Files.write(historyFile, kept, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    /**
     * 记录一次成功的 AI 调用：写入调用日志并追加对话历史。
     */
    public synchronized void logSuccess(String username, String sessionId, String message, String context, String answer, String model) {
        writeLog(username, sessionId, message, context, answer, model, null);
        writeHistory(username, sessionId, message, answer);
    }

    /**
     * 记录一次失败的 AI 调用：写入错误日志，并将失败提示作为助手回复追加到历史。
     */
    public synchronized void logFailure(String username, String sessionId, String message, String context, String errorMessage) {
        writeLog(username, sessionId, message, context, null, null, errorMessage);
        writeHistory(username, sessionId, message, "AI调用失败: " + normalizeText(errorMessage, 1000));
    }

    /**
     * 查询用户的对话历史。
     *
     * @param username  用户名
     * @param sessionId 会话 ID，为空时返回全部；"legacy" 表示早期无会话 ID 的历史
     * @param limit     返回上限（最终限制在 1~500），取最新的若干条
     * @return 历史消息列表
     */
    public synchronized List<AiChatHistoryItemResponse> listHistory(String username, String sessionId, int limit) {
        int safeLimit = Math.max(1, Math.min(500, limit));
        Path filePath = resolveHistoryFile(username);
        if (!Files.exists(filePath)) {
            return List.of();
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<AiChatHistoryItemResponse> items = new ArrayList<>();
            for (String line : lines) {
                if (!StringUtils.hasText(line)) continue;
                try {
                    JsonNode node = objectMapper.readTree(line);
                    String role = node.path("role").asText("");
                    String text = node.path("text").asText("");
                    String time = node.path("time").asText("");
                    String sid = node.path("sessionId").asText("");
                    if (!StringUtils.hasText(role) || !StringUtils.hasText(text)) continue;
                    // 按会话 ID 过滤：legacy 匹配无 ID 的旧记录，否则精确匹配
                    if (StringUtils.hasText(sessionId)) {
                        if ("legacy".equals(sessionId)) {
                            if (StringUtils.hasText(sid) && !"legacy".equals(sid)) continue;
                        } else if (!sessionId.equals(sid)) {
                            continue;
                        }
                    }
                    items.add(AiChatHistoryItemResponse.builder().role(role).text(text).time(time).build());
                } catch (JsonProcessingException ignored) {
                }
            }
            // 仅返回末尾最新的 safeLimit 条
            int from = Math.max(0, items.size() - safeLimit);
            return new ArrayList<>(items.subList(from, items.size()));
        } catch (IOException ignored) {
            return List.of();
        }
    }

    // 写入可读的调用日志（按用户与日期分文件），区分成功与失败两种格式
    private void writeLog(String username, String sessionId, String message, String context, String answer, String model, String errorMessage) {
        try {
            Path logDir = Path.of(aiProperties.getLogDir());
            Files.createDirectories(logDir);

            String user = sanitizeFilePart(username);
            String day = LocalDate.now().format(DAY_FMT);
            Path filePath = logDir.resolve(user + "-" + day + ".log");

            StringBuilder sb = new StringBuilder(1024);
            sb.append("[").append(LocalDateTime.now().format(TS_FMT)).append("]\n");
            sb.append("user: ").append(StringUtils.hasText(username) ? username : "unknown").append("\n");
            sb.append("sessionId: ").append(StringUtils.hasText(sessionId) ? sessionId : "-").append("\n");
            sb.append("request: ").append(normalizeText(message, 8000)).append("\n");
            if (StringUtils.hasText(context)) {
                sb.append("context: ").append(normalizeText(context, 16000)).append("\n");
            }
            if (StringUtils.hasText(errorMessage)) {
                sb.append("status: error\n");
                sb.append("error: ").append(normalizeText(errorMessage, 2000)).append("\n");
            } else {
                sb.append("status: success\n");
                sb.append("model: ").append(StringUtils.hasText(model) ? model : "-").append("\n");
                sb.append("answer: ").append(normalizeText(answer, 16000)).append("\n");
            }
            sb.append("--------------------------------------------------\n");

            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging must not break business flow.
        }
    }

    // 将一轮问答追加写入历史文件（用户消息 + 助手回复各一行）
    private void writeHistory(String username, String sessionId, String userMessage, String assistantReply) {
        try {
            Path filePath = resolveHistoryFile(username);
            Files.createDirectories(filePath.getParent());
            String now = LocalDateTime.now().format(TS_FMT);

            appendHistoryLine(filePath, sessionId, "user", normalizeHistoryText(userMessage, 8000), now);
            appendHistoryLine(filePath, sessionId, "assistant", normalizeHistoryText(assistantReply, 16000), now);
        } catch (IOException ignored) {
            // history logging must not break business flow
        }
    }

    // 以 JSONL 格式追加一条历史记录行
    private void appendHistoryLine(Path filePath, String sessionId, String role, String text, String time) throws IOException {
        Map<String, String> lineObj = new LinkedHashMap<>();
        lineObj.put("sessionId", StringUtils.hasText(sessionId) ? sessionId : "");
        lineObj.put("role", role);
        lineObj.put("text", text);
        lineObj.put("time", time);
        String json = objectMapper.writeValueAsString(lineObj);
        Files.writeString(filePath, json + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // 加载会话元数据；文件缺失或为空时回退到基于历史记录构建的兼容会话
    private List<SessionMeta> loadSessions(String username) {
        Path filePath = resolveSessionsFile(username);
        if (!Files.exists(filePath)) {
            return buildLegacySessions(username);
        }
        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(json)) return buildLegacySessions(username);
            List<SessionMeta> sessions = objectMapper.readValue(json, new TypeReference<List<SessionMeta>>() {});
            if (sessions == null || sessions.isEmpty()) {
                return buildLegacySessions(username);
            }
            return new ArrayList<>(sessions);
        } catch (IOException ignored) {
            return buildLegacySessions(username);
        }
    }

    // 为没有会话元数据但存在历史记录的旧用户构建一个名为 "legacy" 的兼容会话
    private List<SessionMeta> buildLegacySessions(String username) {
        Path historyFile = resolveHistoryFile(username);
        if (!Files.exists(historyFile)) return new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return new ArrayList<>();
        } catch (IOException ignored) {
            return new ArrayList<>();
        }

        SessionMeta legacy = new SessionMeta();
        legacy.setSessionId("legacy");
        legacy.setTitle("历史对话");
        legacy.setUpdatedAt(LocalDateTime.now().format(TS_FMT));
        List<SessionMeta> list = new ArrayList<>();
        list.add(legacy);
        saveSessions(username, list);
        return list;
    }

    // 将会话元数据排序后序列化保存到文件
    private void saveSessions(String username, List<SessionMeta> sessions) {
        try {
            sortSessions(sessions);
            Path filePath = resolveSessionsFile(username);
            Files.createDirectories(filePath.getParent());
            String json = objectMapper.writeValueAsString(sessions);
            Files.writeString(filePath, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    // 按更新时间倒序排序会话（空值排在最后）
    private void sortSessions(List<SessionMeta> sessions) {
        sessions.sort(Comparator.comparing(SessionMeta::getUpdatedAt, Comparator.nullsLast(String::compareTo)).reversed());
    }

    // 将会话元数据转换为对外响应对象，标题/时间为空时填充默认值
    private AiChatSessionResponse toSessionResponse(SessionMeta session) {
        return AiChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(StringUtils.hasText(session.getTitle()) ? session.getTitle() : "新对话")
                .updatedAt(StringUtils.hasText(session.getUpdatedAt()) ? session.getUpdatedAt() : "")
                .build();
    }

    // 解析用户历史记录文件路径（history/{user}.jsonl）
    private Path resolveHistoryFile(String username) {
        Path logDir = Path.of(aiProperties.getLogDir()).resolve("history");
        String user = sanitizeFilePart(username);
        return logDir.resolve(user + ".jsonl");
    }

    // 解析用户会话元数据文件路径（history/{user}-sessions.json）
    private Path resolveSessionsFile(String username) {
        Path logDir = Path.of(aiProperties.getLogDir()).resolve("history");
        String user = sanitizeFilePart(username);
        return logDir.resolve(user + "-sessions.json");
    }

    // 清理文件名中的非法字符，避免路径注入与非法文件名
    private String sanitizeFilePart(String text) {
        String src = StringUtils.hasText(text) ? text.trim() : "unknown";
        String cleaned = src.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    // 判断某条历史记录是否属于目标会话（兼容 legacy 旧记录）
    private boolean isHistoryLineOfSession(String targetSessionId, String lineSessionId) {
        if ("legacy".equals(targetSessionId)) {
            return !StringUtils.hasText(lineSessionId) || "legacy".equals(lineSessionId);
        }
        return targetSessionId.equals(lineSessionId);
    }

    // 规范化日志文本：换行替换为分隔符并按上限截断（用于单行可读日志）
    private String normalizeText(String text, int maxLen) {
        String src = StringUtils.hasText(text) ? text.replace("\r", " ").replace("\n", " | ").trim() : "-";
        if (src.length() <= maxLen) {
            return src;
        }
        return src.substring(0, maxLen) + "...(truncated)";
    }

    // 规范化历史文本：统一换行符并按上限截断（保留多行结构）
    private String normalizeHistoryText(String text, int maxLen) {
        String src = StringUtils.hasText(text) ? text.replace("\r\n", "\n").replace('\r', '\n').trim() : "-";
        if (src.length() <= maxLen) {
            return src;
        }
        return src.substring(0, maxLen) + "...(truncated)";
    }

    /**
     * 会话元数据，持久化到 sessions 文件中的最小会话信息。
     */
    @lombok.Data
    public static class SessionMeta {
        // 会话唯一标识
        private String sessionId;
        // 会话标题
        private String title;
        // 最近更新时间
        private String updatedAt;
    }

    // 根据种子文本生成会话标题：清理空白并截断到 22 字符，为空时返回 "新对话"
    private String buildSessionTitle(String seedTitle) {
        String text = StringUtils.hasText(seedTitle) ? seedTitle.replaceAll("\\s+", " ").trim() : "";
        if (!StringUtils.hasText(text)) return "新对话";
        return text.length() <= 22 ? text : text.substring(0, 22);
    }
}
