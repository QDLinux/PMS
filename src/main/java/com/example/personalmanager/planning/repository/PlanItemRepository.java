package com.example.personalmanager.planning.repository;

import com.example.personalmanager.planning.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 计划项数据访问接口。
 */
public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    // 按ID和所属用户查询单条计划，保证数据隔离
    Optional<PlanItem> findByIdAndOwnerId(Long id, Long ownerId);

    // 查询某用户全部计划，按创建时间倒序
    List<PlanItem> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
