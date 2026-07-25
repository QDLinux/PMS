package com.example.personalmanager.goal.repository;

import com.example.personalmanager.goal.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 目标数据访问接口，提供基于所有者的目标查询能力。
 */
public interface GoalRepository extends JpaRepository<Goal, Long> {

    // 按 ID 和所有者查询单个目标，用于数据隔离
    Optional<Goal> findByIdAndOwnerId(Long id, Long ownerId);

    // 查询某用户的全部目标，按创建时间倒序
    List<Goal> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
