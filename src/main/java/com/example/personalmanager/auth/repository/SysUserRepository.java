package com.example.personalmanager.auth.repository;

import com.example.personalmanager.auth.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 系统用户数据访问接口，提供用户的持久化操作与按用户名、邮箱的查询能力。
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    // 根据用户名查询用户
    Optional<SysUser> findByUsername(String username);

    // 判断用户名是否已存在
    boolean existsByUsername(String username);

    // 判断除指定 id 外是否存在相同用户名（用于更新时的唯一性校验）
    boolean existsByUsernameAndIdNot(String username, Long id);

    // 判断除指定 id 外是否存在相同邮箱（用于更新时的唯一性校验）
    boolean existsByEmailAndIdNot(String email, Long id);
}
