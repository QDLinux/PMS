package com.example.personalmanager.dashboard.service;

import com.example.personalmanager.accounting.entity.AccountingRecord;
import com.example.personalmanager.accounting.entity.TransactionType;
import com.example.personalmanager.accounting.repository.AccountingRecordRepository;
import com.example.personalmanager.auth.service.CurrentUserService;
import com.example.personalmanager.dashboard.dto.DashboardSummaryResponse;
import com.example.personalmanager.goal.entity.Goal;
import com.example.personalmanager.goal.entity.GoalStatus;
import com.example.personalmanager.goal.repository.GoalRepository;
import com.example.personalmanager.goal.support.GoalStatusResolver;
import com.example.personalmanager.planning.entity.PlanItem;
import com.example.personalmanager.planning.entity.PlanStatus;
import com.example.personalmanager.planning.repository.PlanItemRepository;
import com.example.personalmanager.planning.support.PlanStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 仪表盘业务服务，汇总当前用户本月的收支、计划与目标统计数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountingRecordRepository accountingRecordRepository;
    private final PlanItemRepository planItemRepository;
    private final GoalRepository goalRepository;
    private final CurrentUserService currentUserService;
    private final PlanStatusResolver planStatusResolver;
    private final GoalStatusResolver goalStatusResolver;

    /**
     * 汇总当前用户的仪表盘数据。
     *
     * @return 包含本月收支、计划完成与目标达成统计的汇总结果
     */
    public DashboardSummaryResponse getSummary() {
        Long ownerId = currentUserService.getCurrentUserId();

        // 计算本月起止时间范围
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
        List<AccountingRecord> monthRecords = accountingRecordRepository
                .findAllByOwnerIdAndAccountDateBetweenOrderByAccountDateDescIdDesc(ownerId, startDateTime, endDateTime);

        // 累加本月收入
        BigDecimal income = monthRecords.stream()
                .filter(r -> r.getType() == TransactionType.INCOME)
                .map(AccountingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 累加本月支出
        BigDecimal expense = monthRecords.stream()
                .filter(r -> r.getType() == TransactionType.EXPENSE)
                .map(AccountingRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计计划总数与已完成数（状态实时推导）
        List<PlanItem> plans = planItemRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        long totalPlans = plans.size();
        long donePlans = plans.stream()
                .filter(plan -> planStatusResolver.resolve(plan.getStatus(), plan.getStartDate(), plan.getEndDate(), now) == PlanStatus.DONE)
                .count();

        // 统计目标总数与已达成数（状态实时推导）
        List<Goal> goals = goalRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        long totalGoals = goals.size();
        long achievedGoals = goals.stream()
                .filter(goal -> goalStatusResolver.resolve(goal.getCurrentAmount(), goal.getTargetAmount(), goal.getStartDate(), goal.getDeadline(), now) == GoalStatus.DONE)
                .count();

        // 组装汇总结果，结余为收入减支出
        return DashboardSummaryResponse.builder()
                .monthIncome(income)
                .monthExpense(expense)
                .monthBalance(income.subtract(expense))
                .totalPlans(totalPlans)
                .donePlans(donePlans)
                .totalGoals(totalGoals)
                .achievedGoals(achievedGoals)
                .build();
    }
}
