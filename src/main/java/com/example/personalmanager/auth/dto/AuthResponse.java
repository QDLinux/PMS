package com.example.personalmanager.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 认证响应 DTO，登录成功后返回给客户端的令牌信息。
 */
@Data
@Builder
public class AuthResponse {

    // 访问令牌（JWT）
    private String token;
    // 令牌类型，通常为 Bearer
    private String tokenType;
    // 令牌有效期（单位：秒）
    private long expiresIn;
    // 登录用户名
    private String username;
}
