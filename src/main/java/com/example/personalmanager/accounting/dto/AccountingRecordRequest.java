package com.example.personalmanager.accounting.dto;

import com.example.personalmanager.accounting.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账记录创建/更新请求 DTO，承载前端提交的记账表单数据并附带校验规则。
 */
@Data
public class AccountingRecordRequest {

    // 交易类型：收入或支出
    @NotNull(message = "交易类型不能为空")
    private TransactionType type;

    // 金额，必须大于0
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;

    // 分类
    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    // 记账日期
    @NotNull(message = "记账日期不能为空")
    private LocalDateTime accountDate;

    // 备注，可空
    @Size(max = 500, message = "备注长度不能超过500")
    private String note;
}
