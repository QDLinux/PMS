package com.example.personalmanager.planning.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 计划项实体，对应数据库 plan_item 表，记录用户的一项计划及其时间区间、优先级和状态。
 */
@Getter
@Setter
@Entity
@Table(name = "plan_item", indexes = {
        @Index(name = "idx_plan_owner_status", columnList = "owner_id,status"),
        @Index(name = "idx_plan_owner_end", columnList = "owner_id,end_date"),
        @Index(name = "idx_plan_owner_created", columnList = "owner_id,created_at")
})
public class PlanItem {

    // 主键ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所属用户ID，用于数据隔离
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // 计划标题
    @Column(nullable = false, length = 100)
    private String title;

    // 计划描述，可空
    @Column(length = 1000)
    private String description;

    // 开始日期，可空
    private LocalDate startDate;

    // 结束日期，可空
    private LocalDate endDate;

    // 优先级
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanPriority priority;

    // 计划状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status;

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
