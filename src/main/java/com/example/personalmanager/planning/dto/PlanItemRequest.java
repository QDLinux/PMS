package com.example.personalmanager.planning.dto;

import com.example.personalmanager.planning.entity.PlanPriority;
import com.example.personalmanager.planning.entity.PlanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 计划项创建/更新请求 DTO，承载前端提交的计划表单数据并附带校验规则。
 */
@Data
public class PlanItemRequest {

    // 计划标题
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    // 计划描述，可空
    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;

    // 开始日期，可空
    private LocalDate startDate;

    // 结束日期，可空
    private LocalDate endDate;

    // 优先级
    @NotNull(message = "优先级不能为空")
    private PlanPriority priority;

    // 计划状态
    @NotNull(message = "状态不能为空")
    private PlanStatus status;
}
