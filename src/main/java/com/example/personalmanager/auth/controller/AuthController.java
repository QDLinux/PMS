package com.example.personalmanager.auth.controller;

import com.example.personalmanager.auth.dto.AuthResponse;
import com.example.personalmanager.auth.dto.LoginRequest;
import com.example.personalmanager.auth.dto.RegisterRequest;
import com.example.personalmanager.auth.service.AuthService;
import com.example.personalmanager.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供用户注册与登录的 REST 接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册接口。
     *
     * @param request 注册请求，包含用户名与密码
     * @return 注册结果响应
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("注册成功", null);
    }

    /**
     * 用户登录接口。
     *
     * @param request 登录请求，包含用户名与密码
     * @return 包含 JWT 令牌的认证响应
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }
}
