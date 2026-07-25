package com.example.personalmanager.planning.dto;

import com.example.personalmanager.planning.entity.PlanPriority;
import com.example.personalmanager.planning.entity.PlanStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划项响应 DTO，返回给前端的计划详情，其中 status 为按当前日期推导后的实际状态。
 */
@Data
@Builder
public class PlanItemResponse {

    // 计划ID
    private Long id;
    // 计划标题
    private String title;
    // 计划描述
    private String description;
    // 开始日期
    private LocalDate startDate;
    // 结束日期
    private LocalDate endDate;
    // 优先级
    private PlanPriority priority;
    // 计划状态（已按当前日期推导）
    private PlanStatus status;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
}
