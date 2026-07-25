package com.example.personalmanager.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标创建/更新请求 DTO，封装客户端提交的目标数据并进行参数校验。
 */
@Data
public class GoalRequest {

    // 目标名称
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 100, message = "目标名称长度不能超过100")
    private String name;

    // 目标描述（可选）
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    // 目标金额
    @NotNull(message = "目标金额不能为空")
    @DecimalMin(value = "0.01", message = "目标金额必须大于0")
    private BigDecimal targetAmount;

    // 当前已完成金额
    @NotNull(message = "当前金额不能为空")
    @DecimalMin(value = "0.00", message = "当前金额不能小于0")
    private BigDecimal currentAmount;

    // 开始日期（可选）
    private LocalDate startDate;

    // 截止日期（可选）
    private LocalDate deadline;
}
