package com.example.personalmanager.goal.support;

import com.example.personalmanager.goal.entity.GoalStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标状态推导器，根据进度金额和起止日期实时计算目标当前所处状态。
 */
@Component
public class GoalStatusResolver {

    /**
     * 推导目标状态。
     *
     * @param currentAmount 当前已完成金额
     * @param targetAmount  目标金额
     * @param startDate     开始日期（可为空）
     * @param deadline      截止日期（可为空）
     * @param today         参照的当前日期
     * @return 推导得到的目标状态
     */
    public GoalStatus resolve(BigDecimal currentAmount,
                              BigDecimal targetAmount,
                              LocalDate startDate,
                              LocalDate deadline,
                              LocalDate today) {
        // 已达到或超过目标金额，视为已完成
        if (currentAmount.compareTo(targetAmount) >= 0) {
            return GoalStatus.DONE;
        }
        // 已过截止日期但未达成，视为已结束
        if (deadline != null && deadline.isBefore(today)) {
            return GoalStatus.ENDED;
        }
        // 开始日期还未到，视为未开始
        if (startDate != null && startDate.isAfter(today)) {
            return GoalStatus.NOT_STARTED;
        }
        // 尚无任何进度，视为未开始
        if (currentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return GoalStatus.NOT_STARTED;
        }
        // 其余情况均为进行中
        return GoalStatus.ONGOING;
    }
}
