package com.example.personalmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO，封装用户登录时提交的用户名与密码。
 */
@Data
public class LoginRequest {

    // 用户名
    @NotBlank(message = "用户名不能为空")
    private String username;

    // 密码
    @NotBlank(message = "密码不能为空")
    private String password;
}
