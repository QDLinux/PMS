package com.example.personalmanager.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 记账记录实体，对应数据库 accounting_record 表，记录用户的一笔收入或支出。
 */
@Getter
@Setter
@Entity
@Table(name = "accounting_record", indexes = {
        @Index(name = "idx_accounting_owner_date", columnList = "owner_id,account_date"),
        @Index(name = "idx_accounting_owner_created", columnList = "owner_id,created_at")
})
public class AccountingRecord {

    // 主键ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属用户ID，用于数据隔离
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 交易类型：收入或支出
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    // 金额，精度12位、小数2位
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // 分类（如餐饮、交通、工资等）
    @Column(nullable = false, length = 50)
    private String category;

    // 记账日期（业务发生时间）
    @Column(nullable = false, columnDefinition = "datetime")
    private LocalDateTime accountDate;

    // 备注，可空
    @Column(length = 500)
    private String note;

    // 创建时间
    @Column(nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    // 更新时间
    @Column(nullable = false, columnDefinition = "datetime")
    private LocalDateTime updatedAt;

    // 持久化前回调：初始化创建时间与更新时间
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // 更新前回调：刷新更新时间
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
