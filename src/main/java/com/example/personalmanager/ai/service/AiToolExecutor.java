package com.example.personalmanager.ai.service;

import com.example.personalmanager.accounting.entity.AccountingRecord;
import com.example.personalmanager.accounting.entity.TransactionType;
import com.example.personalmanager.accounting.repository.AccountingRecordRepository;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.goal.entity.Goal;
import com.example.personalmanager.goal.repository.GoalRepository;
import com.example.personalmanager.planning.entity.PlanItem;
import com.example.personalmanager.planning.repository.PlanItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI 工具执行器，负责接收大模型发起的工具调用请求，按工具名称分发到对应的
 * 数据查询逻辑（账单、计划、目标等），并将查询结果序列化为 JSON 字符串返回给模型。
 */
@Service
public class AiToolExecutor {

    private final AccountingRecordRepository accountingRepo;
    private final PlanItemRepository planRepo;
    private final GoalRepository goalRepo;
    private final CurrentUserService currentUserService;

    public AiToolExecutor(AccountingRecordRepository accountingRepo,
                          PlanItemRepository planRepo,
                          GoalRepository goalRepo,
                          CurrentUserService currentUserService) {
        this.accountingRepo = accountingRepo;
        this.planRepo = planRepo;
        this.goalRepo = goalRepo;
        this.currentUserService = currentUserService;
    }

    /**
     * 执行指定名称的工具调用。
     *
     * @param name 工具名称
     * @param args 模型传入的工具参数
     * @return 工具执行结果的 JSON 字符串；未知工具或执行异常时返回错误 JSON
     */
    public String execute(String name, Map<String, Object> args) {
        // 工具调用始终限定在当前登录用户的数据范围内
        Long ownerId = currentUserService.getCurrentUserId();
        try {
            // 按工具名称分发到对应的查询实现
            return switch (name) {
                case "query_accounting_records" -> queryRecords(args, ownerId);
                case "get_accounting_monthly_summary" -> monthlySummary(args, ownerId);
                case "query_plans" -> queryPlans(args, ownerId);
                case "query_goals" -> queryGoals(args, ownerId);
                default -> jsonError("未知工具: " + name);
            };
        } catch (Exception e) {
            return jsonError("工具执行异常: " + e.getMessage());
        }
    }

    // 查询指定时间范围与筛选条件下的账单明细，并拼装为 JSON
    private String queryRecords(Map<String, Object> args, Long ownerId) {
        String startStr = stringArg(args, "startDate");
        String endStr = stringArg(args, "endDate");
        String typeStr = stringArg(args, "type");
        String category = stringArg(args, "category");
        int limit = clamp(intArg(args, "limit", 200), 1, 500);

        LocalDate startDate = parseDateArg(startStr);
        LocalDate endDate = parseDateArg(endStr);

        // 仅给出结束日期时，起始日期回退到一个足够早的下界
        if (startDate == null && endDate != null) {
            startDate = LocalDate.of(2000, 1, 1);
        }
        // 仅给出起始日期时推断结束日期：按月查询补全到月末，否则取至今天
        if (endDate == null && startDate != null) {
            if (startStr != null && startStr.trim().length() == 7) {
                endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            } else {
                endDate = LocalDate.now();
            }
        }

        List<AccountingRecord> records;
        // 有完整日期范围则按范围查询，否则查询全部
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            records = accountingRepo.findAllByOwnerIdAndAccountDateBetweenOrderByAccountDateDescIdDesc(ownerId, start, end);
        } else {
            records = accountingRepo.findAllByOwnerIdOrderByAccountDateDescIdDesc(ownerId);
        }

        final String typeFilter = typeStr;
        final String categoryFilter = category;
        // 按交易类型与分类关键词（模糊匹配）过滤，并限制返回条数
        List<AccountingRecord> filtered = filterWithLimit(records, r -> {
            if (typeFilter != null && !r.getType().name().equalsIgnoreCase(typeFilter)) return false;
            if (categoryFilter != null && (r.getCategory() == null || !r.getCategory().contains(categoryFilter))) return false;
            return true;
        }, limit);

