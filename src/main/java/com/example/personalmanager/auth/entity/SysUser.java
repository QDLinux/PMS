package com.example.personalmanager.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 系统用户实体，对应数据库表 sys_user，存储用户账号及个人资料信息。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser {

    // 主键 ID，自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名，唯一且非空
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 昵称
    @Column(length = 50)
    private String nickname;

    // 邮箱，唯一
    @Column(unique = true, length = 100)
    private String email;

    // 头像文件存储路径
    @Column(length = 255)
    private String avatarPath;

    // 加密后的登录密码
    @Column(nullable = false, length = 100)
    private String password;

    // 账号创建时间
    @Column(nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    // 持久化前回调，自动填充创建时间
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
