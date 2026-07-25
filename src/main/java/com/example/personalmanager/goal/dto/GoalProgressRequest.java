package com.example.personalmanager.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 目标进度更新请求 DTO，用于为目标累加完成金额。
 */
@Data
public class GoalProgressRequest {

    // 进度增量（本次新增的完成金额）
    @NotNull(message = "进度增量不能为空")
    @DecimalMin(value = "0.01", message = "进度增量必须大于0")
    private BigDecimal increment;
}