        if (filtered.isEmpty()) {
            return "{\"count\":0,\"records\":[]}";
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\"count\":").append(filtered.size()).append(",\"records\":[");
        for (int i = 0; i < filtered.size(); i++) {
            AccountingRecord r = filtered.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"date\":\"").append(r.getAccountDate().format(fmt)).append("\"");
            sb.append(",\"type\":\"").append(r.getType().name()).append("\"");
            sb.append(",\"amount\":").append(r.getAmount().doubleValue());
            sb.append(",\"category\":\"").append(escapeJson(r.getCategory())).append("\"");
            if (StringUtils.hasText(r.getNote())) {
                sb.append(",\"note\":\"").append(escapeJson(r.getNote().trim())).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // 按月汇总账单的收入、支出与结余，返回最近 N 个月的数据
    private String monthlySummary(Map<String, Object> args, Long ownerId) {
        int months = clamp(intArg(args, "months", 12), 1, 60);
        List<AccountingRecord> records = accountingRepo.findAllByOwnerIdOrderByAccountDateDescIdDesc(ownerId);

        // 以 yyyy-MM 为键聚合，数组下标 0 记收入、1 记支出
        Map<String, double[]> groups = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (AccountingRecord r : records) {
            String key = r.getAccountDate().format(fmt);
            groups.computeIfAbsent(key, k -> new double[2]);
            double amt = r.getAmount().doubleValue();
            if (r.getType() == TransactionType.INCOME) {
                groups.get(key)[0] += amt;
            } else {
                groups.get(key)[1] += amt;
            }
        }

        List<String> sortedKeys = new ArrayList<>(groups.keySet());
        Collections.sort(sortedKeys);
        // 仅保留最近 months 个月的窗口
        int from = Math.max(0, sortedKeys.size() - months);
        List<String> window = sortedKeys.subList(from, sortedKeys.size());

        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\"months\":[");
        for (int i = 0; i < window.size(); i++) {
            String key = window.get(i);
            double[] vals = groups.get(key);
            if (i > 0) sb.append(",");
            sb.append("{\"month\":\"").append(key).append("\"");
            sb.append(",\"income\":").append(String.format("%.2f", vals[0]));
            sb.append(",\"expense\":").append(String.format("%.2f", vals[1]));
            sb.append(",\"balance\":").append(String.format("%.2f", vals[0] - vals[1]));
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // 查询用户计划列表，可按状态筛选，并拼装为 JSON
    private String queryPlans(Map<String, Object> args, Long ownerId) {
        String status = stringArg(args, "status");
        int limit = clamp(intArg(args, "limit", 50), 1, 200);
        List<PlanItem> plans = planRepo.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);

        List<PlanItem> filtered = filterWithLimit(plans,
                p -> status == null || (p.getStatus() != null && p.getStatus().name().equalsIgnoreCase(status)),
                limit);

        if (filtered.isEmpty()) return "{\"count\":0,\"plans\":[]}";

        DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\"count\":").append(filtered.size()).append(",\"plans\":[");
        for (int i = 0; i < filtered.size(); i++) {
            PlanItem p = filtered.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"title\":\"").append(escapeJson(p.getTitle())).append("\"");
            sb.append(",\"status\":\"").append(p.getStatus().name()).append("\"");
            sb.append(",\"priority\":\"").append(p.getPriority().name()).append("\"");
            if (p.getEndDate() != null) {
                sb.append(",\"endDate\":\"").append(p.getEndDate().format(dfmt)).append("\"");
            }
            if (StringUtils.hasText(p.getDescription())) {
                String desc = p.getDescription().trim();
                if (desc.length() > 200) desc = desc.substring(0, 200);
                sb.append(",\"description\":\"").append(escapeJson(desc)).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // 查询用户目标列表，可按状态筛选，并拼装为 JSON
    private String queryGoals(Map<String, Object> args, Long ownerId) {
        String status = stringArg(args, "status");
        int limit = clamp(intArg(args, "limit", 50), 1, 200);
        List<Goal> goals = goalRepo.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);

        List<Goal> filtered = filterWithLimit(goals,
                g -> status == null || (g.getStatus() != null && g.getStatus().name().equalsIgnoreCase(status)),
                limit);

        if (filtered.isEmpty()) return "{\"count\":0,\"goals\":[]}";

        DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder(2048);
        sb.append("{\"count\":").append(filtered.size()).append(",\"goals\":[");
        for (int i = 0; i < filtered.size(); i++) {
            Goal g = filtered.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escapeJson(g.getName())).append("\"");
            sb.append(",\"status\":\"").append(g.getStatus().name()).append("\"");
            sb.append(",\"targetAmount\":").append(g.getTargetAmount().doubleValue());
            sb.append(",\"currentAmount\":").append(g.getCurrentAmount().doubleValue());
            if (g.getDeadline() != null) {
                sb.append(",\"deadline\":\"").append(g.getDeadline().format(dfmt)).append("\"");
            }
            sb.append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /* ---- helpers ---- */

    /**
     * 按谓词过滤集合并截断到 limit 条。limit 在内存层生效，
     * 与各查询方法原有的"遍历 + size 达上限即停"语义一致。
     */
    private <T> List<T> filterWithLimit(List<T> source, java.util.function.Predicate<T> predicate, int limit) {
        List<T> result = new ArrayList<>();
        for (T item : source) {
            if (!predicate.test(item)) {
                continue;
            }
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    // 从参数 Map 取字符串值，去除空白；为空时返回 null
    private String stringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    // 从参数 Map 取整数值，缺失或解析失败时返回默认值
    private int intArg(Map<String, Object> args, String key, int def) {
        Object v = args.get(key);
        if (v == null) return def;
        try {
            if (v instanceof Number n) return n.intValue();
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // 将数值限制在 [min, max] 区间内
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // 解析日期参数，支持 yyyy-MM-dd 与 yyyy-MM（按当月 1 号）两种格式，无效时返回 null
    private LocalDate parseDateArg(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            String trimmed = s.trim();
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed);
            }
            if (trimmed.length() == 7) {
                return LocalDate.parse(trimmed + "-01");
            }
            return LocalDate.parse(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    // 转义 JSON 字符串中的特殊字符，保证拼装出的 JSON 合法
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // 构造统一的错误结果 JSON
    private String jsonError(String msg) {
        return "{\"error\":\"" + escapeJson(msg) + "\"}";
    }
}
