package com.example.personalmanager.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表盘汇总响应 DTO，聚合本月收支、计划完成情况与目标达成情况。
 */
@Data
@Builder
public class DashboardSummaryResponse {

    // 本月收入合计
    private BigDecimal monthIncome;
    // 本月支出合计
    private BigDecimal monthExpense;
    // 本月结余（收入减支出）
    private BigDecimal monthBalance;

    // 计划总数
    private long totalPlans;
    // 已完成计划数
    private long donePlans;

    // 目标总数
    private long totalGoals;
    // 已达成目标数
    private long achievedGoals;
}
