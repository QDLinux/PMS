package com.example.personalmanager.goal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 目标实体，对应数据库中的 goal 表，记录用户设定的储蓄/进度类目标。
 */
@Getter
@Setter
@Entity
@Table(name = "goal", indexes = {
        @Index(name = "idx_goal_owner_status", columnList = "owner_id,status"),
        @Index(name = "idx_goal_owner_deadline", columnList = "owner_id,deadline"),
        @Index(name = "idx_goal_owner_created", columnList = "owner_id,created_at")
})
public class Goal {

    // 主键 ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属用户 ID，用于数据隔离
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 目标名称
    @Column(nullable = false, length = 100)
    private String name;

    // 目标描述（可选）
    @Column(length = 500)
    private String description;

    // 目标金额
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    // 当前已完成金额
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAmount;

    // 开始日期（可选）
    private LocalDate startDate;

    // 截止日期（可选）
    private LocalDate deadline;

    // 目标状态，以字符串形式存储
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    // 创建时间
    @Column(nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    // 最后更新时间
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
