package com.example.personalmanager.planning.entity;

/**
 * 计划状态枚举。其中 DONE、ENDED 为终态，TODO、IN_PROGRESS 为可由日期推导的活动状态。
 */
public enum PlanStatus {
    TODO,         // 待开始
    IN_PROGRESS,  // 进行中
    DONE,         // 已完成
    ENDED         // 已结束（截止日期已过仍未完成）
}
