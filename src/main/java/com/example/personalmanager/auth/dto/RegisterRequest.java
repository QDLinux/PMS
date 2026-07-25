package com.example.personalmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO，封装用户注册时提交的用户名与密码。
 */
@Data
public class RegisterRequest {

    // 用户名，长度需在 4 到 50 之间
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在4到50之间")
    private String username;

    // 密码，长度需在 6 到 50 之间
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50之间")
    private String password;
}
