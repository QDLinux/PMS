package com.example.personalmanager.auth.service;

import com.example.personalmanager.auth.entity.SysUser;
import com.example.personalmanager.auth.repository.SysUserRepository;
import com.example.personalmanager.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户服务，从安全上下文中解析并获取当前认证用户的信息。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final SysUserRepository userRepository;

    /**
     * 获取当前登录用户实体。
     *
     * @return 当前登录的用户实体
     */
    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 校验认证状态，未登录或匿名用户均视为未认证
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("当前用户未登录");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("当前用户不存在: " + username));
    }

    /**
     * 获取当前登录用户的 ID。
     *
     * @return 当前用户 ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 获取当前登录用户的用户名。
     *
     * @return 当前用户名
     */
    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}
