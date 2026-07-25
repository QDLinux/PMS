package com.example.personalmanager.goal.dto;

import com.example.personalmanager.goal.entity.GoalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 目标响应 DTO，用于向客户端返回目标详情及其实时状态。
 */
@Data
@Builder
public class GoalResponse {

    // 目标 ID
    private Long id;
    // 目标名称
    private String name;
    // 目标描述
    private String description;
    // 目标金额
    private BigDecimal targetAmount;
    // 当前已完成金额
    private BigDecimal currentAmount;
    // 开始日期
    private LocalDate startDate;
    // 截止日期
    private LocalDate deadline;
    // 目标状态（实时推导得出）
    private GoalStatus status;
    // 创建时间
    private LocalDateTime createdAt;
    // 最后更新时间
    private LocalDateTime updatedAt;
}
