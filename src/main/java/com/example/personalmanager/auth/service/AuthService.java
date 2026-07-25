package com.example.personalmanager.auth.service;

import com.example.personalmanager.auth.dto.AuthResponse;
import com.example.personalmanager.auth.dto.LoginRequest;
import com.example.personalmanager.auth.dto.RegisterRequest;
import com.example.personalmanager.auth.entity.SysUser;
import com.example.personalmanager.auth.repository.SysUserRepository;
import com.example.personalmanager.common.BusinessException;
import com.example.personalmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务，负责用户注册与登录的核心业务逻辑，包括密码加密与 JWT 令牌签发。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * 注册新用户：校验用户名唯一性并对密码加密后持久化。
     *
     * @param request 注册请求
     */
    public void register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setNickname(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

    /**
     * 用户登录：校验凭证并生成 JWT 令牌。
     *
     * @param request 登录请求
     * @return 包含令牌及有效期的认证响应
     */
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        // 通过认证管理器校验用户名与密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMillis() / 1000)
                .username(userDetails.getUsername())
                .build();
    }
}
