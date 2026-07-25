package com.example.personalmanager.goal.entity;

/**
 * 目标状态枚举，描述目标在生命周期中所处的阶段。
 */
public enum GoalStatus {
    NOT_STARTED, // 未开始：尚未到开始日期或暂无进度
    ONGOING,     // 进行中：已开始且未达成
    DONE,        // 已完成：当前金额已达到目标金额
    ENDED,       // 已结束：已过截止日期但未达成
    ACHIEVED,    // 已达成
    EXPIRED      // 已过期
}
