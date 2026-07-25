package com.example.personalmanager.planning.support;

import com.example.personalmanager.planning.entity.PlanStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 计划状态推导器，统一计划状态的计算规则，供写入和读取阶段共用。
 */
@Component
public class PlanStatusResolver {

    /**
     * 解析规划状态：终态（已完成、已结束）保持不变，其余按日期推导当前活动状态。
     * 写入与读取共用同一套规则。
     */
    public PlanStatus resolve(PlanStatus current, LocalDate startDate, LocalDate endDate, LocalDate today) {
        // 已完成为终态，直接保留
        if (current == PlanStatus.DONE) {
            return PlanStatus.DONE;
        }
        // 已结束为终态，直接保留
        if (current == PlanStatus.ENDED) {
            return PlanStatus.ENDED;
        }
        // 非终态则按日期重新推导活动状态
        return calculateActiveStatus(startDate, endDate, today);
    }

    // 根据起止日期与当前日期推导活动状态
    private PlanStatus calculateActiveStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {
        // 结束日期已过，视为已结束
        if (endDate != null && endDate.isBefore(today)) {
            return PlanStatus.ENDED;
        }
        // 尚未到开始日期，视为待开始
        if (startDate != null && today.isBefore(startDate)) {
            return PlanStatus.TODO;
        }
        // 未设置任何日期，默认待开始
        if (startDate == null && endDate == null) {
            return PlanStatus.TODO;
        }
        // 其余情况视为进行中
        return PlanStatus.IN_PROGRESS;
    }
}
